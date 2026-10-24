package com.flightfocus.app.domain.flight

import com.flightfocus.app.domain.TestFixtures.MIN
import com.flightfocus.app.domain.TestFixtures.SEC
import com.flightfocus.app.domain.TestFixtures.charter
import com.flightfocus.app.domain.TestFixtures.scheduled
import com.flightfocus.app.domain.model.FlightOutcome
import com.flightfocus.app.domain.model.FlightPlan
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FlightStateMachineTest {

    private val machine = FlightStateMachine(gracePeriodMillis = 20 * SEC)
    private val t0 = 1_000_000L

    private fun airborne(plan: FlightPlan): FlightState =
        machine.reduce(FlightState.Idle, FlightEvent.TakeOff(plan, t0))

    private fun FlightState.on(vararg events: FlightEvent): FlightState =
        events.fold(this) { state, event -> machine.reduce(state, event) }

    private fun FlightState.result() = (this as FlightState.Arrived).result

    @Test
    fun `정기편은 목표 시간에 도달하면 COMPLETED로 자동 착륙한다`() {
        val state = airborne(scheduled(25)).on(FlightEvent.Tick(t0 + 25 * MIN + 3 * SEC))

        val result = state.result()
        assertEquals(FlightOutcome.COMPLETED, result.outcome)
        assertEquals(25, result.flownMinutes)
        assertEquals(t0 + 25 * MIN, result.endedAtMillis)
    }

    @Test
    fun `정기편을 목표 전에 착륙하면 LANDED_EARLY`() {
        val result = airborne(scheduled(25)).on(FlightEvent.Land(t0 + 10 * MIN + 30 * SEC)).result()

        assertEquals(FlightOutcome.LANDED_EARLY, result.outcome)
        assertEquals(10, result.flownMinutes)
    }

    @Test
    fun `부정기편 직접 착륙은 COMPLETED`() {
        val result = airborne(charter()).on(FlightEvent.Land(t0 + 42 * MIN)).result()

        assertEquals(FlightOutcome.COMPLETED, result.outcome)
        assertEquals(42, result.flownMinutes)
    }

    @Test
    fun `부정기편은 기종 연료 한계에서 자동 착륙한다`() {
        val result = airborne(charter()).on(FlightEvent.Tick(t0 + 61 * MIN)).result()

        assertEquals(FlightOutcome.COMPLETED, result.outcome)
        assertEquals(60, result.flownMinutes)
    }

    @Test
    fun `유예 시간 안에 돌아오면 비행이 이어지고 나가 있던 시간은 빠진다`() {
        val state = airborne(scheduled(25)).on(
            FlightEvent.LeftApp(t0 + 5 * MIN),
            FlightEvent.ReturnedToApp(t0 + 5 * MIN + 15 * SEC),
            FlightEvent.Tick(t0 + 25 * MIN + 5 * SEC),
        )

        // 15초를 뺐으므로 25분 5초 시점엔 아직 비행 중
        assertTrue(state is FlightState.Airborne)
        val result = state.on(FlightEvent.Tick(t0 + 25 * MIN + 15 * SEC)).result()
        assertEquals(FlightOutcome.COMPLETED, result.outcome)
        assertEquals(t0 + 25 * MIN + 15 * SEC, result.endedAtMillis)
    }

    @Test
    fun `유예 시간을 넘기면 이탈 시점까지만 인정하고 회항한다`() {
        val result = airborne(scheduled(25)).on(
            FlightEvent.LeftApp(t0 + 7 * MIN),
            FlightEvent.Tick(t0 + 7 * MIN + 21 * SEC),
        ).result()

        assertEquals(FlightOutcome.DIVERTED, result.outcome)
        assertEquals(7, result.flownMinutes)
        assertEquals(t0 + 7 * MIN + 20 * SEC, result.endedAtMillis)
    }

    @Test
    fun `Tick을 놓친 채 한참 뒤 복귀해도 회항으로 처리된다`() {
        val result = airborne(scheduled(25)).on(
            FlightEvent.LeftApp(t0 + 3 * MIN),
            FlightEvent.ReturnedToApp(t0 + 30 * MIN),
        ).result()

        assertEquals(FlightOutcome.DIVERTED, result.outcome)
        assertEquals(3, result.flownMinutes)
    }

    @Test
    fun `난기류 중에는 비행 시간이 흐르지 않는다`() {
        val state = airborne(scheduled(25)).on(
            FlightEvent.LeftApp(t0 + 24 * MIN + 50 * SEC),
            FlightEvent.Tick(t0 + 25 * MIN + 5 * SEC),
        )

        assertTrue(state is FlightState.Airborne)
    }

    @Test
    fun `도착 상태는 Acknowledge로만 Idle로 돌아간다`() {
        val arrived = airborne(charter()).on(FlightEvent.Land(t0 + MIN))

        assertEquals(arrived, arrived.on(FlightEvent.Tick(t0 + 2 * MIN)))
        assertEquals(FlightState.Idle, arrived.on(FlightEvent.Acknowledge(t0 + 2 * MIN)))
    }

    @Test
    fun `앱이 꺼진 사이 목표 시간이 지났으면 복원 직후 정확한 시각으로 착륙한다`() {
        // 난기류 10초를 겪고 복귀한 뒤 프로세스가 죽었다가 2시간 뒤 복원
        val restored = airborne(scheduled(25)).on(
            FlightEvent.LeftApp(t0 + 3 * MIN),
            FlightEvent.ReturnedToApp(t0 + 3 * MIN + 10 * SEC),
        )

        val result = restored.on(FlightEvent.Tick(t0 + 120 * MIN)).result()
        assertEquals(FlightOutcome.COMPLETED, result.outcome)
        assertEquals(25, result.flownMinutes)
        assertEquals(t0 + 25 * MIN + 10 * SEC, result.endedAtMillis)
    }

    @Test
    fun `난기류 중 프로세스가 죽었다가 복원되면 회항으로 처리한다`() {
        val restored = airborne(scheduled(25)).on(FlightEvent.LeftApp(t0 + 4 * MIN))

        val result = restored.on(FlightEvent.ReturnedToApp(t0 + 60 * MIN)).result()
        assertEquals(FlightOutcome.DIVERTED, result.outcome)
        assertEquals(4, result.flownMinutes)
    }
}
