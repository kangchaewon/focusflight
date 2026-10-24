package com.flightfocus.app.ui.logbook

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flightfocus.app.data.db.FlightSessionEntity
import com.flightfocus.app.domain.model.FlightMode
import com.flightfocus.app.domain.model.FlightOutcome
import com.flightfocus.app.ui.durationText
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LogbookScreen(viewModel: LogbookViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Column(Modifier.padding(vertical = 8.dp)) {
                Text("비행 일지", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Stat("총 비행", state.totalMinutes.durationText())
                    Stat("총 거리", "${state.totalDistanceKm}km")
                    Stat("비행", "${state.flightCount}회")
                    Stat("회항", "${state.divertCount}회")
                }
            }
        }
        if (state.sessions.isEmpty()) {
            item {
                Text(
                    "아직 비행 기록이 없어요. 첫 비행을 떠나보세요!",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 24.dp),
                )
            }
        }
        items(state.sessions, key = { it.id }) { SessionRow(it) }
    }
}

@Composable
private fun Stat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private val dateFormat = SimpleDateFormat("M/d HH:mm", Locale.KOREA)

@Composable
private fun SessionRow(session: FlightSessionEntity) {
    val outcome = when (session.outcome) {
        FlightOutcome.COMPLETED -> if (session.mode == FlightMode.SCHEDULED) "정시 도착" else "착륙"
        FlightOutcome.LANDED_EARLY -> "조기 착륙"
        FlightOutcome.DIVERTED -> "회항"
    }
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    session.destinationIata?.let { "${session.originIata} → $it" } ?: "${session.originIata} 부정기편",
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    "${dateFormat.format(Date(session.startedAt))} · ${session.flownMinutes.durationText()} · $outcome",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text("+${session.miles}", color = MaterialTheme.colorScheme.primary)
        }
    }
}
