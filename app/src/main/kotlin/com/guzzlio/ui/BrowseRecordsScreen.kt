package com.guzzlio.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.guzzlio.R
import com.guzzlio.data.GarageRepository
import com.guzzlio.domain.model.AppPreferenceSnapshot
import com.guzzlio.domain.model.BrowseRecordFilter
import com.guzzlio.domain.model.BrowseRecordItem
import com.guzzlio.domain.model.BrowseTripPaidStatus
import com.guzzlio.domain.model.RecordFamily
import java.time.LocalDate
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun BrowseRecordsScreen(
    repository: GarageRepository,
    preselectedVehicleId: Long? = null,
    onBack: () -> Unit,
    onOpenRecord: (BrowseRecordItem) -> Unit,
    onEditRecord: (BrowseRecordItem) -> Unit,
    onCopyTrip: (BrowseRecordItem) -> Unit,
    onFinishTrip: (BrowseRecordItem) -> Unit,
    onOpenStats: (Long?) -> Unit,
) {
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val context = LocalContext.current
    val today = remember { LocalDate.now() }
    val vehicles by repository.observeVehicles().collectAsStateWithLifecycle(initialValue = emptyList())
    val records by repository.observeBrowseRecords().collectAsStateWithLifecycle(initialValue = emptyList())
    val preferences by repository.preferences.collectAsStateWithLifecycle(initialValue = AppPreferenceSnapshot())

    var selectedVehicleId by rememberSaveable(preselectedVehicleId) { mutableLongStateOf(preselectedVehicleId ?: 0L) }
    var selectedFamily by remember { mutableStateOf<RecordFamily?>(null) }
    var advancedFiltersExpanded by rememberSaveable { mutableStateOf(false) }
    var selectedDatePresetName by rememberSaveable { mutableStateOf("") }
    var vehicleMenuExpanded by remember { mutableStateOf(false) }
    var overflowMenuExpanded by remember { mutableStateOf(false) }
    var saveSearchDialogOpen by remember { mutableStateOf(false) }
    var manageSavedSearchesDialogOpen by remember { mutableStateOf(false) }
    var savedSearchNameText by rememberSaveable { mutableStateOf("") }
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
    var actionMenuKey by remember { mutableStateOf<String?>(null) }
    var recordPendingDelete by remember { mutableStateOf<BrowseRecordItem?>(null) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val selectedDatePreset = BrowseDatePreset.entries.firstOrNull { it.name == selectedDatePresetName }

    LaunchedEffect(vehicles, preselectedVehicleId) {
        val preferredVehicleId = preselectedVehicleId
            ?: vehicles.firstOrNull { it.lifecycle == com.guzzlio.domain.model.VehicleLifecycle.ACTIVE }?.id
            ?: vehicles.firstOrNull()?.id
            ?: 0L
        if (vehicles.isNotEmpty() && (selectedVehicleId == 0L || vehicles.none { it.id == selectedVehicleId })) {
            selectedVehicleId = preferredVehicleId
        }
    }

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
    LaunchedEffect(fromDateText, toDateText, today) {
        selectedDatePresetName = resolveBrowseDatePreset(
            fromDate = parseFilterDate(fromDateText),
            toDate = parseFilterDate(toDateText),
            today = today,
        )?.name.orEmpty()
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
    val sortedRecords = remember(filteredRecords, preferences.browseSortDescending) {
        sortBrowseRecords(filteredRecords, preferences.browseSortDescending)
    }
    val savedBrowseSearches = remember(preferences.savedBrowseSearches) {
        preferences.savedBrowseSearches.sortedBy { it.name.lowercase() }
    }
    val selectedVehicleName = remember(vehicles, selectedVehicleId) {
        vehicles.firstOrNull { it.id == selectedVehicleId }?.name
    }
    val activeFilterTokens = remember(fullFilter, selectedVehicleName) {
        buildBrowseFilterTokens(
            filter = fullFilter,
            vehicleName = selectedVehicleName,
        )
    }
    val canSaveCurrentSearch = remember(activeFilterTokens) { activeFilterTokens.isNotEmpty() }

    fun runExport(uri: Uri?) {
        if (uri == null) return
        scope.launch {
            runCatching {
                val stream = context.contentResolver.openOutputStream(uri)
                    ?: error("Unable to create the selected browse export file.")
                stream.use {
                    repository.exportBrowseRecordsCsv(
                        outputStream = it,
                        records = sortedRecords,
                        sortDescending = preferences.browseSortDescending,
                    )
                }
            }.onSuccess {
                statusMessage = "Browse CSV export saved."
                errorMessage = null
            }.onFailure {
                statusMessage = null
                errorMessage = it.message
            }
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        runExport(uri)
    }

    fun clearFilters() {
        selectedVehicleId = preselectedVehicleId ?: 0L
        selectedFamily = null
        selectedDatePresetName = ""
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

    fun applyFilterState(filter: BrowseRecordFilter) {
        selectedVehicleId = filter.vehicleId ?: 0L
        selectedFamily = filter.family
        queryText = filter.query
        tagText = filter.tag
        fromDateText = filter.fromDate?.let(::coerceDateText).orEmpty()
        toDateText = filter.toDate?.let(::coerceDateText).orEmpty()
        subtypeText = filter.subtype
        paymentTypeText = filter.paymentType
        eventPlaceText = filter.eventPlace
        fuelBrandText = filter.fuelBrand
        fuelTypeText = filter.fuelType
        fuelAdditiveText = filter.fuelAdditive
        drivingModeText = filter.drivingMode
        tripPurposeText = filter.tripPurpose
        tripClientText = filter.tripClient
        tripLocationText = filter.tripLocation
        selectedTripPaidStatus = filter.tripPaidStatus
    }

    fun saveCurrentSearch() {
        val trimmedName = savedSearchNameText.trim()
        if (trimmedName.isBlank()) {
            statusMessage = null
            errorMessage = "Saved search name is required."
            return
        }
        val existingSearch = findSavedBrowseSearch(savedBrowseSearches, trimmedName)
        scope.launch {
            runCatching {
                repository.updatePreferences { current ->
                    val search = fullFilter.toSavedBrowseSearch(trimmedName)
                    current.copy(
                        savedBrowseSearches = (current.savedBrowseSearches
                            .filterNot { it.name.equals(trimmedName, ignoreCase = true) } + search)
                            .sortedBy { it.name.lowercase() },
                    )
                }
            }.onSuccess {
                saveSearchDialogOpen = false
                savedSearchNameText = ""
                statusMessage = if (existingSearch == null) {
                    "Saved browse search added."
                } else {
                    "Saved browse search updated."
                }
                errorMessage = null
            }.onFailure {
                statusMessage = null
                errorMessage = it.message
            }
        }
    }

    fun deleteSavedSearch(name: String) {
        scope.launch {
            runCatching {
                repository.updatePreferences { current ->
                    current.copy(
                        savedBrowseSearches = current.savedBrowseSearches
                            .filterNot { it.name.equals(name, ignoreCase = true) },
                    )
                }
            }.onSuccess {
                statusMessage = "Saved browse search deleted."
                errorMessage = null
            }.onFailure {
                statusMessage = null
                errorMessage = it.message
            }
        }
    }

    fun applyDatePreset(preset: BrowseDatePreset?) {
        if (preset == null) {
            selectedDatePresetName = ""
            fromDateText = ""
            toDateText = ""
            return
        }
        val (fromDate, toDate) = browseDatePresetRange(preset, today)
        selectedDatePresetName = preset.name
        fromDateText = coerceDateText(fromDate)
        toDateText = coerceDateText(toDate)
    }

    fun clearActiveFilter(tokenKey: BrowseFilterTokenKey) {
        when (tokenKey) {
            BrowseFilterTokenKey.VEHICLE -> selectedVehicleId = 0L
            BrowseFilterTokenKey.FAMILY -> selectedFamily = null
            BrowseFilterTokenKey.QUERY -> queryText = ""
            BrowseFilterTokenKey.TAG -> tagText = ""
            BrowseFilterTokenKey.FROM_DATE -> {
                fromDateText = ""
                selectedDatePresetName = ""
            }
            BrowseFilterTokenKey.TO_DATE -> {
                toDateText = ""
                selectedDatePresetName = ""
            }
            BrowseFilterTokenKey.SUBTYPE -> subtypeText = ""
            BrowseFilterTokenKey.PAYMENT_TYPE -> paymentTypeText = ""
            BrowseFilterTokenKey.EVENT_PLACE -> eventPlaceText = ""
            BrowseFilterTokenKey.FUEL_BRAND -> fuelBrandText = ""
            BrowseFilterTokenKey.FUEL_TYPE -> fuelTypeText = ""
            BrowseFilterTokenKey.FUEL_ADDITIVE -> fuelAdditiveText = ""
            BrowseFilterTokenKey.DRIVING_MODE -> drivingModeText = ""
            BrowseFilterTokenKey.TRIP_PURPOSE -> tripPurposeText = ""
            BrowseFilterTokenKey.TRIP_CLIENT -> tripClientText = ""
            BrowseFilterTokenKey.TRIP_LOCATION -> tripLocationText = ""
            BrowseFilterTokenKey.TRIP_PAID_STATUS -> selectedTripPaidStatus = null
        }
    }

    if (recordPendingDelete != null) {
        AlertDialog(
            onDismissRequest = { recordPendingDelete = null },
            title = { Text("Delete Record?") },
            text = { Text("This action is permanent and cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        val target = recordPendingDelete ?: return@TextButton
                        scope.launch {
                            runCatching {
                                when (target.family) {
                                    RecordFamily.FILL_UP -> repository.deleteFillUp(target.recordId)
                                    RecordFamily.SERVICE -> repository.deleteService(target.recordId)
                                    RecordFamily.EXPENSE -> repository.deleteExpense(target.recordId)
                                    RecordFamily.TRIP -> repository.deleteTrip(target.recordId)
                                }
                            }.onSuccess {
                                recordPendingDelete = null
                                statusMessage = "Record deleted."
                                errorMessage = null
                            }.onFailure {
                                statusMessage = null
                                errorMessage = it.message
                            }
                        }
                    },
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { recordPendingDelete = null }) {
                    Text("Cancel")
                }
            },
        )
    }

    if (saveSearchDialogOpen) {
        AlertDialog(
            onDismissRequest = {
                saveSearchDialogOpen = false
                savedSearchNameText = ""
            },
            title = { Text("Save Current Search") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Store the current local filter set so you can reload it later.")
                    OutlinedTextField(
                        value = savedSearchNameText,
                        onValueChange = { savedSearchNameText = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Search name") },
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = ::saveCurrentSearch) {
                    Text(
                        if (findSavedBrowseSearch(savedBrowseSearches, savedSearchNameText) == null) {
                            "Save"
                        } else {
                            "Replace"
                        },
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        saveSearchDialogOpen = false
                        savedSearchNameText = ""
                    },
                ) {
                    Text("Cancel")
                }
            },
        )
    }

    if (manageSavedSearchesDialogOpen) {
        AlertDialog(
            onDismissRequest = { manageSavedSearchesDialogOpen = false },
            title = { Text("Saved Searches") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (savedBrowseSearches.isEmpty()) {
                        Text("No saved searches yet.")
                    } else {
                        savedBrowseSearches.forEach { search ->
                            Card {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Text(search.name, fontWeight = FontWeight.SemiBold)
                                    val tokenSummary = buildBrowseFilterTokens(
                                        filter = search.toBrowseRecordFilter(),
                                        vehicleName = vehicles.firstOrNull { it.id == search.vehicleId }?.name,
                                    )
                                    if (tokenSummary.isNotEmpty()) {
                                        Text(
                                            tokenSummary.joinToString(" | ") { it.label },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        TextButton(
                                            onClick = {
                                                applyFilterState(search.toBrowseRecordFilter())
                                                manageSavedSearchesDialogOpen = false
                                                statusMessage = "Saved browse search loaded."
                                                errorMessage = null
                                            },
                                        ) {
                                            Text("Load")
                                        }
                                        TextButton(onClick = { deleteSavedSearch(search.name) }) {
                                            Text("Delete")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { manageSavedSearchesDialogOpen = false }) {
                    Text("Close")
                }
            },
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Browse Records") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } },
                actions = {
                    Box {
                        IconButton(onClick = { overflowMenuExpanded = true }) {
                            Icon(Icons.Outlined.MoreVert, contentDescription = "Browse options")
                        }
                        DropdownMenu(
                            expanded = overflowMenuExpanded,
                            onDismissRequest = { overflowMenuExpanded = false },
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        if (preferences.browseSortDescending) {
                                            "Sort Oldest First"
                                        } else {
                                            "Sort Newest First"
                                        },
                                    )
                                },
                                onClick = {
                                    overflowMenuExpanded = false
                                    scope.launch {
                                        runCatching {
                                            repository.updatePreferences { current ->
                                                current.copy(browseSortDescending = !current.browseSortDescending)
                                            }
                                        }.onSuccess {
                                            statusMessage = "Browse sort updated."
                                            errorMessage = null
                                        }.onFailure {
                                            statusMessage = null
                                            errorMessage = it.message
                                        }
                                    }
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("Save Current Search") },
                                enabled = canSaveCurrentSearch,
                                onClick = {
                                    overflowMenuExpanded = false
                                    savedSearchNameText = ""
                                    saveSearchDialogOpen = true
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("Saved Searches") },
                                enabled = savedBrowseSearches.isNotEmpty(),
                                onClick = {
                                    overflowMenuExpanded = false
                                    manageSavedSearchesDialogOpen = true
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("Export Current Results") },
                                enabled = sortedRecords.isNotEmpty(),
                                onClick = {
                                    overflowMenuExpanded = false
                                    exportLauncher.launch("guzzlio-browse.csv")
                                },
                            )
                        }
                    }
                },
            )
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(
                        onClick = { advancedFiltersExpanded = !advancedFiltersExpanded },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(if (advancedFiltersExpanded) "Hide Search" else "Search / Filter")
                    }
                    TextButton(
                        onClick = { onOpenStats(selectedVehicleId.takeIf { it > 0L }) },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Statistics & Charts")
                    }
                }
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            item {
                Card {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Quick Filters", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Box {
                            TextButton(onClick = { vehicleMenuExpanded = true }) {
                                Text(
                                    selectedVehicleName ?: "All Vehicles",
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
                        Text("Date Range", style = MaterialTheme.typography.labelLarge)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            FilterChip(
                                selected = selectedDatePreset == null && fromDateText.isBlank() && toDateText.isBlank(),
                                onClick = { applyDatePreset(null) },
                                label = { Text("All Time") },
                            )
                            BrowseDatePreset.entries.forEach { preset ->
                                FilterChip(
                                    selected = selectedDatePreset == preset,
                                    onClick = { applyDatePreset(if (selectedDatePreset == preset) null else preset) },
                                    label = { Text(preset.label) },
                                )
                            }
                        }
                        if (savedBrowseSearches.isNotEmpty()) {
                            Text("Saved Searches", style = MaterialTheme.typography.labelLarge)
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                savedBrowseSearches.take(6).forEach { search ->
                                    FilterChip(
                                        selected = false,
                                        onClick = {
                                            applyFilterState(search.toBrowseRecordFilter())
                                            statusMessage = "Saved browse search loaded."
                                            errorMessage = null
                                        },
                                        label = { Text(search.name) },
                                    )
                                }
                            }
                        }
                        if (activeFilterTokens.isNotEmpty()) {
                            Text("Active Filters", style = MaterialTheme.typography.labelLarge)
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                activeFilterTokens.forEach { token ->
                                    FilterChip(
                                        selected = true,
                                        onClick = { clearActiveFilter(token.key) },
                                        label = { Text(token.label) },
                                    )
                                }
                            }
                            Text(
                                "Tap a chip to clear that part of the query.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        TextButton(onClick = { advancedFiltersExpanded = !advancedFiltersExpanded }) {
                            Text(if (advancedFiltersExpanded) "Hide Advanced Builder" else "Show Advanced Builder")
                        }
                        if (advancedFiltersExpanded) {
                            Text(
                                "Build a more exact local query with scoped fields and suggestion chips.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text("Common Fields", style = MaterialTheme.typography.labelLarge)
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
                            PickerDateField(
                                value = fromDateText,
                                onValueChange = {
                                    fromDateText = it
                                    selectedDatePresetName = ""
                                },
                                modifier = Modifier.fillMaxWidth(),
                                label = "From date (yyyy-MM-dd)",
                            )
                            PickerDateField(
                                value = toDateText,
                                onValueChange = {
                                    toDateText = it
                                    selectedDatePresetName = ""
                                },
                                modifier = Modifier.fillMaxWidth(),
                                label = "To date (yyyy-MM-dd)",
                            )
                            if (
                                selectedFamily == null ||
                                selectedFamily == RecordFamily.FILL_UP ||
                                filterOptions.fuelBrandSuggestions.isNotEmpty() ||
                                filterOptions.fuelTypeSuggestions.isNotEmpty() ||
                                filterOptions.fuelAdditiveSuggestions.isNotEmpty() ||
                                filterOptions.drivingModeSuggestions.isNotEmpty()
                            ) {
                                Text("Fuel-Up Fields", style = MaterialTheme.typography.labelLarge)
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
                            }
                            if (
                                selectedFamily == RecordFamily.TRIP ||
                                filterOptions.tripPurposeSuggestions.isNotEmpty() ||
                                filterOptions.tripClientSuggestions.isNotEmpty() ||
                                filterOptions.tripLocationSuggestions.isNotEmpty()
                            ) {
                                Text("Trip Fields", style = MaterialTheme.typography.labelLarge)
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
                        }
                        TextButton(onClick = ::clearFilters) {
                            Text("Clear Filters")
                        }
                        Text(
                            "${sortedRecords.size} matching records | ${browseSortLabel(preferences.browseSortDescending)}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        statusMessage?.let { message ->
                            Text(message, color = MaterialTheme.colorScheme.primary)
                        }
                        errorMessage?.let { message ->
                            Text(message, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
            if (sortedRecords.isEmpty()) {
                item {
                    Card {
                        Text(
                            "No records match the current filters.",
                            modifier = Modifier.padding(18.dp),
                        )
                    }
                }
            } else {
                itemsIndexed(sortedRecords, key = { _, item -> "${item.family.name}:${item.recordId}" }) { index, item ->
                    val actionItems = remember(item) { buildBrowseActionItems(item) }
                    val menuKey = remember(item.family, item.recordId) { "${item.family.name}:${item.recordId}" }
                    BrowseCompactRecordRow(
                        item = item,
                        index = index,
                        preferences = preferences,
                        showVehicleName = selectedVehicleId == 0L,
                        actionMenuExpanded = actionMenuKey == menuKey,
                        onOpenRecord = { onOpenRecord(item) },
                        onMenuOpen = { actionMenuKey = menuKey },
                        onMenuDismiss = { actionMenuKey = null },
                        actionItems = actionItems,
                        onActionSelected = { action ->
                            actionMenuKey = null
                            when (action) {
                                BrowseRecordAction.VIEW -> onOpenRecord(item)
                                BrowseRecordAction.EDIT -> onEditRecord(item)
                                BrowseRecordAction.FINISH_TRIP -> onFinishTrip(item)
                                BrowseRecordAction.COPY_TRIP -> onCopyTrip(item)
                                BrowseRecordAction.DELETE -> recordPendingDelete = item
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun BrowseCompactRecordRow(
    item: BrowseRecordItem,
    index: Int,
    preferences: AppPreferenceSnapshot,
    showVehicleName: Boolean,
    actionMenuExpanded: Boolean,
    onOpenRecord: () -> Unit,
    onMenuOpen: () -> Unit,
    onMenuDismiss: () -> Unit,
    actionItems: List<BrowseRecordActionItem>,
    onActionSelected: (BrowseRecordAction) -> Unit,
) {
    val rowColor = if (index % 2 == 0) {
        MaterialTheme.colorScheme.surface
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = rowColor,
        shape = MaterialTheme.shapes.small,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                painter = painterResource(id = browseRecordIconRes(item.family)),
                contentDescription = item.family.displayLabel(),
                modifier = Modifier.size(22.dp),
                tint = androidx.compose.ui.graphics.Color.Unspecified,
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onOpenRecord() },
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                when (item.family) {
                    RecordFamily.FILL_UP -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            BrowsePrimaryMetric(
                                text = item.fuelVolumeLabel.ifBlank { item.title },
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Start,
                            )
                            BrowsePrimaryMetric(
                                text = item.fuelEfficiencyLabel.ifBlank { "--" },
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Center,
                            )
                            BrowsePrimaryMetric(
                                text = item.fuelPricePerUnitLabel.ifBlank { "--" },
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.End,
                            )
                        }
                        BrowseSecondaryLine(
                            leftText = buildString {
                                item.amount?.let { append(it.asCurrency()) }
                                item.odometerReading?.let {
                                    if (isNotEmpty()) append(" @ ")
                                    append(it.toStableString())
                                }
                                if (showVehicleName && item.vehicleName.isNotBlank()) {
                                    if (isNotEmpty()) append(" | ")
                                    append(item.vehicleName)
                                }
                            },
                            rightText = item.occurredAt.toLocalDate().formatForDisplay(preferences, compact = true),
                        )
                    }

                    RecordFamily.SERVICE, RecordFamily.EXPENSE -> {
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        BrowseSecondaryLine(
                            leftText = buildString {
                                item.amount?.let { append(it.asCurrency()) }
                                item.odometerReading?.let {
                                    if (isNotEmpty()) append(" @ ")
                                    append(it.toStableString())
                                }
                                if (showVehicleName && item.vehicleName.isNotBlank()) {
                                    if (isNotEmpty()) append(" | ")
                                    append(item.vehicleName)
                                }
                            }.ifBlank {
                                item.subtitle.ifBlank {
                                    if (showVehicleName) item.vehicleName else ""
                                }
                            },
                            rightText = item.occurredAt.toLocalDate().formatForDisplay(preferences, compact = true),
                        )
                    }

                    RecordFamily.TRIP -> {
                        val startLocation = item.tripLocations.getOrNull(0).orEmpty().ifBlank { "Trip Start" }
                        val endLocation = item.tripLocations.getOrNull(1).orEmpty().ifBlank { if (item.tripOpen) "Open" else "Trip End" }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(
                                text = startLocation,
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = "->",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = endLocation,
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (item.tripDistanceLabel.isNotBlank()) {
                                Text(
                                    text = item.tripDistanceLabel,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                )
                            }
                        }
                        BrowseSecondaryLine(
                            leftText = listOfNotNull(
                                item.tripPurpose.takeIf { it.isNotBlank() },
                                item.tripClient.takeIf { it.isNotBlank() },
                                item.vehicleName.takeIf { showVehicleName && it.isNotBlank() },
                                "Open Trip".takeIf { item.tripOpen },
                            ).joinToString(" | "),
                            rightText = item.occurredAt.toLocalDate().formatForDisplay(preferences, compact = true),
                        )
                    }
                }
            }
            Box {
                IconButton(
                    onClick = onMenuOpen,
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(Icons.Outlined.MoreVert, contentDescription = "Record actions")
                }
                DropdownMenu(
                    expanded = actionMenuExpanded,
                    onDismissRequest = onMenuDismiss,
                ) {
                    actionItems.forEach { actionItem ->
                        DropdownMenuItem(
                            text = { Text(actionItem.label) },
                            enabled = actionItem.enabled,
                            onClick = { onActionSelected(actionItem.action) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BrowsePrimaryMetric(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign,
) {
    Text(
        text = text,
        modifier = modifier,
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        textAlign = textAlign,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun BrowseSecondaryLine(
    leftText: String,
    rightText: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = leftText,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = rightText,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
    }
}

private fun browseRecordIconRes(family: RecordFamily): Int = when (family) {
    RecordFamily.FILL_UP -> R.drawable.console_action_fuel_up
    RecordFamily.SERVICE -> R.drawable.console_action_service
    RecordFamily.EXPENSE -> R.drawable.console_action_expense
    RecordFamily.TRIP -> R.drawable.console_action_trip
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
