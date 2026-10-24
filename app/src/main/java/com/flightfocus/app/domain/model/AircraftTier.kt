package com.flightfocus.app.domain.model

/**
 * 기종 등급. [maxMinutes]는 한 번에 비행할 수 있는 최대 시간(연료 한계)이다.
 * 해금 비용은 하루 3시간 공부 기준 협동체까지 약 1주, 광동체까지 약 3주가 걸리도록 잡았다.
 */
enum class AircraftTier(
    val displayName: String,
    val maxMinutes: Int,
    val unlockCost: Long,
) {
    LIGHT("경비행기", maxMinutes = 60, unlockCost = 0),
    REGIONAL_JET("리저널 제트", maxMinutes = 120, unlockCost = 3_000),
    NARROW_BODY("협동체", maxMinutes = 240, unlockCost = 12_000),
    WIDE_BODY("광동체", maxMinutes = 360, unlockCost = 40_000);

    companion object {
        val STARTER = LIGHT

        /** 해금된 기종 중 [minutes]를 비행할 수 있는 가장 작은 기종. 없으면 null. */
        fun smallestFor(minutes: Int, unlocked: Set<AircraftTier>): AircraftTier? =
            entries.firstOrNull { it in unlocked && it.maxMinutes >= minutes }

        /** 해금된 기종 중 가장 큰 기종. 부정기편(초시계)에 쓴다. */
        fun largestOf(unlocked: Set<AircraftTier>): AircraftTier =
            entries.last { it in unlocked || it == STARTER }
    }
}
