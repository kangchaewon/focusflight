package com.flightfocus.app.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.flightfocus.app.domain.model.AircraftTier
import com.flightfocus.app.domain.model.FlightMode
import com.flightfocus.app.domain.model.FlightOutcome

@Entity(tableName = "flight_sessions", indices = [Index("startedAt")])
data class FlightSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val mode: FlightMode,
    val originIata: String,
    val destinationIata: String?,
    val aircraft: AircraftTier,
    val targetMinutes: Int?,
    val startedAt: Long,
    val endedAt: Long,
    val flownMinutes: Int,
    /** 일일 상한 적용 후 인정된 시간. 오늘 남은 상한 계산에 쓴다. */
    val creditedMinutes: Int,
    val outcome: FlightOutcome,
    val miles: Long,
)

/** 마일리지 잔액 = 비행으로 번 마일 합계 - 기종 해금에 쓴 마일 합계. 잔액은 따로 저장하지 않는다. */
@Entity(tableName = "aircraft_unlocks")
data class AircraftUnlockEntity(
    @PrimaryKey val tier: AircraftTier,
    val unlockedAt: Long,
    val cost: Long,
)
