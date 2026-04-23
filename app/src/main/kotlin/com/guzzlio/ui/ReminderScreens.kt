package com.guzzlio.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
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
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.guzzlio.data.GarageRepository
import com.guzzlio.domain.model.AppPreferenceSnapshot
import com.guzzlio.domain.model.ReminderCenterItem
import com.guzzlio.domain.model.ServiceReminder
import com.guzzlio.domain.model.ServiceType
import com.guzzlio.domain.model.Vehicle
import java.time.LocalDate
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RemindersCenterScreen(
    repository: GarageRepository,
    preselectedVehicleId: Long? = null,
    onBack: () -> Unit,
    onAddReminder: (Long) -> Unit,
    onEditReminder: (Long, Long) -> Unit,
    onCreateService: (Long, Long) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val vehicles by repository.observeVehicles().collectAsStateWithLifecycle(initialValue = emptyList())
    val preferences by repository.preferences.collectAsStateWithLifecycle(initialValue = AppPreferenceSnapshot())
    var selectedVehicleId by rememberSaveable(preselectedVehicleId) { mutableLongStateOf(preselectedVehicleId ?: -1L) }
    val reminders by repository.observeReminderCenter(selectedVehicleId.takeIf { it > 0L })
        .collectAsStateWithLifecycle(initialValue = emptyList())
    var deleteTarget by remember { mutableStateOf<ReminderCenterItem?>(null) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(vehicles, preselectedVehicleId) {
        if (vehicles.isEmpty()) {
            selectedVehicleId = -1L
            return@LaunchedEffect
        }
        if (preselectedVehicleId != null && vehicles.any { it.id == preselectedVehicleId }) {
            selectedVehicleId = preselectedVehicleId
            return@LaunchedEffect
        }
        if (selectedVehicleId <= 0L || vehicles.none { it.id == selectedVehicleId }) {
            selectedVehicleId = vehicles.firstOrNull { it.lifecycle == com.guzzlio.domain.model.VehicleLifecycle.ACTIVE }?.id
                ?: vehicles.first().id
        }
    }

    deleteTarget?.let { item ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Delete Reminder") },
            text = { Text("Delete the ${item.serviceTypeName} reminder for ${item.vehicleName}?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            repository.deleteReminder(item.reminder.id)
                            deleteTarget = null
                        }
                    },
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) {
                    Text("Cancel")
                }
            },
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Reminders") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } },
                actions = {
                    TextButton(
                        enabled = vehicles.isNotEmpty() && selectedVehicleId > 0L,
                        onClick = {
                            onAddReminder(selectedVehicleId)
                        },
                    ) {
                        Text("Add")
                    }
                },
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
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Reminder Center", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("Track due service schedules locally and jump straight into a matching service record when work gets done.")
                        if (vehicles.isNotEmpty()) {
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterChip(
                                    selected = selectedVehicleId <= 0L,
                                    onClick = { selectedVehicleId = -1L },
                                    label = { Text("All Vehicles") },
                                )
                                vehicles.forEach { vehicle ->
                                    FilterChip(
                                        selected = selectedVehicleId == vehicle.id,
                                        onClick = { selectedVehicleId = vehicle.id },
                                        label = { Text(vehicle.name) },
                                    )
                                }
                            }
                            selectedVehicleId.takeIf { it > 0L }?.let { vehicleId ->
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    AssistChip(
                                        onClick = {
                                            scope.launch {
                                                val created = repository.seedReminderDefaultsForVehicle(vehicleId)
                                                statusMessage = if (created > 0) {
                                                    "Seeded $created reminder default(s)."
                                                } else {
                                                    "No missing default reminders were found."
                                                }
                                            }
                                        },
                                        label = { Text("Seed Defaults") },
                                    )
                                    AssistChip(
                                        onClick = { onAddReminder(vehicleId) },
                                        label = { Text("New Reminder") },
                                    )
                                }
                            }
                        } else {
                            Text("Add a vehicle first to manage reminders.")
                        }
                        statusMessage?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                }
            }
            if (reminders.isEmpty()) {
                item {
                    Card {
                        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("No reminders scheduled.")
                            Text("Create a reminder manually or seed defaults from a vehicle.")
                        }
                    }
                }
            } else {
                items(reminders.size) { index ->
                    val item = reminders[index]
                    Card {
                        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(item.serviceTypeName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            if (selectedVehicleId <= 0L) {
                                Text(item.vehicleName)
                            }
                            Text(formatReminderDueSummary(item, preferences))
                            Text(formatReminderIntervalSummary(item.reminder, item.distanceUnitLabel), color = MaterialTheme.colorScheme.onSurfaceVariant)
                            formatReminderStatusSummary(item)?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                AssistChip(
                                    onClick = { onEditReminder(item.reminder.vehicleId, item.reminder.id) },
                                    label = { Text("Edit") },
                                )
                                AssistChip(
                                    onClick = { onCreateService(item.reminder.vehicleId, item.reminder.serviceTypeId) },
                                    label = { Text("New Service") },
                                )
                                AssistChip(
                                    onClick = {
                                        scope.launch {
                                            repository.saveReminder(
                                                item.reminder.copy(timeAlertSilent = !item.reminder.timeAlertSilent),
                                            )
                                        }
                                    },
                                    label = { Text(if (item.reminder.timeAlertSilent) "Time Alert On" else "Silence Time") },
                                )
                                AssistChip(
                                    onClick = {
                                        scope.launch {
                                            repository.saveReminder(
                                                item.reminder.copy(distanceAlertSilent = !item.reminder.distanceAlertSilent),
                                            )
                                        }
                                    },
                                    label = { Text(if (item.reminder.distanceAlertSilent) "Distance Alert On" else "Silence Distance") },
                                )
                                AssistChip(
                                    onClick = { deleteTarget = item },
                                    label = { Text("Delete") },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ReminderEditorScreen(
    repository: GarageRepository,
    vehicleId: Long,
    reminderId: Long,
    onBack: () -> Unit,
    onSaved: (Long) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val vehicles by repository.observeVehicles().collectAsStateWithLifecycle(initialValue = emptyList())
    val serviceTypes by repository.observeServiceTypes().collectAsStateWithLifecycle(initialValue = emptyList())
    val existingReminder by produceState<ServiceReminder?>(initialValue = null, key1 = reminderId) {
        value = if (reminderId > 0L) repository.getReminder(reminderId) else null
    }

    var initialized by rememberSaveable(reminderId, vehicleId) { mutableStateOf(false) }
    var selectedVehicleId by rememberSaveable { mutableLongStateOf(0L) }
    var selectedServiceTypeId by rememberSaveable { mutableLongStateOf(0L) }
    var serviceTypeFilter by rememberSaveable { mutableStateOf("") }
    var intervalMonthsText by rememberSaveable { mutableStateOf("") }
    var intervalDistanceText by rememberSaveable { mutableStateOf("") }
    var dueDateText by rememberSaveable { mutableStateOf("") }
    var dueDistanceText by rememberSaveable { mutableStateOf("") }
    var timeAlertSilent by rememberSaveable { mutableStateOf(false) }
    var distanceAlertSilent by rememberSaveable { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var helperMessage by remember { mutableStateOf<String?>(null) }
    var lastAutoSuggestionKey by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(existingReminder, vehicles) {
        if (initialized) return@LaunchedEffect
        existingReminder?.let { reminder ->
            selectedVehicleId = reminder.vehicleId
            selectedServiceTypeId = reminder.serviceTypeId
            intervalMonthsText = reminder.intervalTimeMonths.takeIf { it > 0 }?.toString().orEmpty()
            intervalDistanceText = reminder.intervalDistance?.takeIf { it > 0.0 }?.toStableString().orEmpty()
            dueDateText = reminder.dueDate?.toString().orEmpty()
            dueDistanceText = reminder.dueDistance?.toStableString().orEmpty()
            timeAlertSilent = reminder.timeAlertSilent
            distanceAlertSilent = reminder.distanceAlertSilent
        } ?: run {
            selectedVehicleId = when {
                vehicleId > 0L -> vehicleId
                vehicles.size == 1 -> vehicles.single().id
                else -> 0L
            }
        }
        initialized = true
    }

    LaunchedEffect(selectedVehicleId, selectedServiceTypeId, existingReminder, serviceTypes) {
        if (!initialized || existingReminder != null || selectedVehicleId <= 0L || selectedServiceTypeId <= 0L) return@LaunchedEffect
        val key = "$selectedVehicleId:$selectedServiceTypeId"
        if (lastAutoSuggestionKey == key) return@LaunchedEffect
        val serviceType = serviceTypes.firstOrNull { it.id == selectedServiceTypeId } ?: return@LaunchedEffect
        intervalMonthsText = serviceType.defaultTimeReminderMonths.takeIf { it > 0 }?.toString().orEmpty()
        intervalDistanceText = serviceType.defaultDistanceReminder?.takeIf { it > 0.0 }?.toStableString().orEmpty()
        val suggestion = repository.suggestReminderSchedule(
            vehicleId = selectedVehicleId,
            serviceTypeId = selectedServiceTypeId,
            intervalTimeMonths = serviceType.defaultTimeReminderMonths,
            intervalDistance = serviceType.defaultDistanceReminder,
        )
        dueDateText = suggestion.dueDate?.toString().orEmpty()
        dueDistanceText = suggestion.dueDistance?.toStableString().orEmpty()
        helperMessage = "Defaults and next due values were prefilled from service history or the current vehicle state."
        lastAutoSuggestionKey = key
    }

    val filteredServiceTypes = remember(serviceTypes, serviceTypeFilter) {
        val query = serviceTypeFilter.trim()
        serviceTypes
            .filter { query.isBlank() || it.name.contains(query, ignoreCase = true) }
            .take(18)
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(if (reminderId > 0L) "Edit Reminder" else "New Reminder") },
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
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Service Reminder", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("Preload due dates and mileage from the most recent matching service, then adjust only if you need something different.")
                    }
                }
            }
            item {
                Card {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Vehicle", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        if (vehicles.isEmpty()) {
                            Text("Add a vehicle first.")
                        } else {
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                vehicles.forEach { vehicle ->
                                    FilterChip(
                                        selected = selectedVehicleId == vehicle.id,
                                        onClick = {
                                            selectedVehicleId = vehicle.id
                                            if (existingReminder == null) {
                                                lastAutoSuggestionKey = ""
                                            }
                                        },
                                        label = { Text(vehicle.name) },
                                    )
                                }
                            }
                        }
                    }
                }
            }
            item {
                Card {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Service Type", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        OutlinedTextField(
                            value = serviceTypeFilter,
                            onValueChange = { serviceTypeFilter = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Filter Service Types") },
                            singleLine = true,
                        )
                        if (filteredServiceTypes.isEmpty()) {
                            Text("No matching service types.")
                        } else {
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                filteredServiceTypes.forEach { type ->
                                    FilterChip(
                                        selected = selectedServiceTypeId == type.id,
                                        onClick = {
                                            selectedServiceTypeId = type.id
                                            if (existingReminder == null) {
                                                lastAutoSuggestionKey = ""
                                            }
                                        },
                                        label = { Text(type.name) },
                                    )
                                }
                            }
                        }
                    }
                }
            }
            item {
                Card {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = intervalMonthsText,
                            onValueChange = { intervalMonthsText = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Time Interval (months)") },
                            singleLine = true,
                        )
                        OutlinedTextField(
                            value = intervalDistanceText,
                            onValueChange = { intervalDistanceText = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Distance Interval") },
                            singleLine = true,
                        )
                        PickerDateField(
                            value = dueDateText,
                            onValueChange = { dueDateText = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = "Due Date (yyyy-MM-dd)",
                        )
                        OutlinedTextField(
                            value = dueDistanceText,
                            onValueChange = { dueDistanceText = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Due Distance") },
                            singleLine = true,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AssistChip(
                                onClick = {
                                    if (selectedVehicleId <= 0L || selectedServiceTypeId <= 0L) {
                                        errorMessage = "Choose a vehicle and service type first."
                                        return@AssistChip
                                    }
                                    scope.launch {
                                        runCatching {
                                            repository.suggestReminderSchedule(
                                                vehicleId = selectedVehicleId,
                                                serviceTypeId = selectedServiceTypeId,
                                                intervalTimeMonths = intervalMonthsText.trim().toIntOrNull() ?: 0,
                                                intervalDistance = intervalDistanceText.trim().toDoubleOrNull(),
                                                reminderId = existingReminder?.id ?: 0L,
                                            )
                                        }.onSuccess { suggestion ->
                                            dueDateText = suggestion.dueDate?.toString().orEmpty()
                                            dueDistanceText = suggestion.dueDistance?.toStableString().orEmpty()
                                            helperMessage = "Due values refreshed from the latest matching service history."
                                            errorMessage = null
                                        }.onFailure {
                                            errorMessage = it.message
                                        }
                                    }
                                },
                                label = { Text("Suggest Due") },
                            )
                        }
                        ToggleRow(
                            label = "Silence Time Alert",
                            checked = timeAlertSilent,
                            onCheckedChange = { timeAlertSilent = it },
                        )
                        ToggleRow(
                            label = "Silence Distance Alert",
                            checked = distanceAlertSilent,
                            onCheckedChange = { distanceAlertSilent = it },
                        )
                    }
                }
            }
            helperMessage?.let { message ->
                item {
                    Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            errorMessage?.let { message ->
                item {
                    Text(message, color = MaterialTheme.colorScheme.error)
                }
            }
            item {
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        scope.launch {
                            runCatching {
                                val parsedDueDate = when {
                                    dueDateText.isBlank() -> null
                                    else -> parseDueDate(dueDateText) ?: error("Use `yyyy-MM-dd` for the due date.")
                                }
                                val savedId = repository.saveReminder(
                                    ServiceReminder(
                                        id = existingReminder?.id ?: 0L,
                                        legacySourceId = existingReminder?.legacySourceId,
                                        vehicleId = selectedVehicleId.takeIf { it > 0L }
                                            ?: error("Choose a vehicle first."),
                                        serviceTypeId = selectedServiceTypeId.takeIf { it > 0L }
                                            ?: error("Choose a service type first."),
                                        intervalTimeMonths = intervalMonthsText.trim().toIntOrNull() ?: 0,
                                        intervalDistance = intervalDistanceText.trim().toDoubleOrNull(),
                                        dueDate = parsedDueDate,
                                        dueDistance = dueDistanceText.trim().toDoubleOrNull(),
                                        timeAlertSilent = timeAlertSilent,
                                        distanceAlertSilent = distanceAlertSilent,
                                        lastTimeAlert = existingReminder?.lastTimeAlert,
                                        lastDistanceAlert = existingReminder?.lastDistanceAlert,
                                    ),
                                )
                                savedId
                            }.onSuccess { savedId ->
                                errorMessage = null
                                onSaved(savedId)
                            }.onFailure {
                                errorMessage = it.message
                            }
                        }
                    },
                ) {
                    Text(if (reminderId > 0L) "Save Reminder" else "Add Reminder")
                }
            }
        }
    }
}

private fun parseDueDate(raw: String): LocalDate? = runCatching {
    LocalDate.parse(raw.trim())
}.getOrNull()

private fun formatReminderDueSummary(
    item: ReminderCenterItem,
    preferences: AppPreferenceSnapshot,
): String = listOfNotNull(
    item.reminder.dueDate?.let { "Due ${it.formatForDisplay(preferences, compact = true)}" },
    item.reminder.dueDistance?.let { "At ${it.toStableString()} ${item.distanceUnitLabel}" },
).joinToString(" | ").ifBlank { "Scheduled" }

private fun formatReminderIntervalSummary(reminder: ServiceReminder, distanceUnitLabel: String): String = listOfNotNull(
    reminder.intervalTimeMonths.takeIf { it > 0 }?.let { "Every $it month(s)" },
    reminder.intervalDistance?.takeIf { it > 0.0 }?.let { "Every ${it.toStableString()} $distanceUnitLabel" },
).joinToString(" | ").ifBlank { "No interval set" }

private fun formatReminderStatusSummary(item: ReminderCenterItem): String? {
    val pieces = mutableListOf<String>()
    item.currentOdometer?.let { current ->
        item.reminder.dueDistance?.let { due ->
            val delta = due - current
            val label = "${kotlin.math.abs(delta).toStableString()} ${item.distanceUnitLabel}"
            pieces += if (delta >= 0.0) "$label remaining" else "$label overdue"
        }
    }
    item.reminder.dueDate?.let { dueDate ->
        val days = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), dueDate)
        val label = "${kotlin.math.abs(days)} day(s)"
        pieces += if (days >= 0) "$label remaining" else "$label overdue"
    }
    if (item.reminder.timeAlertSilent) pieces += "time alert silent"
    if (item.reminder.distanceAlertSilent) pieces += "distance alert silent"
    return pieces.takeIf { it.isNotEmpty() }?.joinToString(" | ")
}
