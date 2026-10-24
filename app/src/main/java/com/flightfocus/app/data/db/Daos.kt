package com.flightfocus.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.flightfocus.app.domain.economy.HangarRules
import com.flightfocus.app.domain.economy.UnlockCheck
import com.flightfocus.app.domain.model.AircraftTier
import kotlinx.coroutines.flow.Flow

@Dao
interface FlightDao {
    @Insert
    suspend fun insert(session: FlightSessionEntity): Long

    @Query("SELECT * FROM flight_sessions ORDER BY startedAt DESC")
    fun observeAll(): Flow<List<FlightSessionEntity>>

    @Query("SELECT * FROM flight_sessions ORDER BY endedAt DESC LIMIT 1")
    fun observeLatest(): Flow<FlightSessionEntity?>

    @Query("SELECT COALESCE(SUM(creditedMinutes), 0) FROM flight_sessions WHERE startedAt >= :from AND startedAt < :to")
    suspend fun creditedMinutesBetween(from: Long, to: Long): Int

    @Query("SELECT COALESCE(SUM(miles), 0) FROM flight_sessions")
    fun observeEarnedMiles(): Flow<Long>
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
