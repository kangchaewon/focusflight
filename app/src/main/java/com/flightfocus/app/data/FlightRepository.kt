package com.flightfocus.app.data

import com.flightfocus.app.data.airport.AirportRepository
import com.flightfocus.app.data.db.FlightDao
import com.flightfocus.app.data.db.FlightSessionEntity
import com.flightfocus.app.data.db.toDomain
import com.flightfocus.app.data.db.toEntity
import com.flightfocus.app.domain.economy.MileageCalculator
import com.flightfocus.app.domain.economy.Settlement
import com.flightfocus.app.domain.model.ActiveFlight
import com.flightfocus.app.domain.model.Airport
import com.flightfocus.app.domain.model.FlightOutcome
import com.flightfocus.app.domain.model.FlightResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FlightRepository @Inject constructor(
    private val flightDao: FlightDao,
    private val airports: AirportRepository,
    private val time: TimeProvider,
) {
    fun observeSessions(): Flow<List<FlightSessionEntity>> = flightDao.observeAll()

    /** 지금 비행기가 있는 공항. 목적지에 완주했을 때만 이동하고, 조기 착륙·회항은 출발지에 남는다. */
    fun observeCurrentAirport(): Flow<Airport> = flightDao.observeLatest().map { latest ->
        val iata = when {
            latest == null -> AirportRepository.HOME_IATA
            latest.outcome == FlightOutcome.COMPLETED && latest.destinationIata != null -> latest.destinationIata
            else -> latest.originIata
        }
        airports.find(iata) ?: airports.home
    }

    /** 저장된 진행 중 비행. 복원할 수 없는 값이면 지우고 null. */
    suspend fun loadActiveFlight(): ActiveFlight? {
        val entity = flightDao.activeFlight() ?: return null
        return entity.toDomain(airports::find) ?: run {
            flightDao.clearActiveFlight()
            null
        }
    }

    suspend fun saveActiveFlight(flight: ActiveFlight) = flightDao.saveActiveFlight(flight.toEntity())

    /** 비행 결과를 정산해 기록하고, 진행 중 비행을 지운다. */
    suspend fun record(result: FlightResult): Settlement {
        val today = time.todayRangeMillis()
        val creditedToday = flightDao.creditedMinutesBetween(today.first, today.last + 1)
        val settlement = MileageCalculator.settle(result, creditedToday)
        flightDao.completeFlight(
            FlightSessionEntity(
                mode = result.plan.mode,
                originIata = result.plan.origin.iata,
                destinationIata = result.plan.destination?.iata,
                aircraft = result.plan.aircraft,
                targetMinutes = result.plan.targetMinutes,
                startedAt = result.startedAtMillis,
                endedAt = result.endedAtMillis,
                flownMinutes = result.flownMinutes,
                creditedMinutes = settlement.creditedMinutes,
                outcome = result.outcome,
                miles = settlement.totalMiles,
            ),
        )
        return settlement
    }
}
