package com.flightfocus.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flightfocus.app.data.FlightRepository
import com.flightfocus.app.data.HangarRepository
import com.flightfocus.app.data.airport.AirportRepository
import com.flightfocus.app.domain.flight.FlightRoutes
import com.flightfocus.app.domain.model.AircraftTier
import com.flightfocus.app.domain.model.Airport
import com.flightfocus.app.domain.model.FlightMode
import com.flightfocus.app.domain.model.FlightPlan
import com.flightfocus.app.flight.FlightController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class RouteOption(
    val destination: Airport,
    val minutes: Int,
    val distanceKm: Int,
    /** 이 노선을 비행할 수 있는 가장 작은 해금 기종. null이면 아직 못 감. */
    val aircraft: AircraftTier?,
)

data class HomeUiState(
    val origin: Airport? = null,
    val balance: Long = 0,
    val charterAircraft: AircraftTier = AircraftTier.STARTER,
    val routes: List<RouteOption> = emptyList(),
    val selected: RouteOption? = null,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val airports: AirportRepository,
    flightRepository: FlightRepository,
    hangarRepository: HangarRepository,
    private val flightController: FlightController,
) : ViewModel() {

    private val selectedIata = MutableStateFlow<String?>(null)

    val uiState: StateFlow<HomeUiState> = combine(
        flightRepository.observeCurrentAirport(),
        hangarRepository.unlockedTiers,
        hangarRepository.balance,
        selectedIata,
    ) { origin, unlocked, balance, selected ->
        val routes = airports.airports
            .filter { it.iata != origin.iata }
            .map { destination ->
                val minutes = FlightRoutes.flightMinutes(origin, destination)
                RouteOption(
                    destination = destination,
                    minutes = minutes,
                    distanceKm = FlightRoutes.distanceKm(origin, destination).toInt(),
                    aircraft = AircraftTier.smallestFor(minutes, unlocked),
                )
            }
            .filter { it.minutes <= AircraftTier.entries.last().maxMinutes }
            .sortedBy { it.minutes }
        HomeUiState(
            origin = origin,
            balance = balance,
            charterAircraft = AircraftTier.largestOf(unlocked),
            routes = routes,
            selected = routes.firstOrNull { it.destination.iata == selected && it.aircraft != null },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun select(route: RouteOption) {
        selectedIata.value = route.destination.iata.takeIf { it != selectedIata.value }
    }

    /** @return 이륙했으면 true */
    fun takeOffScheduled(): Boolean {
        val state = uiState.value
        val origin = state.origin ?: return false
        val route = state.selected ?: return false
        val aircraft = route.aircraft ?: return false
        flightController.takeOff(FlightPlan(FlightMode.SCHEDULED, origin, route.destination, aircraft, route.minutes))
        selectedIata.value = null
        return true
    }

    fun takeOffCharter(): Boolean {
        val state = uiState.value
        val origin = state.origin ?: return false
        flightController.takeOff(FlightPlan(FlightMode.CHARTER, origin, null, state.charterAircraft, null))
        return true
    }
}
