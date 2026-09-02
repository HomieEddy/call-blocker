package com.teleshield.app.ui.rules

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.teleshield.domain.ScreeningRule

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RulesScreen(viewModel: RulesViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var editingRule by remember { mutableStateOf<ScreeningRule?>(null) }
    var showDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Rules") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { editingRule = null; showDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add rule")
            }
        },
    ) { padding ->
        LazyColumn(Modifier.padding(padding)) {
            items(uiState.rules, key = { it.id }) { rule ->
                RuleRow(
                    rule = rule,
                    onClick = { editingRule = rule; showDialog = true },
                    onDelete = { viewModel.deleteRule(rule.id) },
                )
            }
        }
    }

    if (showDialog) {
        AddRuleDialog(
            onDismiss = { showDialog = false },
            onAdd = { request ->
                viewModel.addRule(request)
                showDialog = false
            },
            initialRule = editingRule,
        )
    }
}

@Composable
private fun RuleRow(rule: ScreeningRule, onClick: () -> Unit, onDelete: () -> Unit) {
    ListItem(
        headlineContent = { Text(rule.label.ifBlank { rule.pattern.expression }) },
        supportingContent = { Text("${rule.ruleType.name} · ${rule.pattern.expression}") },
        modifier = Modifier.clickable(onClick = onClick),
        trailingContent = {
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete")
            }
        },
    )
}
