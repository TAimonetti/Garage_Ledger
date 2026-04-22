package com.garageledger.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.unit.dp

@Composable
internal fun NumericEntryField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    decimalEnabled: Boolean = true,
    supportingContent: @Composable (() -> Unit)? = null,
) {
    var showDialog by rememberSaveable { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        TapProxyOutlinedField(
            value = value,
            modifier = modifier,
            label = label,
            singleLine = true,
            onTap = { showDialog = true },
            trailingContent = {
                Text(
                    "123",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                )
            },
        )
        supportingContent?.invoke()
    }
    if (showDialog) {
        NumericKeypadDialog(
            title = label,
            initialValue = value,
            decimalEnabled = decimalEnabled,
            onDismiss = { showDialog = false },
            onConfirm = {
                onValueChange(it)
                showDialog = false
            },
        )
    }
}

@Composable
internal fun SingleChoiceDialogField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    choices: List<String>,
    modifier: Modifier = Modifier,
    allowCustomEntry: Boolean = true,
    allowClear: Boolean = true,
    emptyChoicesMessage: String = "No saved choices yet.",
) {
    var showDialog by rememberSaveable { mutableStateOf(false) }
    TapProxyOutlinedField(
        value = value,
        modifier = modifier,
        label = label,
        singleLine = true,
        onTap = { showDialog = true },
        trailingContent = { Text("v", style = MaterialTheme.typography.titleMedium) },
    )
    if (showDialog) {
        SingleChoiceDialog(
            title = label,
            currentValue = value,
            choices = choices,
            allowCustomEntry = allowCustomEntry,
            allowClear = allowClear,
            emptyChoicesMessage = emptyChoicesMessage,
            onDismiss = { showDialog = false },
            onSelected = {
                onValueChange(it)
                showDialog = false
            },
            onCleared = {
                onValueChange("")
                showDialog = false
            },
        )
    }
}

@Composable
internal fun MultiChoiceTagDialogField(
    selectedTags: List<String>,
    suggestions: List<String>,
    onValueChange: (List<String>) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
) {
    var showDialog by rememberSaveable { mutableStateOf(false) }
    TapProxyOutlinedField(
        value = selectedTags.joinToString(", "),
        modifier = modifier,
        label = label,
        onTap = { showDialog = true },
        minLines = 1,
        trailingContent = { Text("v", style = MaterialTheme.typography.titleMedium) },
    )
    if (showDialog) {
        MultiChoiceTagDialog(
            title = label,
            selectedTags = selectedTags,
            suggestions = suggestions,
            onDismiss = { showDialog = false },
            onConfirm = {
                onValueChange(it)
                showDialog = false
            },
        )
    }
}

@Composable
private fun TapProxyOutlinedField(
    value: String,
    label: String,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
    singleLine: Boolean = false,
    minLines: Int = 1,
    trailingContent: @Composable (() -> Unit)? = null,
) {
    Box(
        modifier = modifier.clickable { onTap() },
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            modifier = Modifier.fillMaxWidth(),
            label = { Text(label) },
            enabled = false,
            singleLine = singleLine,
            minLines = minLines,
            trailingIcon = trailingContent,
            colors = OutlinedTextFieldDefaults.colors(
                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                disabledBorderColor = MaterialTheme.colorScheme.outline,
                disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                disabledContainerColor = MaterialTheme.colorScheme.surface,
            ),
        )
    }
}

@Composable
private fun NumericKeypadDialog(
    title: String,
    initialValue: String,
    decimalEnabled: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var value by rememberSaveable(initialValue) { mutableStateOf(initialValue) }
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 6.dp,
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.large,
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 18.dp),
                    ) {
                        Text(
                            text = value.ifBlank { "0" },
                            style = MaterialTheme.typography.headlineMedium,
                            modifier = Modifier.align(Alignment.CenterEnd),
                        )
                    }
                }
                NumericKeypadRow(
                    labels = listOf("7", "8", "9"),
                    onPress = { value = appendNumericToken(value, it, decimalEnabled) },
                )
                NumericKeypadRow(
                    labels = listOf("4", "5", "6"),
                    onPress = { value = appendNumericToken(value, it, decimalEnabled) },
                )
                NumericKeypadRow(
                    labels = listOf("1", "2", "3"),
                    onPress = { value = appendNumericToken(value, it, decimalEnabled) },
                )
                NumericKeypadRow(
                    labels = listOf(if (decimalEnabled) "." else "C", "0", "<-"),
                    onPress = { token ->
                        value = when (token) {
                            "C" -> ""
                            "<-" -> value.dropLast(1)
                            else -> appendNumericToken(value, token, decimalEnabled)
                        }
                    },
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedButton(
                        onClick = { value = "" },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Clear")
                    }
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = { onConfirm(value) },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Done")
                    }
                }
            }
        }
    }
}

@Composable
private fun NumericKeypadRow(
    labels: List<String>,
    onPress: (String) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        labels.forEach { label ->
            Button(
                onClick = { onPress(label) },
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(vertical = 18.dp),
            ) {
                Text(label, style = MaterialTheme.typography.titleLarge)
            }
        }
    }
}

private fun appendNumericToken(
    current: String,
    token: String,
    decimalEnabled: Boolean,
): String {
    if (token == "." && (!decimalEnabled || current.contains('.'))) return current
    if (token == "." && current.isBlank()) return "0."
    if (current == "0" && token != ".") return token
    return current + token
}

@Composable
private fun SingleChoiceDialog(
    title: String,
    currentValue: String,
    choices: List<String>,
    allowCustomEntry: Boolean,
    allowClear: Boolean,
    emptyChoicesMessage: String,
    onDismiss: () -> Unit,
    onSelected: (String) -> Unit,
    onCleared: () -> Unit,
) {
    var customValue by rememberSaveable(currentValue) { mutableStateOf("") }
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 6.dp,
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                if (choices.isEmpty()) {
                    Text(emptyChoicesMessage, style = MaterialTheme.typography.bodyMedium)
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 320.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        items(choices) { choice ->
                            Surface(
                                color = if (choice.equals(currentValue, ignoreCase = true)) {
                                    MaterialTheme.colorScheme.secondaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                },
                                shape = MaterialTheme.shapes.large,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelected(choice) },
                            ) {
                                Text(
                                    text = choice,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                    fontWeight = if (choice.equals(currentValue, ignoreCase = true)) FontWeight.Bold else FontWeight.Normal,
                                )
                            }
                        }
                    }
                }
                if (allowCustomEntry) {
                    OutlinedTextField(
                        value = customValue,
                        onValueChange = { customValue = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Add New") },
                        singleLine = true,
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (allowClear && currentValue.isNotBlank()) {
                        TextButton(onClick = onCleared, modifier = Modifier.weight(1f)) {
                            Text("Clear")
                        }
                    }
                    TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text(if (allowCustomEntry) "Cancel" else "Close")
                    }
                    if (allowCustomEntry) {
                        Button(
                            onClick = { onSelected(customValue.trim()) },
                            modifier = Modifier.weight(1f),
                            enabled = customValue.isNotBlank(),
                        ) {
                            Text("Use")
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MultiChoiceTagDialog(
    title: String,
    selectedTags: List<String>,
    suggestions: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (List<String>) -> Unit,
) {
    val initialTags = remember(selectedTags) { selectedTags.map(String::trim).filter(String::isNotBlank).toSet() }
    var workingTags by rememberSaveable(selectedTags) { mutableStateOf(initialTags) }
    var customTag by rememberSaveable { mutableStateOf("") }
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 6.dp,
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                if (suggestions.isEmpty()) {
                    Text("No saved tags yet. Add one below to start building your list.")
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 280.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        items(suggestions) { tag ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        workingTags = if (tag in workingTags) {
                                            workingTags - tag
                                        } else {
                                            workingTags + tag
                                        }
                                    }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Checkbox(
                                    checked = tag in workingTags,
                                    onCheckedChange = { checked ->
                                        workingTags = if (checked) workingTags + tag else workingTags - tag
                                    },
                                )
                                Text(tag)
                            }
                        }
                    }
                }
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    workingTags.sortedBy(String::lowercase).forEach { tag ->
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = MaterialTheme.shapes.large,
                        ) {
                            Text(
                                tag,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            )
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        value = customTag,
                        onValueChange = { customTag = it },
                        modifier = Modifier.weight(1f),
                        label = { Text("Add Tag") },
                        singleLine = true,
                    )
                    Button(
                        onClick = {
                            val trimmed = customTag.trim()
                            if (trimmed.isNotBlank()) {
                                workingTags = workingTags + trimmed
                                customTag = ""
                            }
                        },
                        enabled = customTag.isNotBlank(),
                    ) {
                        Text("Add")
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("Cancel")
                    }
                    OutlinedButton(
                        onClick = { workingTags = emptySet() },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Clear")
                    }
                    Button(
                        onClick = { onConfirm(workingTags.sortedBy(String::lowercase)) },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Done")
                    }
                }
            }
        }
    }
}
