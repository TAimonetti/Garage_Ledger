package com.garageledger.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.garageledger.data.GarageRepository
import com.garageledger.domain.calc.TripCostBreakdown
import com.garageledger.domain.model.RecordAttachment
import com.garageledger.domain.model.RecordFamily
import com.garageledger.domain.model.TripRecord
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TripEditorScreen(
    repository: GarageRepository,
    vehicleId: Long,
    recordId: Long,
    onBack: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val vehicles by repository.observeVehicles().collectAsStateWithLifecycle(initialValue = emptyList())
    val preferences by repository.preferences.collectAsStateWithLifecycle(initialValue = com.garageledger.domain.model.AppPreferenceSnapshot())
    val existingRecord by produceState<TripRecord?>(initialValue = null, key1 = recordId) {
        value = if (recordId > 0L) repository.getTrip(recordId) else null
    }
    val existingAttachments by produceState(initialValue = emptyList<RecordAttachment>(), key1 = recordId) {
        value = if (recordId > 0L) repository.getRecordAttachments(RecordFamily.TRIP, recordId) else emptyList()
    }
    val tripTypes by produceState(initialValue = emptyList<com.garageledger.domain.model.TripType>()) {
        value = repository.getTripTypes()
    }
    val priorTrips by produceState(initialValue = emptyList<TripRecord>(), key1 = vehicleId) {
        value = repository.getVehicleTrips(vehicleId)
    }
    val purposeSuggestions by produceState(initialValue = emptyList<String>()) {
        value = repository.getTripPurposeSuggestions()
    }
    val clientSuggestions by produceState(initialValue = emptyList<String>()) {
        value = repository.getTripClientSuggestions()
    }
    val locationSuggestions by produceState(initialValue = emptyList<String>()) {
        value = repository.getTripLocationSuggestions()
    }

    var initialized by rememberSaveable(recordId) { mutableStateOf(false) }
    var startDateText by rememberSaveable { mutableStateOf("") }
    var startOdometerText by rememberSaveable { mutableStateOf("") }
    var startLocation by rememberSaveable { mutableStateOf("") }
    var endDateText by rememberSaveable { mutableStateOf("") }
    var endOdometerText by rememberSaveable { mutableStateOf("") }
    var endLocation by rememberSaveable { mutableStateOf("") }
    var purpose by rememberSaveable { mutableStateOf("") }
    var client by rememberSaveable { mutableStateOf("") }
    var taxRateText by rememberSaveable { mutableStateOf("") }
    var reimbursementRateText by rememberSaveable { mutableStateOf("") }
    var tagsText by rememberSaveable { mutableStateOf("") }
    var notesText by rememberSaveable { mutableStateOf("") }
    var paid by rememberSaveable { mutableStateOf(false) }
    var tripTypeId by remember { mutableStateOf<Long?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var attachments by remember(recordId) { mutableStateOf(emptyList<RecordAttachment>()) }
    var attachmentsInitialized by remember(recordId) { mutableStateOf(false) }
    var showCustomizeFields by remember { mutableStateOf(false) }

    LaunchedEffect(existingRecord) {
        if (initialized) return@LaunchedEffect
        val record = existingRecord
        if (record != null) {
            startDateText = record.startDateTime.format(EditorDateFormatter)
            startOdometerText = record.startOdometerReading.toString()
            startLocation = record.startLocation
            endDateText = record.endDateTime?.format(EditorDateFormatter).orEmpty()
            endOdometerText = record.endOdometerReading?.toString().orEmpty()
            endLocation = record.endLocation
            purpose = record.purpose
            client = record.client
            taxRateText = record.taxDeductionRate?.toString().orEmpty()
            reimbursementRateText = record.reimbursementRate?.toString().orEmpty()
            tagsText = record.tags.joinToString(", ")
            notesText = record.notes
            paid = record.paid
            tripTypeId = record.tripTypeId
        } else {
            startDateText = java.time.LocalDateTime.now().format(EditorDateFormatter)
        }
        initialized = true
    }

    LaunchedEffect(existingAttachments) {
        if (attachmentsInitialized) return@LaunchedEffect
        attachments = existingAttachments
        attachmentsInitialized = true
    }

    val vehicleName = vehicles.firstOrNull { it.id == vehicleId }?.name ?: "Trip"
    val visibleFields = preferences.visibleFields
    val showTripLocation = com.garageledger.domain.model.OptionalFieldToggle.TRIP_LOCATION in visibleFields
    val showTripPurpose = com.garageledger.domain.model.OptionalFieldToggle.TRIP_PURPOSE in visibleFields
    val showTripClient = com.garageledger.domain.model.OptionalFieldToggle.TRIP_CLIENT in visibleFields
    val showTripTaxDeduction = com.garageledger.domain.model.OptionalFieldToggle.TRIP_TAX_DEDUCTION in visibleFields
    val showTripReimbursement = com.garageledger.domain.model.OptionalFieldToggle.TRIP_REIMBURSEMENT in visibleFields
    val showTags = com.garageledger.domain.model.OptionalFieldToggle.TAGS in visibleFields
    val showNotes = com.garageledger.domain.model.OptionalFieldToggle.NOTES in visibleFields
    val parsedStart = remember(startDateText) { parseEditorDateTime(startDateText) }
    val parsedEnd = remember(endDateText) { parseEditorDateTime(endDateText).takeIf { endDateText.isNotBlank() } }
    val startOdometer = remember(startOdometerText) { startOdometerText.toDoubleOrNull() }
    val endOdometer = remember(endOdometerText) { endOdometerText.toDoubleOrNull() }
    val distancePreview = remember(startOdometer, endOdometer) {
        if (startOdometer != null && endOdometer != null && endOdometer >= startOdometer) {
            endOdometer - startOdometer
        } else {
            null
        }
    }
    val durationPreview = remember(parsedStart, parsedEnd) {
        if (parsedStart != null && parsedEnd != null && !parsedEnd.isBefore(parsedStart)) {
            java.time.Duration.between(parsedStart, parsedEnd)
        } else {
            null
        }
    }
    val tripDraft = remember(
        parsedStart,
        startOdometer,
        parsedEnd,
        endOdometer,
        startLocation,
        endLocation,
        purpose,
        client,
        taxRateText,
        reimbursementRateText,
        tagsText,
        notesText,
        paid,
        tripTypeId,
        preferences.distanceUnit,
        existingRecord,
    ) {
        if (parsedStart == null || startOdometer == null) {
            null
        } else {
            TripRecord(
                id = existingRecord?.id ?: 0L,
                legacySourceId = existingRecord?.legacySourceId,
                vehicleId = vehicleId,
                startDateTime = parsedStart,
                startOdometerReading = startOdometer,
                startLocation = startLocation,
                endDateTime = parsedEnd,
                endOdometerReading = endOdometer,
                endLocation = endLocation,
                distanceUnit = preferences.distanceUnit,
                tripTypeId = tripTypeId,
                purpose = purpose,
                client = client,
                taxDeductionRate = taxRateText.toDoubleOrNull(),
                reimbursementRate = reimbursementRateText.toDoubleOrNull(),
                paid = paid,
                tags = tagsText.split(",").map(String::trim).filter(String::isNotBlank),
                notes = notesText,
            )
        }
    }
    val estimatedCost by produceState<TripCostBreakdown?>(initialValue = null, key1 = tripDraft) {
        value = tripDraft
            ?.takeIf { it.endOdometerReading != null }
            ?.let { runCatching { repository.estimateTripCost(it) }.getOrNull() }
    }

    val lastCompletedTrip = priorTrips.lastOrNull { it.id != recordId && it.endDateTime != null }

    if (showCustomizeFields) {
        VisibleFieldsDialog(
            title = "Customize Trip Screen",
            options = TripVisibleFieldOptions,
            visibleFields = visibleFields,
            onToggle = { toggle, visible ->
                scope.launch { repository.setVisibleField(toggle, visible) }
            },
            onDismiss = { showCustomizeFields = false },
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(if (recordId > 0L) "Edit Trip" else "New Trip") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } },
                actions = {
                    TextButton(onClick = { showCustomizeFields = true }) {
                        Text("Customize")
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
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(vehicleName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("Trips can stay open, inherit the last destination, or flip into a return trip.")
                        if (lastCompletedTrip != null) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                TextButton(
                                    onClick = {
                                        startLocation = lastCompletedTrip.endLocation.ifBlank { lastCompletedTrip.startLocation }
                                        lastCompletedTrip.endOdometerReading?.let { startOdometerText = it.toString() }
                                    },
                                ) {
                                    Text("From Last Destination")
                                }
                                TextButton(
                                    onClick = {
                                        startLocation = lastCompletedTrip.endLocation.ifBlank { lastCompletedTrip.startLocation }
                                        endLocation = lastCompletedTrip.startLocation
                                        lastCompletedTrip.endOdometerReading?.let { startOdometerText = it.toString() }
                                    },
                                ) {
                                    Text("Return Last Trip")
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
                            value = startDateText,
                            onValueChange = { startDateText = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Start (yyyy-MM-dd HH:mm)") },
                            singleLine = true,
                        )
                        OutlinedTextField(
                            value = startOdometerText,
                            onValueChange = { startOdometerText = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Start Odometer (${preferences.distanceUnit.storageValue})") },
                            singleLine = true,
                        )
                        if (showTripLocation) {
                            OutlinedTextField(
                                value = startLocation,
                                onValueChange = { startLocation = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Start Location") },
                            )
                            SuggestionRow(locationSuggestions, onSelect = { startLocation = it })
                        }
                    }
                }
            }
            item {
                Card {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = endDateText,
                            onValueChange = { endDateText = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("End (optional)") },
                            singleLine = true,
                        )
                        OutlinedTextField(
                            value = endOdometerText,
                            onValueChange = { endOdometerText = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("End Odometer (optional)") },
                            singleLine = true,
                        )
                        if (showTripLocation) {
                            OutlinedTextField(
                                value = endLocation,
                                onValueChange = { endLocation = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("End Location") },
                            )
                            SuggestionRow(locationSuggestions, onSelect = { endLocation = it })
                        }
                    }
                }
            }
            item {
                Card {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Trip Type", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            tripTypes.forEach { type ->
                                FilterChip(
                                    selected = tripTypeId == type.id,
                                    onClick = {
                                        tripTypeId = if (tripTypeId == type.id) null else type.id
                                        if (taxRateText.isBlank() && type.defaultTaxDeductionRate != null) {
                                            taxRateText = type.defaultTaxDeductionRate.toString()
                                        }
                                    },
                                    label = { Text(type.name) },
                                )
                            }
                        }
                        if (showTripPurpose) {
                            OutlinedTextField(
                                value = purpose,
                                onValueChange = { purpose = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Purpose") },
                            )
                            SuggestionRow(purposeSuggestions, onSelect = { purpose = it })
                        }
                        if (showTripClient) {
                            OutlinedTextField(
                                value = client,
                                onValueChange = { client = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Client") },
                            )
                            SuggestionRow(clientSuggestions, onSelect = { client = it })
                        }
                    }
                }
            }
            item {
                Card {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (showTripTaxDeduction) {
                            OutlinedTextField(
                                value = taxRateText,
                                onValueChange = { taxRateText = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Tax Deduction Rate") },
                                singleLine = true,
                            )
                        }
                        if (showTripReimbursement) {
                            OutlinedTextField(
                                value = reimbursementRateText,
                                onValueChange = { reimbursementRateText = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Reimbursement Rate") },
                                singleLine = true,
                            )
                            ToggleRow("Paid", paid) { paid = it }
                        }
                        if (showTags) {
                            OutlinedTextField(
                                value = tagsText,
                                onValueChange = { tagsText = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Tags") },
                            )
                        }
                        if (showNotes) {
                            OutlinedTextField(
                                value = notesText,
                                onValueChange = { notesText = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Notes") },
                                minLines = 3,
                            )
                        }
                    }
                }
            }
            item {
                AttachmentEditorCard(
                    vehicleId = vehicleId,
                    recordFamily = RecordFamily.TRIP,
                    attachments = attachments,
                    onAttachmentsChange = { attachments = it },
                    onError = { errorMessage = it },
                )
            }
            item {
                Card {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Trip Preview", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(
                            "Distance: " + (distancePreview?.let { "${it.toStableString()} ${preferences.distanceUnit.storageValue}" } ?: "Open trip"),
                        )
                        Text(
                            "Duration: " + (
                                durationPreview?.let { duration ->
                                    "${duration.toHours()}h ${duration.toMinutesPart()}m"
                                } ?: "Open trip"
                            ),
                        )
                        if (showTripTaxDeduction) taxRateText.toDoubleOrNull()?.let { rate ->
                            distancePreview?.let { distance ->
                                Text("Tax Deduction: ${(rate * distance).asCurrency()}")
                            }
                        }
                        if (showTripReimbursement) reimbursementRateText.toDoubleOrNull()?.let { rate ->
                            distancePreview?.let { distance ->
                                Text("Reimbursement: ${(rate * distance).asCurrency()}")
                            }
                        }
                        estimatedCost?.let { estimate ->
                            Text("Direct Expenses: ${estimate.directExpenseCost.asCurrency()}")
                            Text("Fuel Estimate: ${estimate.estimatedFuelCost.asCurrency()}")
                            Text("Service Estimate: ${estimate.estimatedServiceCost.asCurrency()}")
                            Text("Estimated Total Cost: ${estimate.totalCost.asCurrency()}", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
            errorMessage?.let { message ->
                item {
                    Text(message, color = MaterialTheme.colorScheme.error)
                }
            }
            item {
                Spacer(Modifier.height(8.dp))
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        scope.launch {
                            runCatching {
                                val savedId = repository.saveTrip(
                                    tripDraft ?: error("Enter a valid start date and start odometer."),
                                )
                                repository.replaceRecordAttachments(
                                    vehicleId = vehicleId,
                                    recordFamily = RecordFamily.TRIP,
                                    recordId = savedId,
                                    attachments = attachments,
                                )
                            }.onSuccess {
                                errorMessage = null
                                onBack()
                            }.onFailure {
                                errorMessage = it.message
                            }
                        }
                    },
                ) {
                    Text(if (recordId > 0L) "Save Trip" else "Add Trip")
                }
            }
        }
    }
}
