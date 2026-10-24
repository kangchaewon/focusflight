package com.flightfocus.app.ui.hangar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flightfocus.app.domain.economy.UnlockCheck
import com.flightfocus.app.ui.durationText
import com.flightfocus.app.ui.miles

@Composable
fun HangarScreen(viewModel: HangarViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(message) {
        message?.let {
            snackbar.showSnackbar(it)
            viewModel.messageShown()
        }
    }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Column(Modifier.padding(vertical = 8.dp)) {
                    Text("격납고", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Text("보유 ${state.balance.miles()}", color = MaterialTheme.colorScheme.primary)
                }
            }
            items(state.slots, key = { it.tier }) { slot -> SlotCard(slot, onUnlock = { viewModel.unlock(slot.tier) }) }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun SlotCard(slot: HangarSlot, onUnlock: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(slot.tier.displayName, style = MaterialTheme.typography.titleMedium)
                Text(
                    "최대 비행 ${slot.tier.maxMinutes.durationText()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            when (val check = slot.check) {
                UnlockCheck.AlreadyUnlocked -> Text("✅ 보유", color = MaterialTheme.colorScheme.secondary)
                UnlockCheck.Allowed -> Button(onClick = onUnlock) { Text(slot.tier.unlockCost.miles()) }
                is UnlockCheck.NotEnoughMiles -> Button(onClick = onUnlock, enabled = false) { Text(slot.tier.unlockCost.miles()) }
                is UnlockCheck.PreviousTierRequired -> Text("🔒", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
