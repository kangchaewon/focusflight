package com.flightfocus.app.flight

import android.content.Context
import android.os.PowerManager
import com.flightfocus.app.data.FlightRepository
import com.flightfocus.app.data.TimeProvider
import com.flightfocus.app.domain.economy.Settlement
import com.flightfocus.app.domain.flight.FlightEvent
import com.flightfocus.app.domain.flight.FlightState
import com.flightfocus.app.domain.flight.FlightStateMachine
import com.flightfocus.app.domain.model.ActiveFlight
import com.flightfocus.app.domain.model.FlightPlan
import com.flightfocus.app.domain.model.FlightResult
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 진행 중인 비행을 앱 전역에서 하나만 관리한다. 비행 상태의 유일한 원천.
 *
 * - 비행 데이터가 바뀔 때마다 Room에 저장하고, 앱이 다시 뜨면 복원한다.
 * - 비행 중에는 [FlightService]를 띄워 프로세스를 살려 두고 알림으로 남은 시간을 보여 준다.
 */
@Singleton
class FlightController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: FlightRepository,
    private val time: TimeProvider,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val machine = FlightStateMachine()
    private var ticker: Job? = null

    /** DB 쓰기를 호출 순서대로 실행한다. 순서가 뒤집히면 정산 후 지운 비행이 되살아날 수 있다. */
    private val writeLock = Mutex()

    /** 사용자가 앱 안에 있는지. 화면을 꺼도 앱을 떠난 것으로 보지 않는다. */
    private var userInApp = false

    private val _state = MutableStateFlow<FlightState>(FlightState.Idle)
    val state: StateFlow<FlightState> = _state.asStateFlow()

    private val _settlement = MutableStateFlow<Settlement?>(null)

    /** 마지막 착륙의 정산 결과. 기록이 끝나기 전에는 null. */
    val settlement: StateFlow<Settlement?> = _settlement.asStateFlow()

    /** 사용자가 지금 앱 화면을 보고 있는지. 이때는 착륙 알림을 띄우지 않는다. */
    val isUserWatching: Boolean
        get() = userInApp && context.getSystemService(PowerManager::class.java).isInteractive

    init {
        scope.launch { restore() }
    }

    fun takeOff(plan: FlightPlan) = dispatch(FlightEvent.TakeOff(plan, time.nowMillis()))

    fun land() = dispatch(FlightEvent.Land(time.nowMillis()))

    fun onLeftApp() {
        userInApp = false
        dispatch(FlightEvent.LeftApp(time.nowMillis()))
    }

    fun onReturnedToApp() {
        userInApp = true
        dispatch(FlightEvent.ReturnedToApp(time.nowMillis()))
    }

    fun acknowledge() {
        dispatch(FlightEvent.Acknowledge(time.nowMillis()))
        _settlement.value = null
        FlightNotifications.cancelArrival(context)
    }

    private suspend fun restore() {
        val flight = withContext(Dispatchers.IO) { repository.loadActiveFlight() } ?: return
        // 복원하는 사이 사용자가 새로 이륙했다면 그 비행이 우선
        if (_state.value != FlightState.Idle) return
        transition(FlightState.Idle, FlightState.Airborne(flight))
        // 꺼져 있던 동안 목표 시간이 지났거나 유예를 넘겼으면 여기서 바로 착륙/회항한다.
        // onStart가 복원보다 먼저 불렸다면 복귀 이벤트를 다시 보내야 난기류가 풀린다.
        val now = time.nowMillis()
        dispatch(if (userInApp) FlightEvent.ReturnedToApp(now) else FlightEvent.Tick(now))
    }

    private fun dispatch(event: FlightEvent) {
        val before = _state.value
        val after = machine.reduce(before, event)
        if (after != before) transition(before, after)
    }

    private fun transition(before: FlightState, after: FlightState) {
        _state.value = after
        when (after) {
            is FlightState.Airborne -> {
                if (before !is FlightState.Airborne) {
                    startTicker()
                    FlightService.start(context)
                }
                persist(after.flight)
            }
            is FlightState.Arrived -> if (before is FlightState.Airborne) {
                ticker?.cancel()
                complete(after.result)
            }
            FlightState.Idle -> Unit
        }
    }

    private fun persist(flight: ActiveFlight) {
        scope.launch { writeLock.withLock { repository.saveActiveFlight(flight) } }
    }

    private fun complete(result: FlightResult) {
        scope.launch {
            _settlement.value = writeLock.withLock { repository.record(result) }
        }
    }

    private fun startTicker() {
        ticker?.cancel()
        ticker = scope.launch {
            while (isActive) {
                delay(TICK_MILLIS)
                dispatch(FlightEvent.Tick(time.nowMillis()))
            }
        }
    }

    private companion object {
        const val TICK_MILLIS = 1_000L
    }
}
