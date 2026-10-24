package com.flightfocus.app.domain.economy

import com.flightfocus.app.domain.model.AircraftTier

sealed interface UnlockCheck {
    data object Allowed : UnlockCheck
    data object AlreadyUnlocked : UnlockCheck
    data class PreviousTierRequired(val previous: AircraftTier) : UnlockCheck
    data class NotEnoughMiles(val shortBy: Long) : UnlockCheck
}

/** 기종은 작은 것부터 순서대로 해금한다. */
object HangarRules {

    fun check(tier: AircraftTier, unlocked: Set<AircraftTier>, balance: Long): UnlockCheck {
        if (tier in unlocked) return UnlockCheck.AlreadyUnlocked
        val previous = AircraftTier.entries.getOrNull(tier.ordinal - 1)
        if (previous != null && previous !in unlocked) return UnlockCheck.PreviousTierRequired(previous)
        if (balance < tier.unlockCost) return UnlockCheck.NotEnoughMiles(tier.unlockCost - balance)
        return UnlockCheck.Allowed
    }
}
