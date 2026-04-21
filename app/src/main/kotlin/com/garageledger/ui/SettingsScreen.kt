package com.garageledger.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.garageledger.core.model.DistanceUnit
import com.garageledger.core.model.FuelEfficiencyAssignmentMethod
import com.garageledger.core.model.FuelEfficiencyUnit
import com.garageledger.core.model.VolumeUnit
import com.garageledger.data.GarageRepository
import com.garageledger.domain.model.AppPreferenceSnapshot
import com.garageledger.domain.model.OptionalFieldToggle
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    repository: GarageRepository,
    onBack: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val preferences by repository.preferences.collectAsStateWithLifecycle(initialValue = AppPreferenceSnapshot())

    var initialized by rememberSaveable { mutableStateOf(false) }
    var distanceUnit by rememberSaveable { mutableStateOf(preferences.distanceUnit) }
    var volumeUnit by rememberSaveable { mutableStateOf(preferences.volumeUnit) }
    var fuelEfficiencyUnit by rememberSaveable { mutableStateOf(preferences.fuelEfficiencyUnit) }
    var assignmentMethod by rememberSaveable { mutableStateOf(preferences.fuelEfficiencyAssignmentMethod) }
    var browseSortDescending by rememberSaveable { mutableStateOf(preferences.browseSortDescending) }
    var useLocation by rememberSaveable { mutableStateOf(preferences.useLocation) }
    var notificationsEnabled by rememberSaveable { mutableStateOf(preferences.notificationsEnabled) }
    var notificationLedEnabled by rememberSaveable { mutableStateOf(preferences.notificationLedEnabled) }
    var currencySymbol by rememberSaveable { mutableStateOf(preferences.currencySymbol) }
    var localeTag by rememberSaveable { mutableStateOf(preferences.localeTag) }
    var fullDateFormat by rememberSaveable { mutableStateOf(preferences.fullDateFormat) }
    var compactDateFormat by rememberSaveable { mutableStateOf(preferences.compactDateFormat) }
    var reminderTimePercentText by rememberSaveable { mutableStateOf(preferences.reminderTimeAlertPercent.toString()) }
    var reminderDistancePercentText by rememberSaveable { mutableStateOf(preferences.reminderDistanceAlertPercent.toString()) }
    var backupFrequencyHoursText by rememberSaveable { mutableStateOf(preferences.backupFrequencyHours.toString()) }
    var backupHistoryCountText by rememberSaveable { mutableStateOf(preferences.backupHistoryCount.toString()) }
    var visibleFields by rememberSaveable { mutableStateOf(preferences.visibleFields) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(preferences) {
        if (initialized) return@LaunchedEffect
        distanceUnit = preferences.distanceUnit
        volumeUnit = preferences.volumeUnit
        fuelEfficiencyUnit = preferences.fuelEfficiencyUnit
        assignmentMethod = preferences.fuelEfficiencyAssignmentMethod
        browseSortDescending = preferences.browseSortDescending
        useLocation = preferences.useLocation
        notificationsEnabled = preferences.notificationsEnabled
        notificationLedEnabled = preferences.notificationLedEnabled
        currencySymbol = preferences.currencySymbol
        localeTag = preferences.localeTag
        fullDateFormat = preferences.fullDateFormat
        compactDateFormat = preferences.compactDateFormat
        reminderTimePercentText = preferences.reminderTimeAlertPercent.toString()
        reminderDistancePercentText = preferences.reminderDistanceAlertPercent.toString()
        backupFrequencyHoursText = preferences.backupFrequencyHours.toString()
        backupHistoryCountText = preferences.backupHistoryCount.toString()
        visibleFields = preferences.visibleFields
        initialized = true
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Settings") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Card {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Units, Language & Formats", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        SettingsEnumSection("Distance Unit", DistanceUnit.entries, distanceUnit, { distanceUnit = it }) { it.storageValue }
                        SettingsEnumSection("Volume Unit", VolumeUnit.entries, volumeUnit, { volumeUnit = it }) { it.storageValue }
                        SettingsEnumSection("Fuel Efficiency", FuelEfficiencyUnit.entries, fuelEfficiencyUnit, { fuelEfficiencyUnit = it }) { it.storageValue }
                        OutlinedTextField(
                            value = currencySymbol,
                            onValueChange = { currencySymbol = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Currency Symbol") },
                            singleLine = true,
                        )
                        OutlinedTextField(
                            value = localeTag,
                            onValueChange = { localeTag = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Locale Tag") },
                            singleLine = true,
                        )
                        OutlinedTextField(
                            value = fullDateFormat,
                            onValueChange = { fullDateFormat = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Full Date Format") },
                            singleLine = true,
                        )
                        OutlinedTextField(
                            value = compactDateFormat,
                            onValueChange = { compactDateFormat = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Compact Date Format") },
                            singleLine = true,
                        )
                    }
                }
            }
            item {
                Card {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("General", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        SettingsEnumSection(
                            title = "Fuel Efficiency Assignment",
                            options = FuelEfficiencyAssignmentMethod.entries,
                            selected = assignmentMethod,
                            onSelect = { assignmentMethod = it },
                        ) { option ->
                            when (option) {
                                FuelEfficiencyAssignmentMethod.PREVIOUS_RECORD -> "Previous Record"
                                FuelEfficiencyAssignmentMethod.CURRENT_RECORD -> "Current Record"
                            }
                        }
                        ToggleRow("Browse Sort Descending", browseSortDescending) { browseSortDescending = it }
                        ToggleRow("Use Location", useLocation) { useLocation = it }
                    }
                }
            }
            item {
                Card {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Backup & Notifications", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        OutlinedTextField(
                            value = backupFrequencyHoursText,
                            onValueChange = { backupFrequencyHoursText = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Backup Frequency (hours)") },
                            singleLine = true,
                        )
                        OutlinedTextField(
                            value = backupHistoryCountText,
                            onValueChange = { backupHistoryCountText = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Backup History Count") },
                            singleLine = true,
                        )
                        OutlinedTextField(
                            value = reminderTimePercentText,
                            onValueChange = { reminderTimePercentText = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Reminder Time Alert Threshold (%)") },
                            singleLine = true,
                        )
                        OutlinedTextField(
                            value = reminderDistancePercentText,
                            onValueChange = { reminderDistancePercentText = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Reminder Distance Alert Threshold (%)") },
                            singleLine = true,
                        )
                        ToggleRow("Status Bar Notifications", notificationsEnabled) { notificationsEnabled = it }
                        ToggleRow("Flash / LED Notification Hint", notificationLedEnabled) { notificationLedEnabled = it }
                    }
                }
            }
            item {
                Card {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Optional Fields", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text("These toggles seed the same optional field visibility used by the record editors.")
                        OptionalFieldToggle.entries.forEach { toggle ->
                            ToggleRow(
                                label = toggle.displayLabel(),
                                checked = toggle in visibleFields,
                                onCheckedChange = { checked ->
                                    visibleFields = visibleFields.toMutableSet().also { set ->
                                        if (checked) set.add(toggle) else set.remove(toggle)
                                    }
                                },
                            )
                        }
                    }
                }
            }
            errorMessage?.let { message ->
                item {
                    Card(colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                        Text(message, modifier = Modifier.padding(18.dp))
                    }
                }
            }
            successMessage?.let { message ->
                item {
                    Card(colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                        Text(message, modifier = Modifier.padding(18.dp))
                    }
                }
            }
            item {
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        scope.launch {
                            runCatching {
                                repository.updatePreferences { current ->
                                    current.copy(
                                        distanceUnit = distanceUnit,
                                        volumeUnit = volumeUnit,
                                        fuelEfficiencyUnit = fuelEfficiencyUnit,
                                        currencySymbol = currencySymbol.ifBlank { current.currencySymbol },
                                        localeTag = localeTag.ifBlank { "system" },
                                        fullDateFormat = fullDateFormat.ifBlank { current.fullDateFormat },
                                        compactDateFormat = compactDateFormat.ifBlank { current.compactDateFormat },
                                        browseSortDescending = browseSortDescending,
                                        fuelEfficiencyAssignmentMethod = assignmentMethod,
                                        reminderTimeAlertPercent = reminderTimePercentText.toIntOrNull()
                                            ?: error("Enter a valid reminder time percentage."),
                                        reminderDistanceAlertPercent = reminderDistancePercentText.toIntOrNull()
                                            ?: error("Enter a valid reminder distance percentage."),
                                        backupFrequencyHours = backupFrequencyHoursText.toIntOrNull()
                                            ?: error("Enter a valid backup frequency in hours."),
                                        backupHistoryCount = backupHistoryCountText.toIntOrNull()
                                            ?: error("Enter a valid backup history count."),
                                        useLocation = useLocation,
                                        notificationsEnabled = notificationsEnabled,
                                        notificationLedEnabled = notificationLedEnabled,
                                        visibleFields = visibleFields,
                                    )
                                }
                            }.onSuccess {
                                errorMessage = null
                                successMessage = "Settings saved locally."
                            }.onFailure { error ->
                                errorMessage = error.message
                                successMessage = null
                            }
                        }
                    },
                ) {
                    Text("Save Settings")
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> SettingsEnumSection(
    title: String,
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    label: (T) -> String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, fontWeight = FontWeight.Medium)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            options.forEach { option ->
                FilterChip(
                    selected = option == selected,
                    onClick = { onSelect(option) },
                    label = { Text(label(option)) },
                )
            }
        }
    }
}

private fun OptionalFieldToggle.displayLabel(): String = when (this) {
    OptionalFieldToggle.VEHICLE_LICENSE_PLATE -> "Vehicle License Plate"
    OptionalFieldToggle.VEHICLE_VIN -> "Vehicle VIN"
    OptionalFieldToggle.VEHICLE_INSURANCE_POLICY -> "Vehicle Insurance Policy"
    OptionalFieldToggle.VEHICLE_BODY_STYLE -> "Vehicle Body Style"
    OptionalFieldToggle.VEHICLE_COLOR -> "Vehicle Color"
    OptionalFieldToggle.VEHICLE_ENGINE_DISPLACEMENT -> "Vehicle Engine Displacement"
    OptionalFieldToggle.VEHICLE_FUEL_TANK_CAPACITY -> "Vehicle Fuel Tank Capacity"
    OptionalFieldToggle.VEHICLE_PURCHASE_INFO -> "Vehicle Purchase Info"
    OptionalFieldToggle.VEHICLE_SELLING_INFO -> "Vehicle Selling Info"
    OptionalFieldToggle.PAYMENT_TYPE -> "Payment Type"
    OptionalFieldToggle.FUEL_TYPE -> "Fuel Type"
    OptionalFieldToggle.FUEL_ADDITIVE -> "Fuel Additive"
    OptionalFieldToggle.FUELING_STATION -> "Fueling Station"
    OptionalFieldToggle.SERVICE_CENTER -> "Service Center"
    OptionalFieldToggle.EXPENSE_CENTER -> "Expense Center"
    OptionalFieldToggle.TAGS -> "Tags"
    OptionalFieldToggle.NOTES -> "Notes"
    OptionalFieldToggle.AVERAGE_SPEED -> "Average Speed"
    OptionalFieldToggle.DRIVING_MODE -> "Driving Mode"
    OptionalFieldToggle.DRIVING_CONDITION -> "Driving Condition"
    OptionalFieldToggle.TRIP_PURPOSE -> "Trip Purpose"
    OptionalFieldToggle.TRIP_CLIENT -> "Trip Client"
    OptionalFieldToggle.TRIP_LOCATION -> "Trip Location"
    OptionalFieldToggle.TRIP_TAX_DEDUCTION -> "Trip Tax Deduction"
    OptionalFieldToggle.TRIP_REIMBURSEMENT -> "Trip Reimbursement"
}
