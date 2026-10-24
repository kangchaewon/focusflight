package com.flightfocus.app.data.db

import com.flightfocus.app.domain.TestFixtures.CJU
import com.flightfocus.app.domain.TestFixtures.GMP
import com.flightfocus.app.domain.TestFixtures.charter
import com.flightfocus.app.domain.TestFixtures.scheduled
import com.flightfocus.app.domain.model.ActiveFlight
import com.flightfocus.app.domain.model.AircraftTier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ActiveFlightMapperTest {

    private val airports = listOf(GMP, CJU).associateBy { it.iata }

    @Test
    fun `저장했다 복원하면 같은 비행이 나온다`() {
        val flights = listOf(
            ActiveFlight(scheduled(39), startedAtMillis = 1_000, turbulenceStartedAtMillis = 5_000, turbulenceTotalMillis = 700),
            ActiveFlight(charter(AircraftTier.REGIONAL_JET), startedAtMillis = 2_000),
        )

        flights.forEach { assertEquals(it, it.toEntity().toDomain(airports::get)) }
    }

    @Test
    fun `공항 데이터에서 사라진 공항이면 복원하지 않는다`() {
        val entity = ActiveFlight(scheduled(39), startedAtMillis = 0).toEntity()

        assertNull(entity.toDomain { if (it == "CJU") null else airports[it] })
    }

    @Test
    fun `규칙에 맞지 않는 값이면 복원하지 않는다`() {
        // 경비행기 최대 60분인데 목표 300분
        val broken = ActiveFlight(scheduled(39), startedAtMillis = 0).toEntity().copy(targetMinutes = 300)

        assertNull(broken.toDomain(airports::get))
    }
}
