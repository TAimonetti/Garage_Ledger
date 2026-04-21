package com.garageledger.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.garageledger.data.GarageRepository
import com.garageledger.domain.model.BrowseRecordFilter
import com.garageledger.domain.model.BrowseRecordItem
import com.garageledger.domain.model.BrowseTripPaidStatus
import com.garageledger.domain.model.RecordFamily
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun BrowseRecordsScreen(
    repository: GarageRepository,
    preselectedVehicleId: Long? = null,
    onBack: () -> Unit,
    onOpenRecord: (BrowseRecordItem) -> Unit,
) {
    val vehicles by repository.observeVehicles().collectAsStateWithLifecycle(initialValue = emptyList())
    val records by repository.observeBrowseRecords().collectAsStateWithLifecycle(initialValue = emptyList())

    var selectedVehicleId by rememberSaveable { mutableLongStateOf(preselectedVehicleId ?: 0L) }
    var selectedFamily by remember { mutableStateOf<RecordFamily?>(null) }
    var vehicleMenuExpanded by remember { mutableStateOf(false) }
    var queryText by rememberSaveable { mutableStateOf("") }
    var tagText by rememberSaveable { mutableStateOf("") }
    var fromDateText by rememberSaveable { mutableStateOf("") }
    var toDateText by rememberSaveable { mutableStateOf("") }
    var subtypeText by rememberSaveable { mutableStateOf("") }
    var paymentTypeText by rememberSaveable { mutableStateOf("") }
    var eventPlaceText by rememberSaveable { mutableStateOf("") }
    var fuelBrandText by rememberSaveable { mutableStateOf("") }
    var fuelTypeText by rememberSaveable { mutableStateOf("") }
    var fuelAdditiveText by rememberSaveable { mutableStateOf("") }
    var drivingModeText by rememberSaveable { mutableStateOf("") }
    var tripPurposeText by rememberSaveable { mutableStateOf("") }
    var tripClientText by rememberSaveable { mutableStateOf("") }
    var tripLocationText by rememberSaveable { mutableStateOf("") }
    var selectedTripPaidStatus by remember { mutableStateOf<BrowseTripPaidStatus?>(null) }

    LaunchedEffect(selectedFamily) {
        if (selectedFamily != RecordFamily.TRIP) {
            tripPurposeText = ""
            tripClientText = ""
            tripLocationText = ""
            selectedTripPaidStatus = null
        }
        if (selectedFamily != RecordFamily.FILL_UP) {
            fuelBrandText = ""
            fuelTypeText = ""
            fuelAdditiveText = ""
            drivingModeText = ""
        }
    }

    val coreFilter = remember(selectedVehicleId, selectedFamily, queryText, fromDateText, toDateText) {
        BrowseRecordFilter(
            vehicleId = selectedVehicleId.takeIf { it > 0L },
            family = selectedFamily,
            query = queryText,
            fromDate = parseFilterDate(fromDateText),
            toDate = parseFilterDate(toDateText),
        )
    }
    val scopedRecords = remember(records, coreFilter) {
        applyBrowseRecordFilter(records, coreFilter)
    }
    val filterOptions = remember(scopedRecords, selectedFamily) {
        buildBrowseFilterOptions(scopedRecords, selectedFamily)
    }
    val fullFilter = remember(
        coreFilter,
        tagText,
        subtypeText,
        paymentTypeText,
        eventPlaceText,
        fuelBrandText,
        fuelTypeText,
        fuelAdditiveText,
        drivingModeText,
        tripPurposeText,
        tripClientText,
        tripLocationText,
        selectedTripPaidStatus,
    ) {
        coreFilter.copy(
            tag = tagText,
            subtype = subtypeText,
            paymentType = paymentTypeText,
            eventPlace = eventPlaceText,
            fuelBrand = fuelBrandText,
            fuelType = fuelTypeText,
            fuelAdditive = fuelAdditiveText,
            drivingMode = drivingModeText,
            tripPurpose = tripPurposeText,
            tripClient = tripClientText,
            tripLocation = tripLocationText,
            tripPaidStatus = selectedTripPaidStatus,
        )
    }
    val filteredRecords = remember(records, fullFilter) {
        applyBrowseRecordFilter(records, fullFilter)
    }

    fun clearFilters() {
        selectedVehicleId = preselectedVehicleId ?: 0L
        selectedFamily = null
        queryText = ""
        tagText = ""
        fromDateText = ""
        toDateText = ""
        subtypeText = ""
        paymentTypeText = ""
        eventPlaceText = ""
        fuelBrandText = ""
        fuelTypeText = ""
        fuelAdditiveText = ""
        drivingModeText = ""
        tripPurposeText = ""
        tripClientText = ""
        tripLocationText = ""
        selectedTripPaidStatus = null
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Browse Records") },
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
                        Text("Filters", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Box {
                            TextButton(onClick = { vehicleMenuExpanded = true }) {
                                Text(
                                    vehicles.firstOrNull { it.id == selectedVehicleId }?.name ?: "All Vehicles",
                                )
                            }
                            DropdownMenu(
                                expanded = vehicleMenuExpanded,
                                onDismissRequest = { vehicleMenuExpanded = false },
                            ) {
                                DropdownMenuItem(
                                    text = { Text("All Vehicles") },
                                    onClick = {
                                        selectedVehicleId = 0L
                                        vehicleMenuExpanded = false
                                    },
                                )
                                vehicles.forEach { vehicle ->
                                    DropdownMenuItem(
                                        text = { Text(vehicle.name) },
                                        onClick = {
                                            selectedVehicleId = vehicle.id
                                            vehicleMenuExpanded = false
                                        },
                                    )
                                }
                            }
                        }
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            FilterChip(
                                selected = selectedFamily == null,
                                onClick = { selectedFamily = null },
                                label = { Text("All Types") },
                            )
                            RecordFamily.entries.forEach { family ->
                                FilterChip(
                                    selected = selectedFamily == family,
                                    onClick = {
                                        selectedFamily = if (selectedFamily == family) null else family
                                    },
                                    label = { Text(family.displayLabel()) },
                                )
                            }
                        }
                        OutlinedTextField(
                            value = queryText,
                            onValueChange = { queryText = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Search text") },
                        )
                        SuggestionFilterField(
                            value = tagText,
                            onValueChange = { tagText = it },
                            label = "Tag contains",
                            suggestions = filterOptions.tagSuggestions,
                        )
                        if (filterOptions.subtypeSuggestions.isNotEmpty()) {
                            SuggestionFilterField(
                                value = subtypeText,
                                onValueChange = { subtypeText = it },
                                label = "Subtype",
                                suggestions = filterOptions.subtypeSuggestions,
                            )
                        }
                        if (filterOptions.paymentTypeSuggestions.isNotEmpty()) {
                            SuggestionFilterField(
                                value = paymentTypeText,
                                onValueChange = { paymentTypeText = it },
                                label = "Payment Type",
                                suggestions = filterOptions.paymentTypeSuggestions,
                            )
                        }
                        if (filterOptions.eventPlaceSuggestions.isNotEmpty()) {
                            SuggestionFilterField(
                                value = eventPlaceText,
                                onValueChange = { eventPlaceText = it },
                                label = "Event Place",
                                suggestions = filterOptions.eventPlaceSuggestions,
                            )
                        }
                        if (filterOptions.fuelBrandSuggestions.isNotEmpty()) {
                            SuggestionFilterField(
                                value = fuelBrandText,
                                onValueChange = { fuelBrandText = it },
                                label = "Fuel Brand",
                                suggestions = filterOptions.fuelBrandSuggestions,
                            )
                        }
                        if (filterOptions.fuelTypeSuggestions.isNotEmpty()) {
                            SuggestionFilterField(
                                value = fuelTypeText,
                                onValueChange = { fuelTypeText = it },
                                label = "Fuel Type",
                                suggestions = filterOptions.fuelTypeSuggestions,
                            )
                        }
                        if (filterOptions.fuelAdditiveSuggestions.isNotEmpty()) {
                            SuggestionFilterField(
                                value = fuelAdditiveText,
                                onValueChange = { fuelAdditiveText = it },
                                label = "Fuel Additive",
                                suggestions = filterOptions.fuelAdditiveSuggestions,
                            )
                        }
                        if (filterOptions.drivingModeSuggestions.isNotEmpty()) {
                            SuggestionFilterField(
                                value = drivingModeText,
                                onValueChange = { drivingModeText = it },
                                label = "Driving Mode",
                                suggestions = filterOptions.drivingModeSuggestions,
                            )
                        }
                        if (filterOptions.tripPurposeSuggestions.isNotEmpty()) {
                            SuggestionFilterField(
                                value = tripPurposeText,
                                onValueChange = { tripPurposeText = it },
                                label = "Trip Purpose",
                                suggestions = filterOptions.tripPurposeSuggestions,
                            )
                        }
                        if (filterOptions.tripClientSuggestions.isNotEmpty()) {
                            SuggestionFilterField(
                                value = tripClientText,
                                onValueChange = { tripClientText = it },
                                label = "Trip Client",
                                suggestions = filterOptions.tripClientSuggestions,
                            )
                        }
                        if (filterOptions.tripLocationSuggestions.isNotEmpty()) {
                            SuggestionFilterField(
                                value = tripLocationText,
                                onValueChange = { tripLocationText = it },
                                label = "Trip Location",
                                suggestions = filterOptions.tripLocationSuggestions,
                            )
                        }
                        if (selectedFamily == RecordFamily.TRIP || filterOptions.tripLocationSuggestions.isNotEmpty()) {
                            Text("Trip Paid Status", style = MaterialTheme.typography.labelLarge)
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                FilterChip(
                                    selected = selectedTripPaidStatus == null,
                                    onClick = { selectedTripPaidStatus = null },
                                    label = { Text("Any") },
                                )
                                FilterChip(
                                    selected = selectedTripPaidStatus == BrowseTripPaidStatus.PAID,
                                    onClick = {
                                        selectedTripPaidStatus = if (selectedTripPaidStatus == BrowseTripPaidStatus.PAID) null else BrowseTripPaidStatus.PAID
                                    },
                                    label = { Text("Paid") },
                                )
                                FilterChip(
                                    selected = selectedTripPaidStatus == BrowseTripPaidStatus.UNPAID,
                                    onClick = {
                                        selectedTripPaidStatus = if (selectedTripPaidStatus == BrowseTripPaidStatus.UNPAID) null else BrowseTripPaidStatus.UNPAID
                                    },
                                    label = { Text("Unpaid") },
                                )
                            }
                        }
                        OutlinedTextField(
                            value = fromDateText,
                            onValueChange = { fromDateText = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("From date (yyyy-MM-dd)") },
                        )
                        OutlinedTextField(
                            value = toDateText,
                            onValueChange = { toDateText = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("To date (yyyy-MM-dd)") },
                        )
                        TextButton(onClick = ::clearFilters) {
                            Text("Clear Filters")
                        }
                        Text("${filteredRecords.size} matching records", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            if (filteredRecords.isEmpty()) {
                item {
                    Card {
                        Text(
                            "No records match the current filters.",
                            modifier = Modifier.padding(18.dp),
                        )
                    }
                }
            } else {
                items(filteredRecords.size) { index ->
                    val item = filteredRecords[index]
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenRecord(item) },
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(item.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text(
                                "${item.vehicleName} | ${item.family.displayLabel()} | ${
                                    item.occurredAt.format(DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm"))
                                }",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            if (item.subtitle.isNotBlank()) {
                                Text(item.subtitle)
                            }
                            val metadata = buildList {
                                item.odometerReading?.let { add("${it.toStableString()} odometer") }
                                item.amount?.let { add(it.asCurrency()) }
                                if (item.tags.isNotEmpty()) add(item.tags.joinToString(", "))
                            }
                            if (metadata.isNotEmpty()) {
                                Text(metadata.joinToString(" | "), color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SuggestionFilterField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    suggestions: List<String>,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
    )
    SuggestionRow(
        suggestions = suggestions.filter { value.isBlank() || it.contains(value, ignoreCase = true) },
        onSelect = onValueChange,
    )
}
