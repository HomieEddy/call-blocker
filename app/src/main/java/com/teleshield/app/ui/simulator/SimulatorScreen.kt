package com.teleshield.app.ui.simulator

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.teleshield.domain.ScreeningVerdict

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimulatorScreen(viewModel: SimulatorViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var callerId by remember { mutableStateOf("") }
    var isPrivate by remember { mutableStateOf(false) }

    Scaffold(topBar = { TopAppBar(title = { Text("Simulator") }) }) { padding ->
        Column(
            Modifier.padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = callerId,
                    onValueChange = { callerId = it },
                    label = { Text("Caller ID") },
                    enabled = !isPrivate,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                Text("Private")
                Switch(checked = isPrivate, onCheckedChange = { isPrivate = it })
            }

            Button(
                onClick = { viewModel.simulate(if (isPrivate) "Private" else callerId) },
                enabled = isPrivate || callerId.isNotBlank(),
            ) { Text("Simulate") }

            val result = uiState.result
            if (result != null) {
                Text(verdictText(result.verdict), style = MaterialTheme.typography.bodyLarge)
                Text("Duration: ${result.executionDurationMs} ms")
            }
        }
    }
}

private fun verdictText(verdict: ScreeningVerdict): String = when (verdict) {
    is ScreeningVerdict.Blocked ->
        "BLOCKED: ${verdict.matchedRule.label.ifBlank { verdict.matchedRule.pattern.expression }}"
    is ScreeningVerdict.Whitelisted -> "ALLOWED (whitelisted)"
    is ScreeningVerdict.Allowed -> "ALLOWED: ${verdict.reason}"
}
