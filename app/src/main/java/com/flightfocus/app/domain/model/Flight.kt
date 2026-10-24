package com.flightfocus.app.domain.model

/** 정기편 = 카운트다운 타이머, 부정기편 = 카운트업 초시계. */
enum class FlightMode { SCHEDULED, CHARTER }

enum class FlightOutcome {
    /** 정기편 목표 시간 완주, 또는 부정기편 착륙(연료 한계 자동 착륙 포함). */
    COMPLETED,

    /** 정기편을 목표 시간 전에 직접 착륙. */
    LANDED_EARLY,

    /** 다른 앱으로 이탈한 채 유예 시간을 넘김 (Focus Lock). */
    DIVERTED,
}

data class FlightPlan(
    val mode: FlightMode,
    val origin: Airport,
    val destination: Airport?,
    val aircraft: AircraftTier,
    val targetMinutes: Int?,
) {
    init {
        when (mode) {
            FlightMode.SCHEDULED -> {
                requireNotNull(destination) { "정기편은 목적지가 필요합니다" }
                requireNotNull(targetMinutes) { "정기편은 목표 시간이 필요합니다" }
                require(targetMinutes in 1..aircraft.maxMinutes) {
                    "${aircraft.displayName}의 최대 비행 시간(${aircraft.maxMinutes}분)을 넘습니다"
                }
            }
            FlightMode.CHARTER -> require(targetMinutes == null) { "부정기편은 목표 시간이 없습니다" }
        }
    }

    /** 자동 착륙 기준: 정기편은 목표 시간, 부정기편은 기종의 연료 한계. */
    val limitMinutes: Int get() = targetMinutes ?: aircraft.maxMinutes
}

/**
 * 비행 중 상태. 시간은 tick을 세지 않고 타임스탬프로만 계산한다.
 * 그래서 프로세스가 죽었다 살아나도 이 값만 복원하면 정확히 이어진다.
 */
data class ActiveFlight(
    val plan: FlightPlan,
    val startedAtMillis: Long,
    val turbulenceStartedAtMillis: Long? = null,
    val turbulenceTotalMillis: Long = 0,
) {
    val inTurbulence: Boolean get() = turbulenceStartedAtMillis != null

    /** 실제 집중한 시간. 다른 앱에 나가 있던 시간(난기류)은 빼고 센다. */
    fun flownMillis(nowMillis: Long): Long {
        val ongoingTurbulence = turbulenceStartedAtMillis?.let { nowMillis - it } ?: 0
        return (nowMillis - startedAtMillis - turbulenceTotalMillis - ongoingTurbulence).coerceAtLeast(0)
    }
}

data class FlightResult(
    val plan: FlightPlan,
    val startedAtMillis: Long,
    val endedAtMillis: Long,
    val flownMinutes: Int,
    val outcome: FlightOutcome,
)
