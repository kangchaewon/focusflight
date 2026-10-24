package com.flightfocus.app.domain.flight

import com.flightfocus.app.domain.TestFixtures.BKK
import com.flightfocus.app.domain.TestFixtures.CJU
import com.flightfocus.app.domain.TestFixtures.GMP
import com.flightfocus.app.domain.TestFixtures.ICN
import com.flightfocus.app.domain.TestFixtures.NRT
import org.junit.Assert.assertEquals
import org.junit.Test

class FlightRoutesTest {

    @Test
    fun `대권거리가 실제 노선 거리와 오차 2퍼센트 이내`() {
        assertEquals(451.0, FlightRoutes.distanceKm(GMP, CJU), 9.0)
        assertEquals(1_250.0, FlightRoutes.distanceKm(ICN, NRT), 25.0)
        assertEquals(3_700.0, FlightRoutes.distanceKm(ICN, BKK), 74.0)
    }

    @Test
    fun `노선 비행 시간 = 거리 나누기 800kmh + 이착륙 5분`() {
        assertEquals(39, FlightRoutes.flightMinutes(GMP, CJU))
        assertEquals(FlightRoutes.flightMinutes(GMP, CJU), FlightRoutes.flightMinutes(CJU, GMP))
    }

    @Test
    fun `공부 시간을 거리로 환산`() {
        assertEquals(800.0, FlightRoutes.distanceForMinutes(60), 0.001)
    }
}
