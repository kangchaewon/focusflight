package com.flightfocus.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.flightfocus.app.domain.economy.HangarRules
import com.flightfocus.app.domain.economy.UnlockCheck
import com.flightfocus.app.domain.model.AircraftTier
import kotlinx.coroutines.flow.Flow

@Dao
abstract class FlightDao {
    @Insert
    protected abstract suspend fun insert(session: FlightSessionEntity): Long

    @Query("SELECT * FROM flight_sessions ORDER BY startedAt DESC")
    abstract fun observeAll(): Flow<List<FlightSessionEntity>>

    @Query("SELECT * FROM flight_sessions ORDER BY endedAt DESC LIMIT 1")
    abstract fun observeLatest(): Flow<FlightSessionEntity?>

    @Query("SELECT COALESCE(SUM(creditedMinutes), 0) FROM flight_sessions WHERE startedAt >= :from AND startedAt < :to")
    abstract suspend fun creditedMinutesBetween(from: Long, to: Long): Int

    @Query("SELECT COALESCE(SUM(miles), 0) FROM flight_sessions")
    abstract fun observeEarnedMiles(): Flow<Long>

    @Query("SELECT * FROM active_flight LIMIT 1")
    abstract suspend fun activeFlight(): ActiveFlightEntity?

    @Upsert
    abstract suspend fun saveActiveFlight(flight: ActiveFlightEntity)

    @Query("DELETE FROM active_flight")
    abstract suspend fun clearActiveFlight()

    /** 기록 저장과 진행 중 비행 삭제를 한 트랜잭션으로 묶어, 중간에 죽어도 같은 비행이 두 번 정산되지 않게 한다. */
    @Transaction
    open suspend fun completeFlight(session: FlightSessionEntity): Long {
        clearActiveFlight()
        return insert(session)
    }
}

@Dao
abstract class HangarDao {
    @Query("SELECT tier FROM aircraft_unlocks")
    abstract fun observeUnlockedTiers(): Flow<List<AircraftTier>>

    @Query("SELECT COALESCE(SUM(cost), 0) FROM aircraft_unlocks")
    abstract fun observeSpentMiles(): Flow<Long>

    @Query("SELECT tier FROM aircraft_unlocks")
    protected abstract suspend fun unlockedTiers(): List<AircraftTier>

    @Query("SELECT (SELECT COALESCE(SUM(miles), 0) FROM flight_sessions) - (SELECT COALESCE(SUM(cost), 0) FROM aircraft_unlocks)")
    protected abstract suspend fun balance(): Long

    @Insert
    protected abstract suspend fun insert(unlock: AircraftUnlockEntity)

    /** 잔액 확인과 차감을 한 트랜잭션으로 묶어 중복 해금을 막는다. */
    @Transaction
    open suspend fun unlock(tier: AircraftTier, nowMillis: Long): UnlockCheck {
        val unlocked = unlockedTiers().toSet() + AircraftTier.STARTER
        val check = HangarRules.check(tier, unlocked, balance())
        if (check == UnlockCheck.Allowed) insert(AircraftUnlockEntity(tier, nowMillis, tier.unlockCost))
        return check
    }
}
