package com.flightfocus.app.domain

import com.flightfocus.app.domain.model.AircraftTier
import com.flightfocus.app.domain.model.Airport
import com.flightfocus.app.domain.model.FlightMode
import com.flightfocus.app.domain.model.FlightPlan

object TestFixtures {
    val GMP = Airport("GMP", "김포", "Gimpo", "대한민국", 37.5583, 126.7906)
    val CJU = Airport("CJU", "제주", "Jeju", "대한민국", 33.5113, 126.4930)
    val ICN = Airport("ICN", "인천", "Incheon", "대한민국", 37.4602, 126.4407)
    val NRT = Airport("NRT", "도쿄 나리타", "Tokyo Narita", "일본", 35.7647, 140.3864)
    val BKK = Airport("BKK", "방콕 수완나품", "Bangkok Suvarnabhumi", "태국", 13.6900, 100.7501)

    fun scheduled(minutes: Int, aircraft: AircraftTier = AircraftTier.LIGHT) =
        FlightPlan(FlightMode.SCHEDULED, GMP, CJU, aircraft, minutes)

    fun charter(aircraft: AircraftTier = AircraftTier.LIGHT) =
        FlightPlan(FlightMode.CHARTER, GMP, null, aircraft, null)

    const val MIN = 60_000L
    const val SEC = 1_000L
}
