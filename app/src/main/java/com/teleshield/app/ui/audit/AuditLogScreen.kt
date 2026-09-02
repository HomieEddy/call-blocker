package com.teleshield.app.ui.audit

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.teleshield.domain.BlockedCallRecord

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuditLogScreen(viewModel: AuditLogViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Audit Log") },
                actions = {
                    IconButton(onClick = { viewModel.purge() }) {
                        Icon(Icons.Default.Delete, contentDescription = "Purge")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(Modifier.padding(padding)) {
            items(uiState.records, key = { it.id }) { record ->
                LogRow(record)
            }
        }
    }
}

@Composable
private fun LogRow(record: BlockedCallRecord) {
    ListItem(
        headlineContent = { Text(record.callerIdentifier) },
        supportingContent = { Text("${record.matchedLabelSnapshot} · ${record.timestamp}") },
    )
}
