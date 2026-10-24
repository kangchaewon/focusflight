package com.flightfocus.app.domain.economy

import com.flightfocus.app.domain.model.AircraftTier
import com.flightfocus.app.domain.model.AircraftTier.LIGHT
import com.flightfocus.app.domain.model.AircraftTier.NARROW_BODY
import com.flightfocus.app.domain.model.AircraftTier.REGIONAL_JET
import com.flightfocus.app.domain.model.AircraftTier.WIDE_BODY
import org.junit.Assert.assertEquals
import org.junit.Test

class HangarRulesTest {

    @Test
    fun `마일리지가 충분하고 이전 기종이 있으면 해금 가능`() {
        assertEquals(UnlockCheck.Allowed, HangarRules.check(REGIONAL_JET, setOf(LIGHT), balance = 3_000))
    }

    @Test
    fun `이전 기종 없이 건너뛸 수 없다`() {
        assertEquals(
            UnlockCheck.PreviousTierRequired(REGIONAL_JET),
            HangarRules.check(NARROW_BODY, setOf(LIGHT), balance = 100_000),
        )
    }

    @Test
    fun `마일리지가 모자라면 부족분을 알려준다`() {
        assertEquals(UnlockCheck.NotEnoughMiles(500), HangarRules.check(REGIONAL_JET, setOf(LIGHT), balance = 2_500))
    }

    @Test
    fun `노선 시간에 맞는 가장 작은 기종을 고른다`() {
        val unlocked = setOf(LIGHT, REGIONAL_JET, NARROW_BODY)

        assertEquals(LIGHT, AircraftTier.smallestFor(39, unlocked))
        assertEquals(NARROW_BODY, AircraftTier.smallestFor(150, unlocked))
        assertEquals(null, AircraftTier.smallestFor(300, unlocked))
        assertEquals(NARROW_BODY, AircraftTier.largestOf(unlocked))
        assertEquals(LIGHT, AircraftTier.largestOf(emptySet()))
        assertEquals(WIDE_BODY, AircraftTier.largestOf(AircraftTier.entries.toSet()))
    }
}
