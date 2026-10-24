package com.flightfocus.app.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [FlightSessionEntity::class, AircraftUnlockEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun flightDao(): FlightDao
    abstract fun hangarDao(): HangarDao
}
