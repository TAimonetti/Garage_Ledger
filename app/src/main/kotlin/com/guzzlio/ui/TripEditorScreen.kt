package com.guzzlio.ui

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
import com.guzzlio.data.GarageRepository
import com.guzzlio.domain.calc.TripCostBreakdown
import com.guzzlio.domain.model.RecordAttachment
import com.guzzlio.domain.model.RecordFamily
import com.guzzlio.domain.model.TripRecord
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TripEditorScreen(
    repository: GarageRepository,
    vehicleId: Long,
    recordId: Long,
    copyFromTripId: Long? = null,
    finishMode: Boolean = false,
    onOpenTripTypes: () -> Unit,
    onBack: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val vehicles by repository.observeVehicles().collectAsStateWithLifecycle(initialValue = emptyList())
    val preferences by repository.preferences.collectAsStateWithLifecycle(initialValue = com.guzzlio.domain.model.AppPreferenceSnapshot())
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
    val tripTypes by repository.observeTripTypes().collectAsStateWithLifecycle(initialValue = emptyList())
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

    LaunchedEffect(Unit) {
        repository.ensureTripTypeCatalogChoices()
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

    val isFinishingOpenTrip = finishMode && existingRecord?.let { it.endDateTime == null || it.endOdometerReading == null } == true
    val screenTitle = when {
        isFinishingOpenTrip -> "Finish Trip"
        recordId > 0L -> "Edit Trip"
        copySourceTrip != null -> "Copy Trip"
        else -> "New Trip"
    }
    val visibleFields = preferences.visibleFields
    val showTripLocation = com.guzzlio.domain.model.OptionalFieldToggle.TRIP_LOCATION in visibleFields
    val showTripPurpose = com.guzzlio.domain.model.OptionalFieldToggle.TRIP_PURPOSE in visibleFields
    val showTripClient = com.guzzlio.domain.model.OptionalFieldToggle.TRIP_CLIENT in visibleFields
    val showTripTaxDeduction = com.guzzlio.domain.model.OptionalFieldToggle.TRIP_TAX_DEDUCTION in visibleFields
    val showTripReimbursement = com.guzzlio.domain.model.OptionalFieldToggle.TRIP_REIMBURSEMENT in visibleFields
    val showTags = com.guzzlio.domain.model.OptionalFieldToggle.TAGS in visibleFields
    val showNotes = com.guzzlio.domain.model.OptionalFieldToggle.NOTES in visibleFields
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
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val sourceTrip = copySourceTrip
                    if (sourceTrip != null && recordId <= 0L) {
                        Text(
                            "Copied from ${sourceTrip.startDateTime.format(EditorDateFormatter)}. Start odometer and end-trip values were reset.",
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
                            "An open trip from ${openTrip.startDateTime.format(EditorDateFormatter)} is still active.",
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
                            ) { Text("From Last Destination") }
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
                            ) { Text("Return Last Trip") }
                        }
                    }
                }
            }
            item {
                CompactFormSectionHeader("Trip Start")
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    CompactDateTimeChooserRow(
                        label = "Start",
                        value = startDateText,
                        onValueChange = { startDateText = it },
                    )
                    CompactNumericDialogRow(
                        label = "Start Odometer",
                        value = startOdometerText,
                        onValueChange = { startOdometerText = it },
                        decimalEnabled = true,
                        valueSuffix = preferences.distanceUnit.storageValue,
                    )
                    suggestedOdometer?.let { suggested ->
                        CompactFormReadoutRow(
                            label = "Suggested",
                            value = "${suggested.toStableString()} ${preferences.distanceUnit.storageValue}",
                        )
                    }
                    if (showTripLocation) {
                        CompactChoiceDialogRow(
                            label = "Start Location",
                            value = startLocation,
                            onValueChange = { startLocation = it },
                            choices = locationSuggestions,
                        )
                    }
                }
            }
            if (showTripLocation) {
                item {
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
            item {
                CompactFormSectionHeader("Trip End")
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    CompactDateTimeChooserRow(
                        label = "End",
                        value = endDateText,
                        onValueChange = { endDateText = it },
                    )
                    CompactNumericDialogRow(
                        label = if (endOdometerMode == TripEndOdometerMode.ABSOLUTE) {
                            "End Odometer"
                        } else {
                            "Trip Distance"
                        },
                        value = endOdometerText,
                        onValueChange = { endOdometerText = it },
                        decimalEnabled = true,
                        valueSuffix = preferences.distanceUnit.storageValue,
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
                        CompactChoiceDialogRow(
                            label = "End Location",
                            value = endLocation,
                            onValueChange = { endLocation = it },
                            choices = locationSuggestions,
                        )
                    }
                }
            }
            if (showTripLocation) {
                item {
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
            item {
                CompactFormSectionHeader("Trip Details")
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    CompactChoiceDialogRow(
                        label = "Trip Type",
                        value = selectedTripTypeName,
                        onValueChange = { selectedName ->
                            val type = tripTypes.firstOrNull { it.name.equals(selectedName, ignoreCase = true) }
                            tripTypeId = type?.id
                            if (taxRateText.isBlank() && type?.defaultTaxDeductionRate != null) {
                                taxRateText = type.defaultTaxDeductionRate.toString()
                            }
                        },
                        choices = tripTypes.map { it.name },
                        allowCustomEntry = false,
                        extraActionLabel = "Manage Trip Types",
                        onExtraAction = onOpenTripTypes,
                    )
                    if (showTripPurpose) {
                        CompactChoiceDialogRow(
                            label = "Purpose",
                            value = purpose,
                            onValueChange = { purpose = it },
                            choices = purposeSuggestions,
                        )
                    }
                    if (showTripClient) {
                        CompactChoiceDialogRow(
                            label = "Client",
                            value = client,
                            onValueChange = { client = it },
                            choices = clientSuggestions,
                        )
                    }
                }
            }
            item {
                CompactFormSectionHeader("Rates and Notes")
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (showTripTaxDeduction) {
                        CompactNumericDialogRow(
                            label = "Tax Rate",
                            value = taxRateText,
                            onValueChange = { taxRateText = it },
                            decimalEnabled = true,
                        )
                    }
                    if (showTripReimbursement) {
                        CompactNumericDialogRow(
                            label = "Reimbursement",
                            value = reimbursementRateText,
                            onValueChange = { reimbursementRateText = it },
                            decimalEnabled = true,
                        )
                        CompactFormCheckboxRow(
                            label = "Paid",
                            checked = paid,
                            onCheckedChange = { paid = it },
                        )
                    }
                    if (showTags) {
                        CompactTagsRow(
                            selectedTags = selectedTags,
                            suggestions = tagSuggestions,
                            onValueChange = { tags -> tagsText = tags.joinToString(", ") },
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
            item { CompactFormSectionHeader("Attachments") }
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
                CompactFormSectionHeader("Trip Preview")
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    CompactFormReadoutRow(
                        label = "Distance",
                        value = distancePreview?.let { "${it.toStableString()} ${preferences.distanceUnit.storageValue}" } ?: "Open trip",
                    )
                    if (endOdometerText.isNotBlank() && resolvedEndOdometer != null) {
                        CompactFormReadoutRow(
                            label = "Resolved End",
                            value = "${resolvedEndOdometer.toStableString()} ${preferences.distanceUnit.storageValue}",
                        )
                    }
                    CompactFormReadoutRow(
                        label = "Duration",
                        value = durationPreview?.let { duration ->
                            val hours = duration.toHours()
                            val minutes = duration.toMinutes() - (hours * 60)
                            "${hours}h ${minutes}m"
                        } ?: "Open trip",
                    )
                    if (showTripTaxDeduction) taxRateText.toDoubleOrNull()?.let { rate ->
                        distancePreview?.let { distance ->
                            CompactFormReadoutRow(
                                label = "Tax Deduction",
                                value = (rate * distance).asCurrency(),
                            )
                        }
                    }
                    if (showTripReimbursement) reimbursementRateText.toDoubleOrNull()?.let { rate ->
                        distancePreview?.let { distance ->
                            CompactFormReadoutRow(
                                label = "Reimbursement",
                                value = (rate * distance).asCurrency(),
                            )
                        }
                    }
                    estimatedCost?.let { estimate ->
                        CompactFormReadoutRow(label = "Direct Expenses", value = estimate.directExpenseCost.asCurrency())
                        CompactFormReadoutRow(label = "Fuel Estimate", value = estimate.estimatedFuelCost.asCurrency())
                        CompactFormReadoutRow(label = "Service Estimate", value = estimate.estimatedServiceCost.asCurrency())
                        CompactFormReadoutRow(label = "Estimated Total", value = estimate.totalCost.asCurrency())
                    }
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
