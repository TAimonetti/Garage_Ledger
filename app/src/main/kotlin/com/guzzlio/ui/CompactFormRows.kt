package com.guzzlio.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.LocalDateTime

@Composable
internal fun CompactFormSectionHeader(title: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        HorizontalDivider()
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.sp),
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
internal fun CompactFormRow(
    label: String,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            modifier = Modifier.width(112.dp),
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp, lineHeight = 16.sp),
            fontWeight = FontWeight.SemiBold,
        )
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = content,
        )
    }
}

@Composable
internal fun RowScope.CompactFormFieldButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Tap to enter",
    emphasizeValue: Boolean = text.isNotBlank(),
    alignEnd: Boolean = false,
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f),
        shape = MaterialTheme.shapes.small,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Text(
            text = text.ifBlank { placeholder },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 9.dp),
            style = if (emphasizeValue) {
                MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp, lineHeight = 19.sp)
            } else {
                MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp)
            },
            fontWeight = if (emphasizeValue) FontWeight.SemiBold else FontWeight.Normal,
            textAlign = if (alignEnd) TextAlign.End else TextAlign.Start,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
internal fun CompactFormCheckboxRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    CompactFormRow(label = label) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onCheckedChange(!checked) },
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}

@Composable
internal fun CompactFormReadoutRow(label: String, value: String) {
    CompactFormRow(label = label) {
        Text(
            text = value,
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp, lineHeight = 20.sp),
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
internal fun CompactNumericDialogRow(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    decimalEnabled: Boolean,
    displayLabel: String = label,
    placeholder: String = "Tap to enter",
    valueSuffix: String? = null,
) {
    var showDialog by rememberSaveable(label) { mutableStateOf(false) }
    val displayValue = buildString {
        val trimmed = value.trim()
        if (trimmed.isNotBlank()) {
            append(trimmed)
            if (!valueSuffix.isNullOrBlank()) {
                append(" ")
                append(valueSuffix)
            }
        }
    }
    CompactFormRow(label = displayLabel) {
        CompactFormFieldButton(
            text = displayValue,
            onClick = { showDialog = true },
            modifier = Modifier.weight(1f),
            placeholder = placeholder,
            emphasizeValue = value.isNotBlank(),
            alignEnd = true,
        )
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
internal fun CompactChoiceDialogRow(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    choices: List<String>,
    allowCustomEntry: Boolean = true,
    allowClear: Boolean = true,
    extraActionLabel: String? = null,
    onExtraAction: (() -> Unit)? = null,
    placeholder: String = "Tap to choose",
) {
    var showDialog by rememberSaveable(label) { mutableStateOf(false) }
    CompactFormRow(label = label) {
        CompactFormFieldButton(
            text = value,
            onClick = { showDialog = true },
            modifier = Modifier.weight(1f),
            placeholder = placeholder,
            emphasizeValue = value.isNotBlank(),
        )
    }
    if (showDialog) {
        SingleChoiceDialog(
            title = label,
            currentValue = value,
            choices = choices,
            allowCustomEntry = allowCustomEntry,
            allowClear = allowClear,
            emptyChoicesMessage = "No saved choices yet.",
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
internal fun CompactMultiChoiceSelectionRow(
    label: String,
    selectedIds: Set<Long>,
    options: List<Pair<Long, String>>,
    onSelectionChange: (Set<Long>) -> Unit,
    emptyChoicesMessage: String,
    extraActionLabel: String? = null,
    onExtraAction: (() -> Unit)? = null,
) {
    var showDialog by rememberSaveable(label) { mutableStateOf(false) }
    val selectedLabels = options
        .filter { (id, _) -> id in selectedIds }
        .map { it.second }
        .joinToString(", ")
    CompactFormRow(label = label) {
        CompactFormFieldButton(
            text = selectedLabels,
            onClick = { showDialog = true },
            modifier = Modifier.weight(1f),
            placeholder = "Tap to choose",
            emphasizeValue = selectedIds.isNotEmpty(),
        )
    }
    if (showDialog) {
        MultiChoiceSelectionDialog(
            title = label,
            selectedIds = selectedIds,
            options = options,
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
            onConfirm = {
                onSelectionChange(it)
                showDialog = false
            },
        )
    }
}

@Composable
internal fun CompactTagsRow(
    label: String = "Tags",
    selectedTags: List<String>,
    suggestions: List<String>,
    onValueChange: (List<String>) -> Unit,
) {
    var showDialog by rememberSaveable { mutableStateOf(false) }
    CompactFormRow(label = label) {
        CompactFormFieldButton(
            text = selectedTags.joinToString(", "),
            onClick = { showDialog = true },
            modifier = Modifier.weight(1f),
            placeholder = "Tap to choose",
            emphasizeValue = selectedTags.isNotEmpty(),
        )
    }
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
internal fun CompactDateTimeChooserRow(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
) {
    val context = LocalContext.current
    val seed = parseEditorDateTime(value) ?: LocalDateTime.now()
    fun openDatePicker() {
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                onValueChange(
                    applyPickedDateToEditorDateTime(
                        raw = value,
                        pickedDate = LocalDate.of(year, month + 1, dayOfMonth),
                        fallback = seed,
                    ),
                )
            },
            seed.year,
            seed.monthValue - 1,
            seed.dayOfMonth,
        ).show()
    }
    fun openTimePicker() {
        TimePickerDialog(
            context,
            { _, hour, minute ->
                onValueChange(
                    applyPickedTimeToEditorDateTime(
                        raw = value,
                        hour = hour,
                        minute = minute,
                        fallback = seed,
                    ),
                )
            },
            seed.hour,
            seed.minute,
            true,
        ).show()
    }
    CompactFormRow(label = label) {
        CompactFormFieldButton(
            text = value,
            onClick = ::openDatePicker,
            modifier = Modifier.weight(1f),
            emphasizeValue = value.isNotBlank(),
        )
        IconButton(onClick = ::openDatePicker, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Outlined.CalendarMonth, contentDescription = "Pick date")
        }
        IconButton(onClick = ::openTimePicker, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Outlined.AccessTime, contentDescription = "Pick time")
        }
    }
}
