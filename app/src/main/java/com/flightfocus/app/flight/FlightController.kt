package com.flightfocus.app.flight

import com.flightfocus.app.data.FlightRepository
import com.flightfocus.app.data.TimeProvider
import com.flightfocus.app.domain.economy.Settlement
import com.flightfocus.app.domain.flight.FlightEvent
import com.flightfocus.app.domain.flight.FlightState
import com.flightfocus.app.domain.flight.FlightStateMachine
import com.flightfocus.app.domain.model.FlightPlan
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
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 진행 중인 비행을 앱 전역에서 하나만 관리한다.
 *
 * 지금은 프로세스 메모리에만 상태가 있다.
 * TODO(2~3주차): Foreground Service로 옮기고 ActiveFlight를 Room에 저장해 프로세스 종료 후 복구.
 */
@Singleton
class FlightController @Inject constructor(
    private val repository: FlightRepository,
    private val time: TimeProvider,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val machine = FlightStateMachine()
    private var ticker: Job? = null

    private val _state = MutableStateFlow<FlightState>(FlightState.Idle)
    val state: StateFlow<FlightState> = _state.asStateFlow()

    private val _settlement = MutableStateFlow<Settlement?>(null)

    /** 마지막 착륙의 정산 결과. 기록이 끝나기 전에는 null. */
    val settlement: StateFlow<Settlement?> = _settlement.asStateFlow()

    fun takeOff(plan: FlightPlan) {
        dispatch(FlightEvent.TakeOff(plan, time.nowMillis()))
        startTicker()
    }

    fun land() = dispatch(FlightEvent.Land(time.nowMillis()))

    fun onLeftApp() = dispatch(FlightEvent.LeftApp(time.nowMillis()))

    fun onReturnedToApp() = dispatch(FlightEvent.ReturnedToApp(time.nowMillis()))

    fun acknowledge() {
        dispatch(FlightEvent.Acknowledge(time.nowMillis()))
        _settlement.value = null
    }

    private fun dispatch(event: FlightEvent) {
        val before = _state.value
        val after = machine.reduce(before, event)
        _state.value = after
        if (before !is FlightState.Arrived && after is FlightState.Arrived) {
            ticker?.cancel()
            scope.launch { _settlement.value = repository.record(after.result) }
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
