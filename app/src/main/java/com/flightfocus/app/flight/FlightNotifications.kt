package com.flightfocus.app.flight

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.flightfocus.app.MainActivity
import com.flightfocus.app.R
import com.flightfocus.app.domain.flight.FlightStateMachine
import com.flightfocus.app.domain.model.ActiveFlight
import com.flightfocus.app.domain.model.FlightMode
import com.flightfocus.app.domain.model.FlightResult
import com.flightfocus.app.ui.durationText
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object FlightNotifications {
    private const val CHANNEL_FLIGHT = "flight"
    private const val CHANNEL_ALERT = "alert"

    const val ID_IN_FLIGHT = 1
    private const val ID_TURBULENCE = 2
    private const val ID_ARRIVAL = 3

    private const val GRACE_MILLIS = FlightStateMachine.DEFAULT_GRACE_PERIOD_MILLIS

    fun createChannels(context: Context) {
        context.manager().createNotificationChannels(
            listOf(
                NotificationChannel(CHANNEL_FLIGHT, "비행 중", NotificationManager.IMPORTANCE_LOW).apply {
                    description = "진행 중인 비행의 남은 시간"
                    setShowBadge(false)
                },
                NotificationChannel(CHANNEL_ALERT, "난기류·도착", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "앱을 벗어났을 때의 경고와 착륙 결과"
                },
            ),
        )
    }

    /** 비행 중 상단에 계속 떠 있는 알림. 시간은 시스템 크로노미터가 그려서 매초 갱신할 필요가 없다. */
    fun inFlight(context: Context, flight: ActiveFlight): Notification {
        val plan = flight.plan
        val builder = base(context, CHANNEL_FLIGHT)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setShowWhen(true)
            .setUsesChronometer(true)

        val turbulenceStart = flight.turbulenceStartedAtMillis
        // 난기류 시간만큼 도착이 밀린다
        val cruiseBase = flight.startedAtMillis + flight.turbulenceTotalMillis
        when {
            turbulenceStart != null -> builder
                .setContentTitle("⚠️ 난기류 · 앱으로 돌아오세요")
                .setContentText("돌아오지 않으면 회항합니다")
                .setWhen(turbulenceStart + GRACE_MILLIS)
                .setChronometerCountDown(true)

            plan.mode == FlightMode.SCHEDULED -> {
                val arrival = cruiseBase + plan.limitMinutes * 60_000L
                builder
                    .setContentTitle("${plan.routeText()} 비행 중")
                    .setContentText("${plan.aircraft.displayName} · ${timeFormat.format(Date(arrival))} 도착 예정")
                    .setWhen(arrival)
                    .setChronometerCountDown(true)
            }

            else -> builder
                .setContentTitle("${plan.origin.nameKo} 부정기편 비행 중")
                .setContentText("${plan.aircraft.displayName} · 연료 한계 ${plan.aircraft.maxMinutes.durationText()}")
                .setWhen(cruiseBase)
                .setChronometerCountDown(false)
        }
        return builder.build()
    }

    /** 비행 정보를 읽기 전 서비스가 먼저 떠야 할 때 쓰는 임시 알림. */
    fun preparing(context: Context): Notification =
        base(context, CHANNEL_FLIGHT).setContentTitle("비행 준비 중").setOngoing(true).build()

    fun notifyTurbulence(context: Context, flight: ActiveFlight) {
        val start = flight.turbulenceStartedAtMillis ?: return
        context.manager().notify(
            ID_TURBULENCE,
            base(context, CHANNEL_ALERT)
                .setContentTitle("⚠️ 난기류 발생!")
                .setContentText("${GRACE_MILLIS / 1000}초 안에 돌아오지 않으면 회항합니다")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setAutoCancel(true)
                .setTimeoutAfter(GRACE_MILLIS)
                .setShowWhen(true)
                .setUsesChronometer(true)
                .setWhen(start + GRACE_MILLIS)
                .setChronometerCountDown(true)
                .build(),
        )
    }

    fun cancelTurbulence(context: Context) = context.manager().cancel(ID_TURBULENCE)

    fun notifyArrival(context: Context, result: FlightResult) {
        val (title, subtitle) = result.headline()
        context.manager().notify(
            ID_ARRIVAL,
            base(context, CHANNEL_ALERT)
                .setContentTitle(title)
                .setContentText("$subtitle · ${result.flownMinutes.durationText()} 비행. 눌러서 정산 확인")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .build(),
        )
    }

    fun cancelArrival(context: Context) = context.manager().cancel(ID_ARRIVAL)

    private fun base(context: Context, channel: String) =
        NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_stat_flight)
            .setContentIntent(openApp(context))

    /** 런처 아이콘을 누른 것처럼 기존 작업을 앞으로 가져온다. */
    private fun openApp(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .setAction(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LAUNCHER)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    private fun Context.manager(): NotificationManager = getSystemService(NotificationManager::class.java)

    private val timeFormat = SimpleDateFormat("HH:mm", Locale.KOREA)
}
