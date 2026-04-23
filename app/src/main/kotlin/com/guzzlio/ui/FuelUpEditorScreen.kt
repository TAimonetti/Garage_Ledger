package com.guzzlio.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.guzzlio.core.model.DistanceUnit
import com.guzzlio.core.model.FuelEfficiencyUnit
import com.guzzlio.core.model.VolumeUnit
import com.guzzlio.data.GarageRepository
import com.guzzlio.domain.model.FillUpRecord
import com.guzzlio.domain.model.FuelType
import com.guzzlio.domain.model.RecordAttachment
import com.guzzlio.domain.model.RecordFamily
import com.guzzlio.domain.model.toFuelCategoryDisplayName
import java.time.LocalDate
import java.time.LocalDateTime
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FuelUpEditorScreen(
    repository: GarageRepository,
    vehicleId: Long,
    recordId: Long,
    onBack: () -> Unit,
    onOpenFuelTypes: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val vehicles by repository.observeVehicles().collectAsStateWithLifecycle(initialValue = emptyList())
    val fuelTypes by repository.observeFuelTypes().collectAsStateWithLifecycle(initialValue = emptyList())
    val preferences by repository.preferences.collectAsStateWithLifecycle(initialValue = com.guzzlio.domain.model.AppPreferenceSnapshot())
    val fillUps by produceState(initialValue = emptyList<FillUpRecord>(), key1 = vehicleId) {
        value = repository.getVehicleFillUps(vehicleId)
    }
    val existingRecord by produceState<FillUpRecord?>(initialValue = null, key1 = recordId) {
        value = if (recordId > 0) repository.getFillUp(recordId) else null
    }
    val existingAttachments by produceState(initialValue = emptyList<RecordAttachment>(), key1 = recordId) {
        value = if (recordId > 0L) repository.getRecordAttachments(RecordFamily.FILL_UP, recordId) else emptyList()
    }
    val paymentSuggestions by produceState(initialValue = emptyList<String>()) {
        value = repository.getPaymentTypeSuggestions()
    }
    val brandSuggestions by produceState(initialValue = emptyList<String>()) {
        value = repository.getFuelBrandSuggestions()
    }
    val stationSuggestions by produceState(initialValue = emptyList<String>()) {
        value = repository.getFuelStationSuggestions()
    }
    val additiveSuggestions by produceState(initialValue = emptyList<String>()) {
        value = repository.getFuelAdditiveSuggestions()
    }
    val drivingModeSuggestions by produceState(initialValue = emptyList<String>()) {
        value = repository.getDrivingModeSuggestions()
    }
    val tagSuggestions by produceState(initialValue = emptyList<String>()) {
        value = repository.getFillUpTagSuggestions()
    }

    LaunchedEffect(Unit) {
        repository.ensureFuelTypeCatalogChoices()
    }

    var initialized by rememberSaveable(recordId) { mutableStateOf(false) }
    var dateTimeText by rememberSaveable { mutableStateOf(LocalDateTime.now().format(EditorDateFormatter)) }
    var odometerText by rememberSaveable { mutableStateOf("") }
    var distanceMode by rememberSaveable { mutableStateOf(false) }
    var priceText by rememberSaveable { mutableStateOf("") }
    var volumeText by rememberSaveable { mutableStateOf("") }
    var totalText by rememberSaveable { mutableStateOf("") }
    var paymentType by rememberSaveable { mutableStateOf("") }
    var fuelBrand by rememberSaveable { mutableStateOf("") }
    var stationAddress by rememberSaveable { mutableStateOf("") }
    var fuelCategoryText by rememberSaveable { mutableStateOf("") }
    var fuelTypeText by rememberSaveable { mutableStateOf("") }
    var selectedFuelTypeId by rememberSaveable { mutableStateOf<Long?>(null) }
    var hasFuelAdditive by rememberSaveable { mutableStateOf(false) }
    var fuelAdditiveName by rememberSaveable { mutableStateOf("") }
    var drivingMode by rememberSaveable { mutableStateOf("") }
    var averageSpeedText by rememberSaveable { mutableStateOf("") }
    var cityDrivingText by rememberSaveable { mutableStateOf("") }
    var highwayDrivingText by rememberSaveable { mutableStateOf("") }
    var latitude by rememberSaveable { mutableStateOf<Double?>(null) }
    var longitude by rememberSaveable { mutableStateOf<Double?>(null) }
    var partial by rememberSaveable { mutableStateOf(false) }
    var missed by rememberSaveable { mutableStateOf(false) }
    var tagsText by rememberSaveable { mutableStateOf("") }
    var notesText by rememberSaveable { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var attachments by remember(recordId) { mutableStateOf(emptyList<RecordAttachment>()) }
    var attachmentsInitialized by remember(recordId) { mutableStateOf(false) }
    var showCustomizeFields by remember { mutableStateOf(false) }

    LaunchedEffect(existingAttachments) {
        if (attachmentsInitialized) return@LaunchedEffect
        attachments = existingAttachments
        attachmentsInitialized = true
    }

    val editorDateTime = remember(dateTimeText) { parseEditorDateTime(dateTimeText) }
    val previousFillUp = remember(fillUps, editorDateTime, recordId) {
        fillUps
            .filter { it.id != recordId && (editorDateTime == null || it.dateTime < editorDateTime) }
            .maxByOrNull { it.dateTime }
    }
    val latestFillUp = remember(fillUps, recordId) {
        fillUps
            .filter { it.id != recordId }
            .maxByOrNull { it.dateTime }
    }

    val selectedVehicle = vehicles.firstOrNull { it.id == vehicleId }
    val distanceUnit = selectedVehicle?.distanceUnitOverride ?: preferences.distanceUnit
    val volumeUnit = selectedVehicle?.volumeUnitOverride ?: preferences.volumeUnit
    val fuelEfficiencyUnit = selectedVehicle?.fuelEfficiencyUnitOverride ?: preferences.fuelEfficiencyUnit
    val visibleFields = preferences.visibleFields
    val usableFuelTypes = remember(fuelTypes) {
        val structured = fuelTypes.filter(FuelType::hasStructuredChoiceData)
        if (structured.isNotEmpty()) structured else fuelTypes
    }
    val fuelCategories = remember(usableFuelTypes) {
        usableFuelTypes
            .map { it.category.trim() }
            .filter { it.isNotBlank() }
            .distinctBy { it.lowercase() }
    }
    val selectedFuelCategory = remember(fuelCategoryText, fuelCategories) {
        fuelCategories.firstOrNull { category ->
            category.equals(fuelCategoryText, ignoreCase = true) ||
                category.toFuelCategoryDisplayName().equals(fuelCategoryText, ignoreCase = true)
        }
    }
    val filteredFuelTypes = remember(usableFuelTypes, selectedFuelCategory) {
        usableFuelTypes.filter { selectedFuelCategory == null || it.category.equals(selectedFuelCategory, ignoreCase = true) }
    }
    val filteredFuelTypeSuggestions = remember(filteredFuelTypes) { filteredFuelTypes.map(FuelType::displayName) }
    val showPaymentType = com.guzzlio.domain.model.OptionalFieldToggle.PAYMENT_TYPE in visibleFields
    val showFuelType = com.guzzlio.domain.model.OptionalFieldToggle.FUEL_TYPE in visibleFields
    val showFuelAdditive = com.guzzlio.domain.model.OptionalFieldToggle.FUEL_ADDITIVE in visibleFields
    val showFuelingStation = com.guzzlio.domain.model.OptionalFieldToggle.FUELING_STATION in visibleFields
    val showAverageSpeed = com.guzzlio.domain.model.OptionalFieldToggle.AVERAGE_SPEED in visibleFields
    val showDrivingMode = com.guzzlio.domain.model.OptionalFieldToggle.DRIVING_MODE in visibleFields
    val showDrivingCondition = com.guzzlio.domain.model.OptionalFieldToggle.DRIVING_CONDITION in visibleFields
    val showTags = com.guzzlio.domain.model.OptionalFieldToggle.TAGS in visibleFields
    val showNotes = com.guzzlio.domain.model.OptionalFieldToggle.NOTES in visibleFields
    val selectedTags = remember(tagsText) { parseCommaValues(tagsText) }

    LaunchedEffect(recordId, existingRecord, latestFillUp) {
        if (initialized) return@LaunchedEffect
        if (recordId > 0L) {
            val record = existingRecord ?: return@LaunchedEffect
            dateTimeText = record.dateTime.format(EditorDateFormatter)
            odometerText = record.odometerReading.toString()
            priceText = record.pricePerUnit.toString()
            volumeText = record.volume.toString()
            totalText = record.totalCost.toString()
            paymentType = record.paymentType
            fuelBrand = record.fuelBrand
            stationAddress = record.stationAddress
            fuelTypeText = record.importedFuelTypeText.orEmpty()
            selectedFuelTypeId = record.fuelTypeId
            hasFuelAdditive = record.hasFuelAdditive
            fuelAdditiveName = record.fuelAdditiveName
            drivingMode = record.drivingMode
            averageSpeedText = record.averageSpeed?.toString().orEmpty()
            cityDrivingText = record.cityDrivingPercentage?.toString().orEmpty()
            highwayDrivingText = record.highwayDrivingPercentage?.toString().orEmpty()
            latitude = record.latitude
            longitude = record.longitude
            partial = record.partial
            missed = record.previousMissedFillups
            tagsText = record.tags.joinToString(", ")
            notesText = record.notes
        } else {
            latestFillUp?.let { last ->
                paymentType = last.paymentType
                fuelBrand = last.fuelBrand
                stationAddress = last.stationAddress
                fuelTypeText = last.importedFuelTypeText.orEmpty()
                selectedFuelTypeId = last.fuelTypeId
                hasFuelAdditive = last.hasFuelAdditive
                fuelAdditiveName = last.fuelAdditiveName
                drivingMode = last.drivingMode.ifBlank { "Normal" }
                cityDrivingText = last.cityDrivingPercentage?.toString() ?: "50"
                highwayDrivingText = last.highwayDrivingPercentage?.toString() ?: "50"
                latitude = last.latitude.takeIf { preferences.useLocation }
                longitude = last.longitude.takeIf { preferences.useLocation }
            }
        }
        initialized = true
    }

    LaunchedEffect(usableFuelTypes, selectedFuelTypeId) {
        val selectedFuelType = usableFuelTypes.firstOrNull { it.id == selectedFuelTypeId }
        if (selectedFuelType != null) {
            fuelCategoryText = selectedFuelType.category
            fuelTypeText = selectedFuelType.displayName
        }
    }

    if (showCustomizeFields) {
        VisibleFieldsDialog(
            title = "Customize Fuel-Up Screen",
            options = FuelUpVisibleFieldOptions,
            visibleFields = visibleFields,
            onToggle = { toggle, visible ->
                scope.launch { repository.setVisibleField(toggle, visible) }
            },
            onDismiss = { showCustomizeFields = false },
        )
    }

    val updateDistanceMode: (Boolean) -> Unit = { checked ->
        distanceMode = checked
        odometerText = if (checked) {
            previousFillUp?.let { previous ->
                val currentAbsolute = odometerText.toDoubleOrNull()
                currentAbsolute?.minus(previous.odometerReading)?.takeIf { it >= 0 }?.toString()
            } ?: odometerText
        } else {
            previousFillUp?.let { previous ->
                val delta = odometerText.toDoubleOrNull()
                delta?.plus(previous.odometerReading)?.toString()
            } ?: odometerText
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(if (recordId > 0) "Edit Fuel-Up" else "New Fuel-Up") },
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
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    FuelUpNumericDialogRow(
                        label = "Odometer",
                        displayLabel = if (distanceMode) "Distance" else "Odometer",
                        value = odometerText,
                        onValueChange = { odometerText = it },
                        decimalEnabled = true,
                        placeholder = distanceUnit.storageValue,
                        valueSuffix = distanceUnit.storageValue,
                        modeButtonLabel = if (distanceMode) "ABS" else "DIST",
                        onModeButtonClick = { updateDistanceMode(!distanceMode) },
                    )
                    previousFillUp?.let { previous ->
                        FuelUpReadoutRow(
                            label = "Previous Odometer",
                            value = "${previous.odometerReading.toStableString()} ${distanceUnit.storageValue}",
                        )
                    }
                    FuelUpNumericDialogRow(
                        label = "Price",
                        value = priceText,
                        onValueChange = {
                            priceText = it
                            val solved = autoCompleteFuelCostFields(priceText, volumeText, totalText, FuelCostField.PRICE)
                            volumeText = solved.volume
                            totalText = solved.total
                        },
                        decimalEnabled = true,
                    )
                    FuelUpNumericDialogRow(
                        label = "Volume",
                        value = volumeText,
                        onValueChange = {
                            volumeText = it
                            val solved = autoCompleteFuelCostFields(priceText, volumeText, totalText, FuelCostField.VOLUME)
                            priceText = solved.price
                            totalText = solved.total
                        },
                        decimalEnabled = true,
                        valueSuffix = volumeUnit.storageValue,
                    )
                    FuelUpNumericDialogRow(
                        label = "Total Cost",
                        value = totalText,
                        onValueChange = {
                            totalText = it
                            val solved = autoCompleteFuelCostFields(priceText, volumeText, totalText, FuelCostField.TOTAL)
                            priceText = solved.price
                            volumeText = solved.volume
                        },
                        decimalEnabled = true,
                    )
                    FuelUpCheckboxRow(
                        label = "Partial Fill-Up",
                        checked = partial,
                        onCheckedChange = { partial = it },
                    )
                    FuelUpCheckboxRow(
                        label = "Missed Fill-Ups",
                        checked = missed,
                        onCheckedChange = { missed = it },
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))
                    FuelUpDateTimeChooserRow(
                        value = dateTimeText,
                        onValueChange = { dateTimeText = it },
                    )
                    if (showPaymentType) {
                        FuelUpChoiceDialogRow(
                            label = "Payment Type",
                            value = paymentType,
                            onValueChange = { paymentType = it },
                            choices = paymentSuggestions,
                        )
                    }
                }
            }
            if (showFuelType) {
                item { FuelUpSectionHeader("Fuel Information") }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (fuelCategories.isNotEmpty()) {
                            FuelUpChoiceDialogRow(
                                label = "Fuel Category",
                                value = fuelCategoryText.takeIf { it.isNotBlank() }?.toFuelCategoryDisplayName().orEmpty(),
                                onValueChange = { selectedDisplay ->
                                    val selectedCategory = fuelCategories.firstOrNull {
                                        it.toFuelCategoryDisplayName().equals(selectedDisplay, ignoreCase = true)
                                    } ?: selectedDisplay
                                    fuelCategoryText = selectedCategory
                                    val selectedType = usableFuelTypes.firstOrNull { it.id == selectedFuelTypeId }
                                    if (selectedType != null && !selectedType.category.equals(selectedCategory, ignoreCase = true)) {
                                        selectedFuelTypeId = null
                                        fuelTypeText = ""
                                    }
                                },
                                choices = fuelCategories.map(String::toFuelCategoryDisplayName),
                                allowCustomEntry = false,
                                extraActionLabel = "Manage Fuel Types",
                                onExtraAction = onOpenFuelTypes,
                            )
                        }
                        FuelUpChoiceDialogRow(
                            label = "Fuel Type",
                            value = fuelTypeText,
                            onValueChange = { selected ->
                                fuelTypeText = selected
                                val fuelType = filteredFuelTypes.firstOrNull {
                                    it.displayName.equals(selected, ignoreCase = true)
                                }
                                selectedFuelTypeId = fuelType?.id
                                if (fuelType != null) {
                                    fuelCategoryText = fuelType.category
                                }
                            },
                            choices = filteredFuelTypeSuggestions,
                            allowCustomEntry = false,
                            extraActionLabel = "Manage Fuel Types",
                            onExtraAction = onOpenFuelTypes,
                        )
                        if (showFuelAdditive) {
                            FuelUpCheckboxRow(
                                label = "Fuel Additive",
                                checked = hasFuelAdditive,
                                onCheckedChange = { hasFuelAdditive = it },
                            )
                            if (hasFuelAdditive) {
                                FuelUpChoiceDialogRow(
                                    label = "Additive Name",
                                    value = fuelAdditiveName,
                                    onValueChange = { fuelAdditiveName = it },
                                    choices = additiveSuggestions,
                                )
                            }
                        }
                    }
                }
            }
            if (showFuelingStation) {
                item { FuelUpSectionHeader("Fueling Station") }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        FuelUpChoiceDialogRow(
                            label = "Fuel Brand",
                            value = fuelBrand,
                            onValueChange = { fuelBrand = it },
                            choices = brandSuggestions,
                        )
                        FuelUpChoiceDialogRow(
                            label = "Station Address",
                            value = stationAddress,
                            onValueChange = { stationAddress = it },
                            choices = stationSuggestions,
                        )
                    }
                }
                item {
                    LocationActionSection(
                        title = "Fueling Coordinates",
                        locationEnabled = preferences.useLocation,
                        latitude = latitude,
                        longitude = longitude,
                        mapLabel = stationAddress.ifBlank { fuelBrand.ifBlank { "Fuel-Up" } },
                        captureLabel = "Use Current Location",
                        onCaptured = { coordinate ->
                            latitude = coordinate.latitude
                            longitude = coordinate.longitude
                        },
                        onCleared = {
                            latitude = null
                            longitude = null
                        },
                        onError = { errorMessage = it },
                    )
                }
            }
            if (showDrivingMode || showAverageSpeed || showDrivingCondition) {
                item { FuelUpSectionHeader("Driving Details") }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (showDrivingMode) {
                            FuelUpChoiceDialogRow(
                                label = "Driving Mode",
                                value = drivingMode,
                                onValueChange = { drivingMode = it },
                                choices = drivingModeSuggestions,
                            )
                        }
                        if (showAverageSpeed) {
                            FuelUpNumericDialogRow(
                                label = "Average Speed",
                                value = averageSpeedText,
                                onValueChange = { averageSpeedText = it },
                                decimalEnabled = false,
                            )
                        }
                        if (showDrivingCondition) {
                            FuelUpNumericDialogRow(
                                label = "City Driving",
                                value = cityDrivingText,
                                onValueChange = { cityDrivingText = it },
                                decimalEnabled = false,
                                valueSuffix = "%",
                            )
                            FuelUpNumericDialogRow(
                                label = "Highway Driving",
                                value = highwayDrivingText,
                                onValueChange = { highwayDrivingText = it },
                                decimalEnabled = false,
                                valueSuffix = "%",
                            )
                        }
                    }
                }
            }
            if (showTags || showNotes) {
                item { FuelUpSectionHeader("Notes and Tags") }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (showTags) {
                            FuelUpTagsRow(
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
            }
            item { FuelUpSectionHeader("Attachments") }
            item {
                AttachmentEditorCard(
                    vehicleId = vehicleId,
                    recordFamily = RecordFamily.FILL_UP,
                    attachments = attachments,
                    onAttachmentsChange = { attachments = it },
                    onError = { errorMessage = it },
                )
            }
            errorMessage?.let { message ->
                item { Text(message, color = MaterialTheme.colorScheme.error) }
            }
            item {
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        scope.launch {
                            runCatching {
                                val parsedDateTime = parseEditorDateTime(dateTimeText)
                                    ?: error("Use yyyy-MM-dd HH:mm for date and time.")
                                val absoluteOdometer = when {
                                    distanceMode && previousFillUp != null -> previousFillUp.odometerReading + (odometerText.toDoubleOrNull()
                                        ?: error("Enter a valid distance."))
                                    else -> odometerText.toDoubleOrNull() ?: error("Enter a valid odometer.")
                                }
                                val solved = autoCompleteFuelCostFields(priceText, volumeText, totalText, changedField = null)
                                val price = solved.price.toDoubleOrNull() ?: error("Enter at least two of price, volume, and total cost.")
                                val volume = solved.volume.toDoubleOrNull() ?: error("Enter at least two of price, volume, and total cost.")
                                val total = solved.total.toDoubleOrNull() ?: error("Enter at least two of price, volume, and total cost.")

                                val savedId = repository.saveFillUp(
                                    FillUpRecord(
                                        id = existingRecord?.id ?: 0L,
                                        legacySourceId = existingRecord?.legacySourceId,
                                        vehicleId = vehicleId,
                                        dateTime = parsedDateTime,
                                        odometerReading = absoluteOdometer,
                                        distanceUnit = distanceUnit,
                                        volume = volume,
                                        volumeUnit = volumeUnit,
                                        pricePerUnit = price,
                                        totalCost = total,
                                        paymentType = paymentType,
                                        partial = partial,
                                        previousMissedFillups = missed,
                                        fuelEfficiencyUnit = fuelEfficiencyUnit,
                                        fuelTypeId = selectedFuelTypeId,
                                        importedFuelTypeText = fuelTypeText.takeIf { selectedFuelTypeId == null && it.isNotBlank() },
                                        hasFuelAdditive = hasFuelAdditive,
                                        fuelAdditiveName = fuelAdditiveName,
                                        fuelBrand = fuelBrand,
                                        stationAddress = stationAddress,
                                        latitude = latitude,
                                        longitude = longitude,
                                        drivingMode = drivingMode,
                                        cityDrivingPercentage = cityDrivingText.toIntOrNull(),
                                        highwayDrivingPercentage = highwayDrivingText.toIntOrNull(),
                                        averageSpeed = averageSpeedText.toDoubleOrNull(),
                                        tags = parseCommaValues(tagsText),
                                        notes = notesText,
                                    ),
                                )
                                repository.replaceRecordAttachments(
                                    vehicleId = vehicleId,
                                    recordFamily = RecordFamily.FILL_UP,
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
                    Text(if (recordId > 0) "Save Fuel-Up" else "Add Fuel-Up")
                }
            }
        }
    }
}

@Composable
private fun FuelUpSectionHeader(title: String) {
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
private fun FuelUpFormRow(
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
private fun RowScope.FuelUpFieldButton(
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
private fun FuelUpMiniActionButton(label: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = MaterialTheme.shapes.small,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp),
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun FuelUpReadoutRow(label: String, value: String) {
    FuelUpFormRow(label = label) {
        Text(
            text = value,
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp, lineHeight = 20.sp),
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun FuelUpCheckboxRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    FuelUpFormRow(label = label) {
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
private fun FuelUpNumericDialogRow(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    decimalEnabled: Boolean,
    displayLabel: String = label,
    placeholder: String = "Tap to enter",
    valueSuffix: String? = null,
    modeButtonLabel: String? = null,
    onModeButtonClick: (() -> Unit)? = null,
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
    FuelUpFormRow(label = displayLabel) {
        FuelUpFieldButton(
            text = displayValue,
            onClick = { showDialog = true },
            modifier = Modifier.weight(1f),
            placeholder = placeholder,
            emphasizeValue = value.isNotBlank(),
            alignEnd = true,
        )
        if (modeButtonLabel != null && onModeButtonClick != null) {
            FuelUpMiniActionButton(label = modeButtonLabel, onClick = onModeButtonClick)
        }
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
private fun FuelUpChoiceDialogRow(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    choices: List<String>,
    allowCustomEntry: Boolean = true,
    allowClear: Boolean = true,
    extraActionLabel: String? = null,
    onExtraAction: (() -> Unit)? = null,
) {
    var showDialog by rememberSaveable(label) { mutableStateOf(false) }
    FuelUpFormRow(label = label) {
        FuelUpFieldButton(
            text = value,
            onClick = { showDialog = true },
            modifier = Modifier.weight(1f),
            placeholder = "Tap to choose",
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
private fun FuelUpTagsRow(
    selectedTags: List<String>,
    suggestions: List<String>,
    onValueChange: (List<String>) -> Unit,
) {
    var showDialog by rememberSaveable { mutableStateOf(false) }
    FuelUpFormRow(label = "Tags") {
        FuelUpFieldButton(
            text = selectedTags.joinToString(", "),
            onClick = { showDialog = true },
            modifier = Modifier.weight(1f),
            placeholder = "Tap to choose",
            emphasizeValue = selectedTags.isNotEmpty(),
        )
    }
    if (showDialog) {
        MultiChoiceTagDialog(
            title = "Tags",
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
private fun FuelUpDateTimeChooserRow(
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
    FuelUpFormRow(label = "Date/Time") {
        FuelUpFieldButton(
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

private data class FuelCostForm(
    val price: String,
    val volume: String,
    val total: String,
)

private enum class FuelCostField {
    PRICE,
    VOLUME,
    TOTAL,
}

private fun autoCompleteFuelCostFields(
    price: String,
    volume: String,
    total: String,
    changedField: FuelCostField?,
): FuelCostForm {
    val priceValue = price.toDoubleOrNull()
    val volumeValue = volume.toDoubleOrNull()
    val totalValue = total.toDoubleOrNull()
    return when {
        changedField != FuelCostField.TOTAL && priceValue != null && volumeValue != null && volumeValue != 0.0 ->
            FuelCostForm(price = price, volume = volume, total = (priceValue * volumeValue).toStableString())
        changedField != FuelCostField.VOLUME && priceValue != null && totalValue != null && priceValue != 0.0 ->
            FuelCostForm(price = price, volume = (totalValue / priceValue).toStableString(), total = total)
        changedField != FuelCostField.PRICE && volumeValue != null && totalValue != null && volumeValue != 0.0 ->
            FuelCostForm(price = (totalValue / volumeValue).toStableString(), volume = volume, total = total)
        else -> FuelCostForm(price = price, volume = volume, total = total)
    }
}

