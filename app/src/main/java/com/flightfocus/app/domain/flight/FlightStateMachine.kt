package com.flightfocus.app.domain.flight

import com.flightfocus.app.domain.model.ActiveFlight
import com.flightfocus.app.domain.model.FlightMode
import com.flightfocus.app.domain.model.FlightOutcome
import com.flightfocus.app.domain.model.FlightPlan
import com.flightfocus.app.domain.model.FlightResult

sealed interface FlightState {
    data object Idle : FlightState
    data class Airborne(val flight: ActiveFlight) : FlightState
    data class Arrived(val result: FlightResult) : FlightState
}

sealed interface FlightEvent {
    val atMillis: Long

    data class TakeOff(val plan: FlightPlan, override val atMillis: Long) : FlightEvent
    data class LeftApp(override val atMillis: Long) : FlightEvent
    data class ReturnedToApp(override val atMillis: Long) : FlightEvent
    data class Tick(override val atMillis: Long) : FlightEvent
    data class Land(override val atMillis: Long) : FlightEvent

    /** 착륙 화면을 닫고 다음 비행을 준비. */
    data class Acknowledge(override val atMillis: Long) : FlightEvent
}

/**
 * 비행 상태 전이를 담당하는 순수 함수 모음.
 *
 * ```
 * Idle ─TakeOff▶ Airborne ─(목표 도달/Land)▶ Arrived ─Acknowledge▶ Idle
 *                  │  ▲
 *          LeftApp ▼  │ 유예 시간 내 복귀
 *              (난기류) ─유예 초과▶ Arrived(DIVERTED)
 * ```
 */
class FlightStateMachine(
    private val gracePeriodMillis: Long = DEFAULT_GRACE_PERIOD_MILLIS,
) {

    fun reduce(state: FlightState, event: FlightEvent): FlightState = when (state) {
        FlightState.Idle -> when (event) {
            is FlightEvent.TakeOff -> FlightState.Airborne(ActiveFlight(event.plan, event.atMillis))
            else -> state
        }

        is FlightState.Airborne -> reduceAirborne(state.flight, event)

        is FlightState.Arrived -> when (event) {
            is FlightEvent.Acknowledge -> FlightState.Idle
            else -> state
        }
    }

    private fun reduceAirborne(flight: ActiveFlight, event: FlightEvent): FlightState {
        // 유예 시간을 넘긴 난기류는 어떤 이벤트가 오든 회항으로 끝난다.
        // (앱이 잠들어 Tick을 놓쳤다가 늦게 깨어나도 같은 결과가 나오도록)
        divertIfGraceExpired(flight, event.atMillis)?.let { return it }

        return when (event) {
            is FlightEvent.LeftApp ->
                if (flight.inTurbulence) FlightState.Airborne(flight)
                else FlightState.Airborne(flight.copy(turbulenceStartedAtMillis = event.atMillis))

            is FlightEvent.ReturnedToApp -> FlightState.Airborne(flight.endTurbulence(event.atMillis))

            is FlightEvent.Tick -> landIfLimitReached(flight, event.atMillis) ?: FlightState.Airborne(flight)

            is FlightEvent.Land -> {
                val calm = flight.endTurbulence(event.atMillis)
                landIfLimitReached(calm, event.atMillis) ?: land(calm, event.atMillis)
            }

            is FlightEvent.TakeOff, is FlightEvent.Acknowledge -> FlightState.Airborne(flight)
        }
    }

    private fun divertIfGraceExpired(flight: ActiveFlight, nowMillis: Long): FlightState? {
        val turbulenceStart = flight.turbulenceStartedAtMillis ?: return null
        if (nowMillis - turbulenceStart <= gracePeriodMillis) return null
        // 난기류에 들어간 순간까지만 비행으로 인정한다.
        val flown = flight.flownMillis(turbulenceStart)
        val limit = flight.plan.limitMinutes * MILLIS_PER_MINUTE
        if (flown >= limit) return arrive(flight, limit, FlightOutcome.COMPLETED)
        return arrive(flight, flown, FlightOutcome.DIVERTED, endedAt = turbulenceStart + gracePeriodMillis)
    }

    private fun landIfLimitReached(flight: ActiveFlight, nowMillis: Long): FlightState? {
        val limit = flight.plan.limitMinutes * MILLIS_PER_MINUTE
        val flown = flight.flownMillis(nowMillis)
        if (flown < limit) return null
        return arrive(flight, limit, FlightOutcome.COMPLETED, endedAt = nowMillis - (flown - limit))
    }

    private fun land(flight: ActiveFlight, nowMillis: Long): FlightState {
        val outcome = when (flight.plan.mode) {
            FlightMode.SCHEDULED -> FlightOutcome.LANDED_EARLY
            FlightMode.CHARTER -> FlightOutcome.COMPLETED
        }
        return arrive(flight, flight.flownMillis(nowMillis), outcome, endedAt = nowMillis)
    }

    private fun arrive(
        flight: ActiveFlight,
        flownMillis: Long,
        outcome: FlightOutcome,
        endedAt: Long = flight.startedAtMillis + flight.turbulenceTotalMillis + flownMillis,
    ) = FlightState.Arrived(
        FlightResult(
            plan = flight.plan,
            startedAtMillis = flight.startedAtMillis,
            endedAtMillis = endedAt,
            flownMinutes = (flownMillis / MILLIS_PER_MINUTE).toInt(),
            outcome = outcome,
        ),
    )

    private fun ActiveFlight.endTurbulence(nowMillis: Long): ActiveFlight {
        val start = turbulenceStartedAtMillis ?: return this
        return copy(
            turbulenceStartedAtMillis = null,
            turbulenceTotalMillis = turbulenceTotalMillis + (nowMillis - start),
        )
    }

    companion object {
        const val DEFAULT_GRACE_PERIOD_MILLIS = 20_000L
        private const val MILLIS_PER_MINUTE = 60_000L
    }
}
