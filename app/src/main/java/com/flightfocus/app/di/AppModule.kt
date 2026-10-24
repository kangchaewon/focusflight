package com.flightfocus.app.di

import android.content.Context
import androidx.room.Room
import com.flightfocus.app.data.SystemTimeProvider
import com.flightfocus.app.data.TimeProvider
import com.flightfocus.app.data.db.AppDatabase
import com.flightfocus.app.data.db.FlightDao
import com.flightfocus.app.data.db.HangarDao
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "flight_focus.db").build()

    @Provides
    fun provideFlightDao(db: AppDatabase): FlightDao = db.flightDao()

    @Provides
    fun provideHangarDao(db: AppDatabase): HangarDao = db.hangarDao()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class TimeModule {

    @Binds
    abstract fun bindTimeProvider(impl: SystemTimeProvider): TimeProvider
}
