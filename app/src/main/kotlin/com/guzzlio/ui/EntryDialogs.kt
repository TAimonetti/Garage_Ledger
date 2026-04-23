package com.guzzlio.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
internal fun CompactNumericEntryRow(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    decimalEnabled: Boolean = true,
    placeholder: String = "Tap to enter",
) {
    var showDialog by rememberSaveable { mutableStateOf(false) }
    CompactSelectionRow(
        label = label,
        value = value.ifBlank { placeholder },
        modifier = modifier,
        onTap = { showDialog = true },
        emphasizeValue = value.isNotBlank(),
    )
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
internal fun CompactSingleChoiceDialogRow(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    choices: List<String>,
    modifier: Modifier = Modifier,
    allowCustomEntry: Boolean = true,
    allowClear: Boolean = true,
    emptyChoicesMessage: String = "No saved choices yet.",
    extraActionLabel: String? = null,
    onExtraAction: (() -> Unit)? = null,
    placeholder: String = "Tap to choose",
) {
    var showDialog by rememberSaveable { mutableStateOf(false) }
    CompactSelectionRow(
        label = label,
        value = value.ifBlank { placeholder },
        modifier = modifier,
        onTap = { showDialog = true },
        emphasizeValue = value.isNotBlank(),
    )
    if (showDialog) {
        SingleChoiceDialog(
            title = label,
            currentValue = value,
            choices = choices,
            allowCustomEntry = allowCustomEntry,
            allowClear = allowClear,
            emptyChoicesMessage = emptyChoicesMessage,
            extraActionLabel = extraActionLabel,
            onExtraAction = if (extraActionLabel != null && onExtraAction != null) {
                {
                    showDialog = false
                    onExtraAction()
                }
            } else {
                null
            },
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
internal fun CompactSelectionRow(
    label: String,
    value: String,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
    emphasizeValue: Boolean = false,
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        shape = MaterialTheme.shapes.medium,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onTap)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(0.9f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = value,
                style = if (emphasizeValue) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
                fontWeight = if (emphasizeValue) FontWeight.Bold else FontWeight.Normal,
                textAlign = TextAlign.End,
                modifier = Modifier.weight(1.1f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
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
    extraActionLabel: String? = null,
    onExtraAction: (() -> Unit)? = null,
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
            extraActionLabel = extraActionLabel,
            onExtraAction = if (extraActionLabel != null && onExtraAction != null) {
                {
                    showDialog = false
                    onExtraAction()
                }
            } else {
                null
            },
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
internal fun MultiChoiceSelectionField(
    selectedIds: Set<Long>,
    options: List<Pair<Long, String>>,
    onSelectionChange: (Set<Long>) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    emptyChoicesMessage: String = "No saved choices yet.",
) {
    var showDialog by rememberSaveable { mutableStateOf(false) }
    val selectedLabels = remember(selectedIds, options) {
        options
            .filter { (id, _) -> id in selectedIds }
            .map { it.second }
            .joinToString(", ")
    }
    TapProxyOutlinedField(
        value = selectedLabels,
        modifier = modifier,
        label = label,
        onTap = { showDialog = true },
        minLines = 1,
        trailingContent = { Text("v", style = MaterialTheme.typography.titleMedium) },
    )
    if (showDialog) {
        MultiChoiceSelectionDialog(
            title = label,
            selectedIds = selectedIds,
            options = options,
            emptyChoicesMessage = emptyChoicesMessage,
            onDismiss = { showDialog = false },
            onConfirm = {
                onSelectionChange(it)
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
internal fun NumericKeypadDialog(
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

private enum class DialogActionStyle {
    TEXT,
    OUTLINED,
    FILLED,
}

private data class DialogActionSpec(
    val label: String,
    val style: DialogActionStyle,
    val enabled: Boolean = true,
    val onClick: () -> Unit,
)

@Composable
private fun DialogActionGrid(actions: List<DialogActionSpec>) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        actions.chunked(2).forEach { rowActions ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                rowActions.forEach { action ->
                    when (action.style) {
                        DialogActionStyle.TEXT -> {
                            TextButton(
                                onClick = action.onClick,
                                enabled = action.enabled,
                                modifier = Modifier.weight(1f),
                            ) {
                                Text(
                                    text = action.label,
                                    textAlign = TextAlign.Center,
                                    maxLines = 2,
                                )
                            }
                        }

                        DialogActionStyle.OUTLINED -> {
                            OutlinedButton(
                                onClick = action.onClick,
                                enabled = action.enabled,
                                modifier = Modifier.weight(1f),
                            ) {
                                Text(
                                    text = action.label,
                                    textAlign = TextAlign.Center,
                                    maxLines = 2,
                                )
                            }
                        }

                        DialogActionStyle.FILLED -> {
                            Button(
                                onClick = action.onClick,
                                enabled = action.enabled,
                                modifier = Modifier.weight(1f),
                            ) {
                                Text(
                                    text = action.label,
                                    textAlign = TextAlign.Center,
                                    maxLines = 2,
                                )
                            }
                        }
                    }
                }
                if (rowActions.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
internal fun SingleChoiceDialog(
    title: String,
    currentValue: String,
    choices: List<String>,
    allowCustomEntry: Boolean,
    allowClear: Boolean,
    emptyChoicesMessage: String,
    extraActionLabel: String?,
    onExtraAction: (() -> Unit)?,
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
                val actions = buildList {
                    if (extraActionLabel != null && onExtraAction != null) {
                        add(
                            DialogActionSpec(
                                label = extraActionLabel,
                                style = DialogActionStyle.OUTLINED,
                                onClick = onExtraAction,
                            ),
                        )
                    }
                    if (allowClear && currentValue.isNotBlank()) {
                        add(
                            DialogActionSpec(
                                label = "Clear",
                                style = DialogActionStyle.OUTLINED,
                                onClick = onCleared,
                            ),
                        )
                    }
                    add(
                        DialogActionSpec(
                            label = if (allowCustomEntry) "Cancel" else "Close",
                            style = DialogActionStyle.TEXT,
                            onClick = onDismiss,
                        ),
                    )
                    if (allowCustomEntry) {
                        add(
                            DialogActionSpec(
                                label = "Use",
                                style = DialogActionStyle.FILLED,
                                enabled = customValue.isNotBlank(),
                                onClick = { onSelected(customValue.trim()) },
                            ),
                        )
                    }
                }
                DialogActionGrid(actions)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun MultiChoiceTagDialog(
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
                DialogActionGrid(
                    actions = listOf(
                        DialogActionSpec(
                            label = "Cancel",
                            style = DialogActionStyle.TEXT,
                            onClick = onDismiss,
                        ),
                        DialogActionSpec(
                            label = "Clear",
                            style = DialogActionStyle.OUTLINED,
                            onClick = { workingTags = emptySet() },
                        ),
                        DialogActionSpec(
                            label = "Done",
                            style = DialogActionStyle.FILLED,
                            onClick = { onConfirm(workingTags.sortedBy(String::lowercase)) },
                        ),
                    ),
                )
            }
        }
    }
}

@Composable
internal fun MultiChoiceSelectionDialog(
    title: String,
    selectedIds: Set<Long>,
    options: List<Pair<Long, String>>,
    emptyChoicesMessage: String,
    extraActionLabel: String? = null,
    onExtraAction: (() -> Unit)? = null,
    onDismiss: () -> Unit,
    onConfirm: (Set<Long>) -> Unit,
) {
    var workingSelection by rememberSaveable(selectedIds) { mutableStateOf(selectedIds) }
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
                if (options.isEmpty()) {
                    Text(emptyChoicesMessage)
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 320.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        items(options) { (id, label) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        workingSelection = if (id in workingSelection) {
                                            workingSelection - id
                                        } else {
                                            workingSelection + id
                                        }
                                    }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Checkbox(
                                    checked = id in workingSelection,
                                    onCheckedChange = { checked ->
                                        workingSelection = if (checked) {
                                            workingSelection + id
                                        } else {
                                            workingSelection - id
                                        }
                                    },
                                )
                                Text(label)
                            }
                        }
                    }
                }
                val actions = buildList {
                    if (extraActionLabel != null && onExtraAction != null) {
                        add(
                            DialogActionSpec(
                                label = extraActionLabel,
                                style = DialogActionStyle.OUTLINED,
                                onClick = onExtraAction,
                            ),
                        )
                    }
                    add(
                        DialogActionSpec(
                            label = "Cancel",
                            style = DialogActionStyle.TEXT,
                            onClick = onDismiss,
                        ),
                    )
                    add(
                        DialogActionSpec(
                            label = "Clear",
                            style = DialogActionStyle.OUTLINED,
                            onClick = { workingSelection = emptySet() },
                        ),
                    )
                    add(
                        DialogActionSpec(
                            label = "Done",
                            style = DialogActionStyle.FILLED,
                            onClick = { onConfirm(workingSelection) },
                        ),
                    )
                }
                DialogActionGrid(actions)
            }
        }
    }
}
