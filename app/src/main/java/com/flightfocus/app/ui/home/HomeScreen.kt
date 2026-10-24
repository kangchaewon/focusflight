package com.flightfocus.app.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flightfocus.app.ui.durationText
import com.flightfocus.app.ui.miles

@Composable
fun HomeScreen(
    onTakeOff: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize()) {
        Header(state, onCharter = { if (viewModel.takeOffCharter()) onTakeOff() })

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(state.routes, key = { it.destination.iata }) { route ->
                RouteRow(route, selected = route == state.selected, onClick = { viewModel.select(route) })
            }
        }

        state.selected?.let { route ->
            Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                Button(
                    onClick = { if (viewModel.takeOffScheduled()) onTakeOff() },
                    modifier = Modifier.fillMaxWidth().padding(16.dp).height(56.dp),
                ) {
                    Text(
                        "🛫  ${route.destination.nameKo}행 이륙 · ${route.minutes.durationText()}",
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }
        }
    }
}

@Composable
private fun Header(state: HomeUiState, onCharter: () -> Unit) {
    Column(Modifier.padding(horizontal = 16.dp, vertical = 20.dp)) {
        Text("현재 위치", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            state.origin?.let { "${it.nameKo} (${it.iata})" } ?: "",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(4.dp))
        Text(state.balance.miles(), color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(16.dp))
        OutlinedButton(onClick = onCharter, modifier = Modifier.fillMaxWidth()) {
            Text("⏱️  부정기편(초시계)으로 이륙 · ${state.charterAircraft.displayName} 최대 ${state.charterAircraft.maxMinutes.durationText()}")
        }
        Spacer(Modifier.height(16.dp))
        Text("정기편 노선 선택", style = MaterialTheme.typography.titleSmall)
    }
}

@Composable
private fun RouteRow(route: RouteOption, selected: Boolean, onClick: () -> Unit) {
    val locked = route.aircraft == null
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surfaceContainer,
        ),
        modifier = Modifier.fillMaxWidth().clickable(enabled = !locked, onClick = onClick),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    "${route.destination.nameKo} ${route.destination.iata}",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (locked) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    "${route.destination.countryKo} · ${route.distanceKm}km · ${route.aircraft?.displayName ?: "🔒 더 큰 기종 필요"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                route.minutes.durationText(),
                style = MaterialTheme.typography.titleMedium,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
