package com.flightfocus.app.domain.economy

import com.flightfocus.app.domain.model.FlightMode
import com.flightfocus.app.domain.model.FlightOutcome
import com.flightfocus.app.domain.model.FlightResult
import kotlin.math.roundToLong

data class Settlement(
    /** 일일 상한을 적용한 뒤 인정된 시간. */
    val creditedMinutes: Int,
    val baseMiles: Long,
    val onTimeBonusMiles: Long,
    val divertPenaltyMiles: Long,
) {
    val totalMiles: Long get() = baseMiles + onTimeBonusMiles - divertPenaltyMiles
}

object MileageCalculator {
    const val MILES_PER_MINUTE = 10L
    const val ON_TIME_BONUS_RATE = 0.20
    const val DIVERT_PENALTY_RATE = 0.50

    /** 조종사 의무 휴식제: 하루 14시간을 넘는 비행은 마일리지를 주지 않는다. */
    const val DAILY_CAP_MINUTES = 14 * 60

    fun settle(result: FlightResult, creditedMinutesToday: Int): Settlement {
        val remainingToday = (DAILY_CAP_MINUTES - creditedMinutesToday).coerceAtLeast(0)
        val credited = result.flownMinutes.coerceAtMost(remainingToday)
        val base = credited * MILES_PER_MINUTE

        val onTime = result.outcome == FlightOutcome.COMPLETED && result.plan.mode == FlightMode.SCHEDULED
        val bonus = if (onTime) (base * ON_TIME_BONUS_RATE).roundToLong() else 0
        val penalty = if (result.outcome == FlightOutcome.DIVERTED) (base * DIVERT_PENALTY_RATE).roundToLong() else 0

        return Settlement(credited, base, bonus, penalty)
    }
}
