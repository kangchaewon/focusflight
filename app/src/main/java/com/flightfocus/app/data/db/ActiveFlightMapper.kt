package com.flightfocus.app.data.db

import com.flightfocus.app.domain.model.ActiveFlight
import com.flightfocus.app.domain.model.Airport
import com.flightfocus.app.domain.model.FlightPlan

fun ActiveFlight.toEntity() = ActiveFlightEntity(
    mode = plan.mode,
    originIata = plan.origin.iata,
    destinationIata = plan.destination?.iata,
    aircraft = plan.aircraft,
    targetMinutes = plan.targetMinutes,
    startedAt = startedAtMillis,
    turbulenceStartedAt = turbulenceStartedAtMillis,
    turbulenceTotalMillis = turbulenceTotalMillis,
)

/**
 * 저장된 비행을 복원한다. 공항 데이터가 바뀌었거나 값이 규칙에 맞지 않으면 null.
 */
fun ActiveFlightEntity.toDomain(findAirport: (String) -> Airport?): ActiveFlight? {
    val origin = findAirport(originIata) ?: return null
    val destination = destinationIata?.let { findAirport(it) ?: return null }
    val plan = runCatching { FlightPlan(mode, origin, destination, aircraft, targetMinutes) }.getOrNull() ?: return null
    return ActiveFlight(plan, startedAt, turbulenceStartedAt, turbulenceTotalMillis)
}
