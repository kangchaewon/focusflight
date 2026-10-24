package com.flightfocus.app.ui.hangar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flightfocus.app.data.HangarRepository
import com.flightfocus.app.domain.economy.HangarRules
import com.flightfocus.app.domain.economy.UnlockCheck
import com.flightfocus.app.domain.model.AircraftTier
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HangarSlot(val tier: AircraftTier, val check: UnlockCheck)

data class HangarUiState(
    val balance: Long = 0,
    val slots: List<HangarSlot> = emptyList(),
)

@HiltViewModel
class HangarViewModel @Inject constructor(
    private val hangarRepository: HangarRepository,
) : ViewModel() {

    val uiState: StateFlow<HangarUiState> = combine(
        hangarRepository.unlockedTiers,
        hangarRepository.balance,
    ) { unlocked, balance ->
        HangarUiState(
            balance = balance,
            slots = AircraftTier.entries.map { HangarSlot(it, HangarRules.check(it, unlocked, balance)) },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HangarUiState())

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun unlock(tier: AircraftTier) {
        viewModelScope.launch {
            _message.value = when (val result = hangarRepository.unlock(tier)) {
                UnlockCheck.Allowed -> "${tier.displayName}을(를) 격납고에 들였습니다 ✈️"
                UnlockCheck.AlreadyUnlocked -> null
                is UnlockCheck.PreviousTierRequired -> "${result.previous.displayName}부터 해금해야 합니다"
                is UnlockCheck.NotEnoughMiles -> "마일리지가 ${result.shortBy} 마일 부족합니다"
            }
        }
    }

    fun messageShown() {
        _message.value = null
    }
}
