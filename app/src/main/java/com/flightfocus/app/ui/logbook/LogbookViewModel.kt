package com.flightfocus.app.ui.logbook

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flightfocus.app.data.FlightRepository
import com.flightfocus.app.data.db.FlightSessionEntity
import com.flightfocus.app.domain.flight.FlightRoutes
import com.flightfocus.app.domain.model.FlightOutcome
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class LogbookUiState(
    val totalMinutes: Int = 0,
    val totalDistanceKm: Int = 0,
    val flightCount: Int = 0,
    val divertCount: Int = 0,
    val sessions: List<FlightSessionEntity> = emptyList(),
)

@HiltViewModel
class LogbookViewModel @Inject constructor(
    flightRepository: FlightRepository,
) : ViewModel() {

    val uiState: StateFlow<LogbookUiState> = flightRepository.observeSessions().map { sessions ->
        val minutes = sessions.sumOf { it.flownMinutes }
        LogbookUiState(
            totalMinutes = minutes,
            totalDistanceKm = FlightRoutes.distanceForMinutes(minutes).toInt(),
            flightCount = sessions.size,
            divertCount = sessions.count { it.outcome == FlightOutcome.DIVERTED },
            sessions = sessions,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LogbookUiState())
}
