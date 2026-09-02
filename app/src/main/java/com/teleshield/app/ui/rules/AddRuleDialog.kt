package com.teleshield.app.ui.rules

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.teleshield.application.AddRuleUseCase
import com.teleshield.domain.RuleType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddRuleDialog(
    onDismiss: () -> Unit,
    onAdd: (AddRuleUseCase.AddRuleRequest) -> Unit,
) {
    var pattern by remember { mutableStateOf("") }
    var label by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(RuleType.EXACT) }
    var isWhitelist by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add rule") },
        text = {
            Column {
                OutlinedTextField(value = pattern, onValueChange = { pattern = it }, label = { Text("Pattern") })
                OutlinedTextField(value = label, onValueChange = { label = it }, label = { Text("Label") })
                RuleTypeSelector(selected = type, onSelect = { type = it })
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Whitelist")
                    Spacer(Modifier.width(8.dp))
                    Switch(checked = isWhitelist, onCheckedChange = { isWhitelist = it })
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onAdd(AddRuleUseCase.AddRuleRequest(pattern, type, label, isWhitelist))
            }) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RuleTypeSelector(selected: RuleType, onSelect: (RuleType) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val options = listOf(RuleType.EXACT, RuleType.PREFIX, RuleType.WILDCARD, RuleType.REGEX)

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selected.name,
            onValueChange = {},
            readOnly = true,
            label = { Text("Type") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.menuAnchor(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.name) },
                    onClick = { onSelect(option); expanded = false },
                )
            }
        }
    }
}
