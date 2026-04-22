package com.garageledger.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilterChip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.garageledger.domain.model.AppPreferenceSnapshot
import com.garageledger.domain.model.RecordFamily
import com.garageledger.domain.model.TripRecord
import com.garageledger.domain.model.VehicleLifecycle
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

internal val EditorDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
internal val FilterDateFormatter: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE
private const val DefaultFullDatePattern = "MMM dd, yyyy"
private const val DefaultCompactDatePattern = "MM/dd/yy"

internal fun parseEditorDateTime(raw: String): LocalDateTime? = runCatching {
    LocalDateTime.parse(raw.trim(), EditorDateFormatter)
}.getOrNull()

internal fun parseFilterDate(raw: String): LocalDate? = runCatching {
    LocalDate.parse(raw.trim(), FilterDateFormatter)
}.getOrNull()

internal fun AppPreferenceSnapshot.displayDateFormatter(compact: Boolean = false): DateTimeFormatter {
    val preferredPattern = if (compact) compactDateFormat else fullDateFormat
    val fallbackPattern = if (compact) DefaultCompactDatePattern else DefaultFullDatePattern
    return preferredFormatter(preferredPattern, fallbackPattern)
}

internal fun AppPreferenceSnapshot.displayDateTimeFormatter(): DateTimeFormatter =
    preferredFormatter("$fullDateFormat HH:mm", "$DefaultFullDatePattern HH:mm")

internal fun LocalDate.formatForDisplay(
    preferences: AppPreferenceSnapshot,
    compact: Boolean = false,
): String = format(preferences.displayDateFormatter(compact))

internal fun LocalDateTime.formatForDisplay(preferences: AppPreferenceSnapshot): String =
    format(preferences.displayDateTimeFormatter())

internal fun String.toDisplayDateOrNull(
    preferences: AppPreferenceSnapshot,
    compact: Boolean = false,
): String? = parseFilterDate(this)?.formatForDisplay(preferences, compact)

private fun AppPreferenceSnapshot.preferredFormatter(
    preferredPattern: String,
    fallbackPattern: String,
): DateTimeFormatter = runCatching {
    DateTimeFormatter.ofPattern(preferredPattern.ifBlank { fallbackPattern }, resolvedLocale())
}.getOrElse {
    DateTimeFormatter.ofPattern(fallbackPattern, resolvedLocale())
}

private fun AppPreferenceSnapshot.resolvedLocale(): Locale {
    val tag = localeTag.trim()
    if (tag.isBlank() || tag.equals("system", ignoreCase = true)) return Locale.getDefault()
    val locale = Locale.forLanguageTag(tag)
    return if (locale.language.isNullOrBlank()) Locale.getDefault() else locale
}

internal fun applyPickedDateToEditorDateTime(
    raw: String,
    pickedDate: LocalDate,
    fallback: LocalDateTime = LocalDateTime.now(),
): String = (parseEditorDateTime(raw) ?: fallback)
    .withYear(pickedDate.year)
    .withMonth(pickedDate.monthValue)
    .withDayOfMonth(pickedDate.dayOfMonth)
    .format(EditorDateFormatter)

internal fun applyPickedTimeToEditorDateTime(
    raw: String,
    hour: Int,
    minute: Int,
    fallback: LocalDateTime = LocalDateTime.now(),
): String = (parseEditorDateTime(raw) ?: fallback)
    .withHour(hour)
    .withMinute(minute)
    .format(EditorDateFormatter)

internal fun coerceDateText(pickedDate: LocalDate): String = pickedDate.format(FilterDateFormatter)

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

internal data class TripCopySeed(
    val startDateText: String,
    val startLocation: String,
    val startLatitude: Double?,
    val startLongitude: Double?,
    val endLocation: String,
    val endLatitude: Double?,
    val endLongitude: Double?,
    val purpose: String,
    val client: String,
    val taxRateText: String,
    val reimbursementRateText: String,
    val tagsText: String,
    val notesText: String,
    val tripTypeId: Long?,
)

internal fun buildTripCopySeed(
    source: TripRecord,
    now: LocalDateTime,
): TripCopySeed = TripCopySeed(
    startDateText = now.format(EditorDateFormatter),
    startLocation = source.startLocation,
    startLatitude = source.startLatitude,
    startLongitude = source.startLongitude,
    endLocation = source.endLocation,
    endLatitude = source.endLatitude,
    endLongitude = source.endLongitude,
    purpose = source.purpose,
    client = source.client,
    taxRateText = source.taxDeductionRate?.toString().orEmpty(),
    reimbursementRateText = source.reimbursementRate?.toString().orEmpty(),
    tagsText = source.tags.joinToString(", "),
    notesText = source.notes,
    tripTypeId = source.tripTypeId,
)

internal fun VehicleLifecycle.allowsRecordModification(): Boolean = this == VehicleLifecycle.ACTIVE

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
internal fun EditableSuggestionField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    suggestions: List<String>,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    minLines: Int = 1,
    onSuggestionSelected: (String) -> Unit = onValueChange,
) {
    var expanded by remember { mutableStateOf(false) }
    val filteredSuggestions = remember(value, suggestions) {
        val query = value.trim()
        suggestions
            .filter { query.isBlank() || it.contains(query, ignoreCase = true) }
            .distinct()
    }
    Box(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = {
                onValueChange(it)
                expanded = suggestions.isNotEmpty()
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(label) },
            singleLine = singleLine,
            minLines = minLines,
            trailingIcon = {
                if (suggestions.isNotEmpty()) {
                    IconButton(onClick = { expanded = !expanded }) {
                        Text(if (expanded) "▲" else "▼")
                    }
                }
            },
        )
        DropdownMenu(
            expanded = expanded && filteredSuggestions.isNotEmpty(),
            onDismissRequest = { expanded = false },
        ) {
            filteredSuggestions.take(12).forEach { suggestion ->
                DropdownMenuItem(
                    text = { Text(suggestion) },
                    onClick = {
                        onSuggestionSelected(suggestion)
                        expanded = false
                    },
                )
            }
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

@Composable
internal fun PickerDateTimeField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        label = { Text(label) },
        singleLine = true,
        trailingIcon = {
            Row {
                IconButton(
                    onClick = {
                        val seed = parseEditorDateTime(value) ?: LocalDateTime.now()
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
                    },
                ) {
                    Icon(Icons.Outlined.CalendarMonth, contentDescription = "Pick date")
                }
                IconButton(
                    onClick = {
                        val seed = parseEditorDateTime(value) ?: LocalDateTime.now()
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
                    },
                ) {
                    Icon(Icons.Outlined.AccessTime, contentDescription = "Pick time")
                }
            }
        },
    )
}

@Composable
internal fun PickerDateField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        label = { Text(label) },
        singleLine = true,
        trailingIcon = {
            IconButton(
                onClick = {
                    val seed = parseFilterDate(value) ?: LocalDate.now()
                    DatePickerDialog(
                        context,
                        { _, year, month, dayOfMonth ->
                            onValueChange(coerceDateText(LocalDate.of(year, month + 1, dayOfMonth)))
                        },
                        seed.year,
                        seed.monthValue - 1,
                        seed.dayOfMonth,
                    ).show()
                },
            ) {
                Icon(Icons.Outlined.CalendarMonth, contentDescription = "Pick date")
            }
        },
    )
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

internal fun parseCommaValues(raw: String): List<String> = raw
    .split(",")
    .map(String::trim)
    .filter(String::isNotBlank)
    .distinctBy(String::lowercase)
