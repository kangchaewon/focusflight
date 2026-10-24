package com.flightfocus.app.ui.cockpit

import androidx.lifecycle.ViewModel
import com.flightfocus.app.data.TimeProvider
import com.flightfocus.app.domain.economy.Settlement
import com.flightfocus.app.domain.flight.FlightState
import com.flightfocus.app.flight.FlightController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class CockpitViewModel @Inject constructor(
    private val flightController: FlightController,
    private val time: TimeProvider,
) : ViewModel() {

    val state: StateFlow<FlightState> = flightController.state
    val settlement: StateFlow<Settlement?> = flightController.settlement

    fun now(): Long = time.nowMillis()

    fun land() = flightController.land()

    fun acknowledge() = flightController.acknowledge()
}
