package com.flightfocus.app.domain.economy

import com.flightfocus.app.domain.TestFixtures.charter
import com.flightfocus.app.domain.TestFixtures.scheduled
import com.flightfocus.app.domain.model.FlightOutcome
import com.flightfocus.app.domain.model.FlightPlan
import com.flightfocus.app.domain.model.FlightResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MileageCalculatorTest {

    private fun result(plan: FlightPlan, minutes: Int, outcome: FlightOutcome) =
        FlightResult(plan, startedAtMillis = 0, endedAtMillis = 0, flownMinutes = minutes, outcome = outcome)

    @Test
    fun `정기편 완주는 기본 마일리지에 20퍼센트 보너스`() {
        val s = MileageCalculator.settle(result(scheduled(50), 50, FlightOutcome.COMPLETED), creditedMinutesToday = 0)

        assertEquals(500, s.baseMiles)
        assertEquals(100, s.onTimeBonusMiles)
        assertEquals(600, s.totalMiles)
    }

    @Test
    fun `조기 착륙과 부정기편은 보너스가 없다`() {
        val early = MileageCalculator.settle(result(scheduled(50), 30, FlightOutcome.LANDED_EARLY), 0)
        val charter = MileageCalculator.settle(result(charter(), 30, FlightOutcome.COMPLETED), 0)

        assertEquals(300, early.totalMiles)
        assertEquals(300, charter.totalMiles)
    }

    @Test
    fun `회항은 절반만 지급`() {
        val s = MileageCalculator.settle(result(scheduled(50), 21, FlightOutcome.DIVERTED), 0)

        assertEquals(105, s.totalMiles)
    }

    @Test
    fun `일일 상한을 넘는 시간은 인정하지 않는다`() {
        val s = MileageCalculator.settle(
            result(charter(), 60, FlightOutcome.COMPLETED),
            creditedMinutesToday = MileageCalculator.DAILY_CAP_MINUTES - 20,
        )

        assertEquals(20, s.creditedMinutes)
        assertEquals(200, s.totalMiles)
    }

    @Test
    fun `상한에 이미 도달했으면 0마일`() {
        val s = MileageCalculator.settle(
            result(charter(), 60, FlightOutcome.COMPLETED),
            creditedMinutesToday = MileageCalculator.DAILY_CAP_MINUTES + 5,
        )

        assertEquals(0, s.creditedMinutes)
        assertEquals(0, s.totalMiles)
    }

    @Test
    fun `하루 3시간 정기편 기준 해금 소요 일수가 설계 의도와 맞다`() {
        // 25분 정기편 완주 = 300마일, 하루 7회(175분) ≈ 2,100마일
        val perDay = MileageCalculator.settle(result(scheduled(25), 25, FlightOutcome.COMPLETED), 0).totalMiles * 7
        val daysToNarrowBody = (3_000 + 12_000) / perDay.toDouble()
        val daysToWideBody = (3_000 + 12_000 + 40_000) / perDay.toDouble()

        assertTrue("협동체까지 $daysToNarrowBody 일", daysToNarrowBody in 5.0..10.0)
        assertTrue("광동체까지 $daysToWideBody 일", daysToWideBody in 20.0..35.0)
    }
}
