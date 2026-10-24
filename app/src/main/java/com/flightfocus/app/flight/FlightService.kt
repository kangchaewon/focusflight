package com.flightfocus.app.flight

import android.app.Notification
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.flightfocus.app.domain.flight.FlightState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 비행 중 프로세스를 살려 두고 알림을 그리는 Foreground Service.
 * 비행 상태는 [FlightController]가 갖고 있고, 서비스는 그것을 지켜보며 그리기만 한다.
 */
@AndroidEntryPoint
class FlightService : Service() {

    @Inject
    lateinit var flightController: FlightController

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var renderJob: Job? = null
    private var shownTurbulenceStart: Long? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // startForegroundService()로 시작했으면 몇 초 안에 반드시 startForeground()를 불러야 한다.
        // 그 사이 비행이 끝났더라도 일단 띄우고, 아래 render()가 곧바로 내린다.
        val flight = (flightController.state.value as? FlightState.Airborne)?.flight
        startInForeground(flight?.let { FlightNotifications.inFlight(this, it) } ?: FlightNotifications.preparing(this))

        if (renderJob == null) {
            renderJob = scope.launch { flightController.state.collect(::render) }
        }
        // 시스템이 프로세스를 죽이면 되살리지 않는다. 다음에 앱을 열 때 Room에서 복원한다.
        return START_NOT_STICKY
    }

    private fun render(state: FlightState) {
        when (state) {
            is FlightState.Airborne -> {
                getSystemService(NotificationManager::class.java)
                    .notify(FlightNotifications.ID_IN_FLIGHT, FlightNotifications.inFlight(this, state.flight))
                val turbulenceStart = state.flight.turbulenceStartedAtMillis
                if (turbulenceStart != shownTurbulenceStart) {
                    if (turbulenceStart != null) FlightNotifications.notifyTurbulence(this, state.flight)
                    else FlightNotifications.cancelTurbulence(this)
                    shownTurbulenceStart = turbulenceStart
                }
            }

            is FlightState.Arrived -> {
                FlightNotifications.cancelTurbulence(this)
                if (!flightController.isUserWatching) FlightNotifications.notifyArrival(this, state.result)
                stop()
            }

            FlightState.Idle -> stop()
        }
    }

    private fun startInForeground(notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(FlightNotifications.ID_IN_FLIGHT, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(FlightNotifications.ID_IN_FLIGHT, notification)
        }
    }

    private fun stop() {
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val TAG = "FlightService"

        fun start(context: Context) {
            try {
                ContextCompat.startForegroundService(context, Intent(context, FlightService::class.java))
            } catch (e: IllegalStateException) {
                // Android 12+에서 앱이 백그라운드일 때 시작하면 거부된다.
                // 비행 자체는 Room에 저장돼 있으니 다음에 앱을 열 때 이어진다.
                Log.w(TAG, "Foreground Service를 시작하지 못했습니다", e)
            }
        }
    }
}
