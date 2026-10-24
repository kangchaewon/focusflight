package com.flightfocus.app.data.db

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [FlightSessionEntity::class, AircraftUnlockEntity::class, ActiveFlightEntity::class],
    version = 2,
    exportSchema = true,
    autoMigrations = [
        // v2: 진행 중 비행 저장(active_flight) 추가
        AutoMigration(from = 1, to = 2),
    ],
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun flightDao(): FlightDao
    abstract fun hangarDao(): HangarDao
}
