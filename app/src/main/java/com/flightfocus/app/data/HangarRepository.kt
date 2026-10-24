package com.flightfocus.app.data

import com.flightfocus.app.data.db.FlightDao
import com.flightfocus.app.data.db.HangarDao
import com.flightfocus.app.domain.economy.UnlockCheck
import com.flightfocus.app.domain.model.AircraftTier
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HangarRepository @Inject constructor(
    private val hangarDao: HangarDao,
    flightDao: FlightDao,
    private val time: TimeProvider,
) {
    val unlockedTiers: Flow<Set<AircraftTier>> =
        hangarDao.observeUnlockedTiers().map { it.toSet() + AircraftTier.STARTER }

    val balance: Flow<Long> =
        combine(flightDao.observeEarnedMiles(), hangarDao.observeSpentMiles()) { earned, spent -> earned - spent }

    suspend fun unlock(tier: AircraftTier): UnlockCheck = hangarDao.unlock(tier, time.nowMillis())
}
