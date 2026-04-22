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
    copyFromTripId: Long? = null,
    finishMode: Boolean = false,
    onBack: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val vehicles by repository.observeVehicles().collectAsStateWithLifecycle(initialValue = emptyList())
    val preferences by repository.preferences.collectAsStateWithLifecycle(initialValue = com.garageledger.domain.model.AppPreferenceSnapshot())
    val existingRecord by produceState<TripRecord?>(initialValue = null, key1 = recordId) {
        value = if (recordId > 0L) repository.getTrip(recordId) else null
    }
    val copySourceTrip by produceState<TripRecord?>(initialValue = null, key1 = copyFromTripId) {
        value = copyFromTripId?.let { repository.getTrip(it) }
    }
    val existingAttachments by produceState(initialValue = emptyList<RecordAttachment>(), key1 = recordId) {
        value = if (recordId > 0L) repository.getRecordAttachments(RecordFamily.TRIP, recordId) else emptyList()
    }
    val suggestedOdometer by produceState<Double?>(initialValue = null, key1 = vehicleId) {
        value = repository.getSuggestedCurrentOdometer(vehicleId)
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
    val tagSuggestions by produceState(initialValue = emptyList<String>()) {
        value = repository.getTagSuggestions()
    }

    var initialized by rememberSaveable(recordId, copyFromTripId, finishMode) { mutableStateOf(false) }
    var startDateText by rememberSaveable { mutableStateOf("") }
    var startOdometerText by rememberSaveable { mutableStateOf("") }
    var startLocation by rememberSaveable { mutableStateOf("") }
    var startLatitude by rememberSaveable { mutableStateOf<Double?>(null) }
    var startLongitude by rememberSaveable { mutableStateOf<Double?>(null) }
    var endDateText by rememberSaveable { mutableStateOf("") }
    var endOdometerText by rememberSaveable { mutableStateOf("") }
    var endOdometerModeName by rememberSaveable { mutableStateOf(TripEndOdometerMode.ABSOLUTE.name) }
    var endLocation by rememberSaveable { mutableStateOf("") }
    var endLatitude by rememberSaveable { mutableStateOf<Double?>(null) }
    var endLongitude by rememberSaveable { mutableStateOf<Double?>(null) }
    var purpose by rememberSaveable { mutableStateOf("") }
    var client by rememberSaveable { mutableStateOf("") }
    var taxRateText by rememberSaveable { mutableStateOf("") }
    var reimbursementRateText by rememberSaveable { mutableStateOf("") }
    var tagsText by rememberSaveable { mutableStateOf("") }
    var notesText by rememberSaveable { mutableStateOf("") }
    var paid by rememberSaveable { mutableStateOf(false) }
    var tripTypeId by remember { mutableStateOf<Long?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var attachments by remember(recordId, copyFromTripId) { mutableStateOf(emptyList<RecordAttachment>()) }
    var attachmentsInitialized by remember(recordId, copyFromTripId) { mutableStateOf(false) }
    var showCustomizeFields by remember { mutableStateOf(false) }

    LaunchedEffect(existingRecord, copySourceTrip, finishMode, suggestedOdometer) {
        if (initialized) return@LaunchedEffect
        val record = existingRecord
        if (record != null) {
            startDateText = record.startDateTime.format(EditorDateFormatter)
            startOdometerText = record.startOdometerReading.toString()
            startLocation = record.startLocation
            startLatitude = record.startLatitude
            startLongitude = record.startLongitude
            endDateText = record.endDateTime?.format(EditorDateFormatter).orEmpty()
            endOdometerText = record.endOdometerReading?.toString().orEmpty()
            endOdometerModeName = TripEndOdometerMode.ABSOLUTE.name
            endLocation = record.endLocation
            endLatitude = record.endLatitude
            endLongitude = record.endLongitude
            purpose = record.purpose
            client = record.client
            taxRateText = record.taxDeductionRate?.toString().orEmpty()
            reimbursementRateText = record.reimbursementRate?.toString().orEmpty()
            tagsText = record.tags.joinToString(", ")
            notesText = record.notes
            paid = record.paid
            tripTypeId = record.tripTypeId
            if (finishMode && (record.endDateTime == null || record.endOdometerReading == null) && endDateText.isBlank()) {
                endDateText = java.time.LocalDateTime.now().format(EditorDateFormatter)
            }
        } else {
            val sourceTrip = copySourceTrip
            if (sourceTrip != null) {
            val seed = buildTripCopySeed(
                source = sourceTrip,
                now = java.time.LocalDateTime.now(),
            )
            startDateText = seed.startDateText
            startOdometerText = ""
            startLocation = seed.startLocation
            startLatitude = seed.startLatitude
            startLongitude = seed.startLongitude
            endDateText = ""
            endOdometerText = ""
            endOdometerModeName = TripEndOdometerMode.ABSOLUTE.name
            endLocation = seed.endLocation
            endLatitude = seed.endLatitude
            endLongitude = seed.endLongitude
            purpose = seed.purpose
            client = seed.client
            taxRateText = seed.taxRateText
            reimbursementRateText = seed.reimbursementRateText
            tagsText = seed.tagsText
            notesText = seed.notesText
            paid = false
            tripTypeId = seed.tripTypeId
            } else {
                startDateText = java.time.LocalDateTime.now().format(EditorDateFormatter)
                suggestedOdometer?.let { startOdometerText = it.toString() }
            }
        }
        initialized = true
    }

    LaunchedEffect(existingAttachments) {
        if (attachmentsInitialized) return@LaunchedEffect
        attachments = existingAttachments
        attachmentsInitialized = true
    }

    val vehicleName = vehicles.firstOrNull { it.id == vehicleId }?.name ?: "Trip"
    val isFinishingOpenTrip = finishMode && existingRecord?.let { it.endDateTime == null || it.endOdometerReading == null } == true
    val screenTitle = when {
        isFinishingOpenTrip -> "Finish Trip"
        recordId > 0L -> "Edit Trip"
        copySourceTrip != null -> "Copy Trip"
        else -> "New Trip"
    }
    val visibleFields = preferences.visibleFields
    val showTripLocation = com.garageledger.domain.model.OptionalFieldToggle.TRIP_LOCATION in visibleFields
    val showTripPurpose = com.garageledger.domain.model.OptionalFieldToggle.TRIP_PURPOSE in visibleFields
    val showTripClient = com.garageledger.domain.model.OptionalFieldToggle.TRIP_CLIENT in visibleFields
    val showTripTaxDeduction = com.garageledger.domain.model.OptionalFieldToggle.TRIP_TAX_DEDUCTION in visibleFields
    val showTripReimbursement = com.garageledger.domain.model.OptionalFieldToggle.TRIP_REIMBURSEMENT in visibleFields
    val showTags = com.garageledger.domain.model.OptionalFieldToggle.TAGS in visibleFields
    val showNotes = com.garageledger.domain.model.OptionalFieldToggle.NOTES in visibleFields
    val selectedTags = remember(tagsText) { parseCommaValues(tagsText) }
    val endOdometerMode = remember(endOdometerModeName) { TripEndOdometerMode.valueOf(endOdometerModeName) }
    val selectedTripTypeName = remember(tripTypeId, tripTypes) {
        tripTypes.firstOrNull { it.id == tripTypeId }?.name.orEmpty()
    }
    val parsedStart = remember(startDateText) { parseEditorDateTime(startDateText) }
    val parsedEnd = remember(endDateText) { parseEditorDateTime(endDateText).takeIf { endDateText.isNotBlank() } }
    val startOdometer = remember(startOdometerText) { startOdometerText.toDoubleOrNull() }
    val resolvedEndOdometer = remember(startOdometer, endOdometerText, endOdometerMode) {
        resolveTripEndOdometer(
            startOdometer = startOdometer,
            rawInput = endOdometerText,
            mode = endOdometerMode,
        )
    }
    val distancePreview = remember(startOdometer, resolvedEndOdometer) {
        if (startOdometer != null && resolvedEndOdometer != null && resolvedEndOdometer >= startOdometer) {
            resolvedEndOdometer - startOdometer
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
        resolvedEndOdometer,
        startLocation,
        startLatitude,
        startLongitude,
        endLocation,
        endLatitude,
        endLongitude,
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
                startLatitude = startLatitude,
                startLongitude = startLongitude,
                endDateTime = parsedEnd,
                endOdometerReading = resolvedEndOdometer,
                endLocation = endLocation,
                endLatitude = endLatitude,
                endLongitude = endLongitude,
                distanceUnit = preferences.distanceUnit,
                tripTypeId = tripTypeId,
                purpose = purpose,
                client = client,
                taxDeductionRate = taxRateText.toDoubleOrNull(),
                reimbursementRate = reimbursementRateText.toDoubleOrNull(),
                paid = paid,
                tags = parseCommaValues(tagsText),
                notes = notesText,
            )
        }
    }
    val estimatedCost by produceState<TripCostBreakdown?>(initialValue = null, key1 = tripDraft) {
        value = tripDraft
            ?.takeIf { it.endOdometerReading != null }
            ?.let { runCatching { repository.estimateTripCost(it) }.getOrNull() }
    }

    val lastCompletedTrip = priorTrips.lastOrNull {
        it.id != recordId && it.endDateTime != null && it.endOdometerReading != null
    }
    val openTrip = priorTrips.lastOrNull {
        it.id != recordId && (it.endDateTime == null || it.endOdometerReading == null)
    }

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
                title = { Text(screenTitle) },
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
                        val sourceTrip = copySourceTrip
                        if (sourceTrip != null && recordId <= 0L) {
                            Text(
                                "Copied from ${sourceTrip.startDateTime.format(EditorDateFormatter)}. " +
                                    "Start odometer and all end-trip values are reset for the new trip.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (isFinishingOpenTrip) {
                            Text(
                                "Finish this open trip by entering the arrival details and final odometer.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (recordId <= 0L && openTrip != null) {
                            Text(
                                "An open trip from ${openTrip.startDateTime.format(EditorDateFormatter)} is still active. " +
                                    "Finish it from Vehicle Details if this entry should close that trip.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (lastCompletedTrip != null) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                TextButton(
                                    onClick = {
                                        startLocation = lastCompletedTrip.endLocation.ifBlank { lastCompletedTrip.startLocation }
                                        startLatitude = lastCompletedTrip.endLatitude ?: lastCompletedTrip.startLatitude
                                        startLongitude = lastCompletedTrip.endLongitude ?: lastCompletedTrip.startLongitude
                                        lastCompletedTrip.endOdometerReading?.let { startOdometerText = it.toString() }
                                    },
                                ) {
                                    Text("From Last Destination")
                                }
                                TextButton(
                                    onClick = {
                                        startLocation = lastCompletedTrip.endLocation.ifBlank { lastCompletedTrip.startLocation }
                                        startLatitude = lastCompletedTrip.endLatitude ?: lastCompletedTrip.startLatitude
                                        startLongitude = lastCompletedTrip.endLongitude ?: lastCompletedTrip.startLongitude
                                        endLocation = lastCompletedTrip.startLocation
                                        endLatitude = lastCompletedTrip.startLatitude
                                        endLongitude = lastCompletedTrip.startLongitude
                                        lastCompletedTrip.endOdometerReading?.let { startOdometerText = it.toString() }
                                        purpose = buildReturnTripPurpose(lastCompletedTrip.purpose)
                                        if (client.isBlank()) {
                                            client = lastCompletedTrip.client
                                        }
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
                        PickerDateTimeField(
                            value = startDateText,
                            onValueChange = { startDateText = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = "Start (yyyy-MM-dd HH:mm)",
                        )
                        NumericEntryField(
                            value = startOdometerText,
                            onValueChange = { startOdometerText = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = "Start Odometer (${preferences.distanceUnit.storageValue})",
                            decimalEnabled = true,
                        )
                        if (showTripLocation) {
                            SingleChoiceDialogField(
                                value = startLocation,
                                onValueChange = { startLocation = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = "Start Location",
                                choices = locationSuggestions,
                            )
                            LocationActionSection(
                                title = "Departure Coordinates",
                                locationEnabled = preferences.useLocation,
                                latitude = startLatitude,
                                longitude = startLongitude,
                                mapLabel = startLocation.ifBlank { "Trip Start" },
                                captureLabel = "Use Current Start Location",
                                onCaptured = { coordinate ->
                                    startLatitude = coordinate.latitude
                                    startLongitude = coordinate.longitude
                                },
                                onCleared = {
                                    startLatitude = null
                                    startLongitude = null
                                },
                                onError = { errorMessage = it },
                            )
                        }
                    }
                }
            }
            item {
                Card {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        PickerDateTimeField(
                            value = endDateText,
                            onValueChange = { endDateText = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = "End (optional)",
                        )
                        NumericEntryField(
                            value = endOdometerText,
                            onValueChange = { endOdometerText = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = when (endOdometerMode) {
                                TripEndOdometerMode.ABSOLUTE -> "End Odometer (optional)"
                                TripEndOdometerMode.DISTANCE_FROM_START -> "Distance From Start (optional)"
                            },
                            decimalEnabled = true,
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            TripEndOdometerMode.entries.forEach { mode ->
                                FilterChip(
                                    selected = endOdometerMode == mode,
                                    onClick = {
                                        if (mode != endOdometerMode) {
                                            endOdometerText = translateTripEndOdometerInput(
                                                rawInput = endOdometerText,
                                                startOdometer = startOdometer,
                                                fromMode = endOdometerMode,
                                                toMode = mode,
                                            )
                                            endOdometerModeName = mode.name
                                        }
                                    },
                                    label = {
                                        Text(
                                            when (mode) {
                                                TripEndOdometerMode.ABSOLUTE -> "Absolute"
                                                TripEndOdometerMode.DISTANCE_FROM_START -> "Trip Distance"
                                            },
                                        )
                                    },
                                )
                            }
                        }
                        if (showTripLocation) {
                            SingleChoiceDialogField(
                                value = endLocation,
                                onValueChange = { endLocation = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = "End Location",
                                choices = locationSuggestions,
                            )
                            LocationActionSection(
                                title = "Arrival Coordinates",
                                locationEnabled = preferences.useLocation,
                                latitude = endLatitude,
                                longitude = endLongitude,
                                mapLabel = endLocation.ifBlank { "Trip End" },
                                captureLabel = "Use Current End Location",
                                onCaptured = { coordinate ->
                                    endLatitude = coordinate.latitude
                                    endLongitude = coordinate.longitude
                                },
                                onCleared = {
                                    endLatitude = null
                                    endLongitude = null
                                },
                                onError = { errorMessage = it },
                            )
                        }
                    }
                }
            }
            item {
                Card {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Trip Type", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        SingleChoiceDialogField(
                            value = selectedTripTypeName,
                            onValueChange = { selectedName ->
                                val type = tripTypes.firstOrNull { it.name.equals(selectedName, ignoreCase = true) }
                                tripTypeId = type?.id
                                if (taxRateText.isBlank() && type?.defaultTaxDeductionRate != null) {
                                    taxRateText = type.defaultTaxDeductionRate.toString()
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = "Type",
                            choices = tripTypes.map { it.name },
                            allowCustomEntry = false,
                        )
                        if (showTripPurpose) {
                            SingleChoiceDialogField(
                                value = purpose,
                                onValueChange = { purpose = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = "Purpose",
                                choices = purposeSuggestions,
                            )
                        }
                        if (showTripClient) {
                            SingleChoiceDialogField(
                                value = client,
                                onValueChange = { client = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = "Client",
                                choices = clientSuggestions,
                            )
                        }
                    }
                }
            }
            item {
                Card {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (showTripTaxDeduction) {
                            NumericEntryField(
                                value = taxRateText,
                                onValueChange = { taxRateText = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = "Tax Deduction Rate",
                                decimalEnabled = true,
                            )
                        }
                        if (showTripReimbursement) {
                            NumericEntryField(
                                value = reimbursementRateText,
                                onValueChange = { reimbursementRateText = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = "Reimbursement Rate",
                                decimalEnabled = true,
                            )
                            ToggleRow("Paid", paid) { paid = it }
                        }
                        if (showTags) {
                            MultiChoiceTagDialogField(
                                selectedTags = selectedTags,
                                suggestions = tagSuggestions,
                                onValueChange = { tags ->
                                    tagsText = tags.joinToString(", ")
                                },
                                modifier = Modifier.fillMaxWidth(),
                                label = "Tags",
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
                        if (endOdometerText.isNotBlank() && resolvedEndOdometer != null) {
                            Text("Resolved End Odometer: ${resolvedEndOdometer.toStableString()} ${preferences.distanceUnit.storageValue}")
                        }
                        Text(
                            "Duration: " + (
                                durationPreview?.let { duration ->
                                    val hours = duration.toHours()
                                    val minutes = duration.toMinutes() - (hours * 60)
                                    "${hours}h ${minutes}m"
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
                    Text(
                        when {
                            isFinishingOpenTrip -> "Finish Trip"
                            recordId > 0L -> "Save Trip"
                            else -> "Add Trip"
                        },
                    )
                }
            }
        }
    }
}
