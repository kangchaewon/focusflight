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

/**
 * 진행 중인 비행. 행은 항상 0개 또는 1개.
 * 프로세스가 죽어도 이 값만 있으면 타임스탬프로 비행을 정확히 이어갈 수 있다.
 */
@Entity(tableName = "active_flight")
data class ActiveFlightEntity(
    @PrimaryKey val id: Int = SINGLETON_ID,
    val mode: FlightMode,
    val originIata: String,
    val destinationIata: String?,
    val aircraft: AircraftTier,
    val targetMinutes: Int?,
    val startedAt: Long,
    val turbulenceStartedAt: Long?,
    val turbulenceTotalMillis: Long,
) {
    companion object {
        const val SINGLETON_ID = 0
    }
}

/** 마일리지 잔액 = 비행으로 번 마일 합계 - 기종 해금에 쓴 마일 합계. 잔액은 따로 저장하지 않는다. */
@Entity(tableName = "aircraft_unlocks")
data class AircraftUnlockEntity(
    @PrimaryKey val tier: AircraftTier,
    val unlockedAt: Long,
    val cost: Long,
)
