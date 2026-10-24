package com.flightfocus.app.ui.cockpit

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flightfocus.app.domain.economy.Settlement
import com.flightfocus.app.domain.flight.FlightState
import com.flightfocus.app.domain.model.ActiveFlight
import com.flightfocus.app.domain.model.FlightMode
import com.flightfocus.app.domain.model.FlightOutcome
import com.flightfocus.app.domain.model.FlightPlan
import com.flightfocus.app.domain.model.FlightResult
import com.flightfocus.app.ui.clockText
import com.flightfocus.app.ui.durationText
import com.flightfocus.app.ui.miles
import kotlinx.coroutines.delay

@Composable
fun CockpitScreen(
    onFinished: () -> Unit,
    viewModel: CockpitViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val settlement by viewModel.settlement.collectAsStateWithLifecycle()

    // 비행 중에는 뒤로 가기로 빠져나갈 수 없다 (착륙 버튼으로만 종료)
    BackHandler(enabled = state is FlightState.Airborne) {}

    when (val s = state) {
        FlightState.Idle -> LaunchedEffect(Unit) { onFinished() }
        is FlightState.Airborne -> InFlight(s.flight, now = viewModel::now, onLand = viewModel::land)
        is FlightState.Arrived -> Arrival(s.result, settlement) {
            viewModel.acknowledge()
            onFinished()
        }
    }
}

@Composable
private fun InFlight(flight: ActiveFlight, now: () -> Long, onLand: () -> Unit) {
    val plan = flight.plan
    val currentMillis by produceState(now()) {
        while (true) {
            value = now()
            delay(250)
        }
    }
    val flown = flight.flownMillis(currentMillis)
    val limit = plan.limitMinutes * 60_000L
    var confirmLanding by remember { mutableStateOf(false) }

    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(plan.routeText(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(
            "${if (plan.mode == FlightMode.SCHEDULED) "정기편" else "부정기편"} · ${plan.aircraft.displayName}",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(48.dp))

        Text(
            if (plan.mode == FlightMode.SCHEDULED) "도착까지" else "비행 시간",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            (if (plan.mode == FlightMode.SCHEDULED) limit - flown else flown).clockText(),
            fontSize = 64.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(24.dp))
        LinearProgressIndicator(
            progress = { (flown.toFloat() / limit).coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth(),
        )
        if (plan.mode == FlightMode.CHARTER) {
            Text(
                "연료 한계 ${plan.aircraft.maxMinutes.durationText()}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
        }

        if (flight.inTurbulence) {
            Spacer(Modifier.height(24.dp))
            Text("⚠️ 난기류 발생! 바로 돌아오지 않으면 회항합니다", color = MaterialTheme.colorScheme.error)
        }

        Spacer(Modifier.height(48.dp))
        OutlinedButton(onClick = {
            if (plan.mode == FlightMode.SCHEDULED) confirmLanding = true else onLand()
        }) {
            Text("🛬 착륙하기")
        }
    }

    if (confirmLanding) {
        AlertDialog(
            onDismissRequest = { confirmLanding = false },
            title = { Text("지금 착륙할까요?") },
            text = { Text("목표 시간 전에 착륙하면 정시 운항 보너스(+20%)를 받지 못하고, 목적지가 아닌 출발 공항으로 돌아갑니다.") },
            confirmButton = {
                TextButton(onClick = { confirmLanding = false; onLand() }) { Text("착륙") }
            },
            dismissButton = {
                TextButton(onClick = { confirmLanding = false }) { Text("계속 비행") }
            },
        )
    }
}

@Composable
private fun Arrival(result: FlightResult, settlement: Settlement?, onDone: () -> Unit) {
    val (title, subtitle) = when (result.outcome) {
        FlightOutcome.COMPLETED ->
            if (result.plan.mode == FlightMode.SCHEDULED) "✅ 정시 도착" to "${result.plan.destination?.nameKo}에 도착했습니다"
            else "🛬 착륙 완료" to "부정기편 비행을 마쳤습니다"
        FlightOutcome.LANDED_EARLY -> "🛬 조기 착륙" to "${result.plan.origin.nameKo}(으)로 돌아왔습니다"
        FlightOutcome.DIVERTED -> "🌪️ 비상 회항" to "난기류로 ${result.plan.origin.nameKo}(으)로 회항했습니다"
    }

    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(32.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(result.plan.routeText(), style = MaterialTheme.typography.titleMedium)
                Text("비행 시간 ${result.flownMinutes.durationText()}")
                if (settlement == null) {
                    Text("정산 중…", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    if (settlement.creditedMinutes < result.flownMinutes) {
                        Text("일일 상한으로 ${settlement.creditedMinutes.durationText()}만 인정", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text("기본 +${settlement.baseMiles.miles()}")
                    if (settlement.onTimeBonusMiles > 0) Text("정시 운항 보너스 +${settlement.onTimeBonusMiles.miles()}")
                    if (settlement.divertPenaltyMiles > 0) Text("회항 차감 -${settlement.divertPenaltyMiles.miles()}", color = MaterialTheme.colorScheme.error)
                    Text(
                        "합계 +${settlement.totalMiles.miles()}",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }

        Spacer(Modifier.height(32.dp))
        Button(onClick = onDone, enabled = settlement != null, modifier = Modifier.fillMaxWidth().height(52.dp)) {
            Text("확인")
        }
    }
}

private fun FlightPlan.routeText(): String =
    destination?.let { "${origin.iata} → ${it.iata}" } ?: "${origin.iata} 출발"
