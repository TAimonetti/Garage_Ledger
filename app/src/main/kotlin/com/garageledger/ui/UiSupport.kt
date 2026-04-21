package com.garageledger.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.garageledger.domain.model.RecordFamily
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

internal val EditorDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
internal val FilterDateFormatter: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

internal fun parseEditorDateTime(raw: String): LocalDateTime? = runCatching {
    LocalDateTime.parse(raw.trim(), EditorDateFormatter)
}.getOrNull()

internal fun parseFilterDate(raw: String): LocalDate? = runCatching {
    LocalDate.parse(raw.trim(), FilterDateFormatter)
}.getOrNull()

internal fun Double.toStableString(): String = if (this % 1.0 == 0.0) {
    this.toInt().toString()
} else {
    "%,.3f".format(this).replace(",", "")
}

internal fun Double.asCurrency(symbol: String): String = symbol + "%,.2f".format(this)

internal fun Double.asCurrency(): String = asCurrency("$")

internal fun Double.formatOneDecimal(): String = "%,.1f".format(this)

internal enum class TripEndOdometerMode {
    ABSOLUTE,
    DISTANCE_FROM_START,
}

internal fun resolveTripEndOdometer(
    startOdometer: Double?,
    rawInput: String,
    mode: TripEndOdometerMode,
): Double? {
    val parsedValue = rawInput.trim().toDoubleOrNull() ?: return null
    return when (mode) {
        TripEndOdometerMode.ABSOLUTE -> parsedValue
        TripEndOdometerMode.DISTANCE_FROM_START -> startOdometer?.plus(parsedValue)
    }
}

internal fun translateTripEndOdometerInput(
    rawInput: String,
    startOdometer: Double?,
    fromMode: TripEndOdometerMode,
    toMode: TripEndOdometerMode,
): String {
    if (rawInput.isBlank() || fromMode == toMode) return rawInput
    val absoluteValue = resolveTripEndOdometer(startOdometer, rawInput, fromMode) ?: return rawInput
    return when (toMode) {
        TripEndOdometerMode.ABSOLUTE -> absoluteValue.toStableString()
        TripEndOdometerMode.DISTANCE_FROM_START -> {
            val startValue = startOdometer ?: return rawInput
            val distance = absoluteValue - startValue
            if (distance < 0.0) rawInput else distance.toStableString()
        }
    }
}

internal fun buildReturnTripPurpose(previousPurpose: String): String = previousPurpose
    .trim()
    .takeIf(String::isNotBlank)
    ?.let { "Return: $it" }
    .orEmpty()

internal fun RecordFamily.displayLabel(): String = when (this) {
    RecordFamily.FILL_UP -> "Fuel-Ups"
    RecordFamily.SERVICE -> "Services"
    RecordFamily.EXPENSE -> "Expenses"
    RecordFamily.TRIP -> "Trips"
}

@Composable
internal fun ToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SuggestionRow(
    suggestions: List<String>,
    limit: Int = 6,
    onSelect: (String) -> Unit,
) {
    if (suggestions.isEmpty()) return
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        suggestions.take(limit).forEach { suggestion ->
            AssistChip(onClick = { onSelect(suggestion) }, label = { Text(suggestion) })
        }
    }
}

@Composable
internal fun SummaryChip(label: String, value: String) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
            )
            Text(
                value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun MultiSelectTypeChips(
    options: List<Pair<Long, String>>,
    selectedIds: Set<Long>,
    onToggle: (Long) -> Unit,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { (id, label) ->
            FilterChip(
                selected = id in selectedIds,
                onClick = { onToggle(id) },
                label = { Text(label) },
            )
        }
    }
}
