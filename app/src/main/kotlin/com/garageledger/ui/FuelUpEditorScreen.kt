package com.garageledger.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
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
import com.garageledger.core.model.DistanceUnit
import com.garageledger.core.model.FuelEfficiencyUnit
import com.garageledger.core.model.VolumeUnit
import com.garageledger.data.GarageRepository
import com.garageledger.domain.model.FillUpRecord
import com.garageledger.domain.model.FuelType
import com.garageledger.domain.model.RecordAttachment
import com.garageledger.domain.model.RecordFamily
import com.garageledger.domain.model.toFuelCategoryDisplayName
import java.time.LocalDateTime
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FuelUpEditorScreen(
    repository: GarageRepository,
    vehicleId: Long,
    recordId: Long,
    onBack: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val vehicles by repository.observeVehicles().collectAsStateWithLifecycle(initialValue = emptyList())
    val fuelTypes by repository.observeFuelTypes().collectAsStateWithLifecycle(initialValue = emptyList())
    val preferences by repository.preferences.collectAsStateWithLifecycle(initialValue = com.garageledger.domain.model.AppPreferenceSnapshot())
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
    val vehicleName = selectedVehicle?.name ?: "Fuel-Up"
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
    val showPaymentType = com.garageledger.domain.model.OptionalFieldToggle.PAYMENT_TYPE in visibleFields
    val showFuelType = com.garageledger.domain.model.OptionalFieldToggle.FUEL_TYPE in visibleFields
    val showFuelAdditive = com.garageledger.domain.model.OptionalFieldToggle.FUEL_ADDITIVE in visibleFields
    val showFuelingStation = com.garageledger.domain.model.OptionalFieldToggle.FUELING_STATION in visibleFields
    val showAverageSpeed = com.garageledger.domain.model.OptionalFieldToggle.AVERAGE_SPEED in visibleFields
    val showDrivingMode = com.garageledger.domain.model.OptionalFieldToggle.DRIVING_MODE in visibleFields
    val showDrivingCondition = com.garageledger.domain.model.OptionalFieldToggle.DRIVING_CONDITION in visibleFields
    val showTags = com.garageledger.domain.model.OptionalFieldToggle.TAGS in visibleFields
    val showNotes = com.garageledger.domain.model.OptionalFieldToggle.NOTES in visibleFields
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
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Card {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(vehicleName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("Enter local data first. The third cost field is calculated as soon as two values are known.")
                    }
                }
            }
            item {
                Card {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        PickerDateTimeField(
                            value = dateTimeText,
                            onValueChange = { dateTimeText = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = "Date/Time (yyyy-MM-dd HH:mm)",
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Column(Modifier.weight(1f)) {
                                NumericEntryField(
                                    value = odometerText,
                                    onValueChange = { odometerText = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    label = if (distanceMode) {
                                        "Distance From Previous (${distanceUnit.storageValue})"
                                    } else {
                                        "Odometer (${distanceUnit.storageValue})"
                                    },
                                    decimalEnabled = true,
                                    supportingContent = {
                                        previousFillUp?.let { previous ->
                                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                                Text(
                                                    "Previous odometer: ${previous.odometerReading.toInt()} ${distanceUnit.storageValue}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                )
                                                Text(
                                                    "Last fuel-up: ${previous.dateTime.format(EditorDateFormatter)}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                )
                                            }
                                        }
                                    },
                                )
                            }
                            Column {
                                Text("Delta Mode")
                                Switch(
                                    checked = distanceMode,
                                    onCheckedChange = { checked ->
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
                                    },
                                )
                            }
                        }
                        NumericEntryField(
                            value = volumeText,
                            onValueChange = {
                                volumeText = it
                                val solved = autoCompleteFuelCostFields(priceText, volumeText, totalText, changedField = FuelCostField.VOLUME)
                                priceText = solved.price
                                totalText = solved.total
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = "Volume (${volumeUnit.storageValue})",
                            decimalEnabled = true,
                        )
                        NumericEntryField(
                            value = priceText,
                            onValueChange = {
                                priceText = it
                                val solved = autoCompleteFuelCostFields(priceText, volumeText, totalText, changedField = FuelCostField.PRICE)
                                volumeText = solved.volume
                                totalText = solved.total
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = "Price Per Unit",
                            decimalEnabled = true,
                        )
                        NumericEntryField(
                            value = totalText,
                            onValueChange = {
                                totalText = it
                                val solved = autoCompleteFuelCostFields(priceText, volumeText, totalText, changedField = FuelCostField.TOTAL)
                                priceText = solved.price
                                volumeText = solved.volume
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = "Total Cost",
                            decimalEnabled = true,
                        )
                    }
                }
            }
            item {
                Card {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        ToggleRow("Partial Fill-Up", partial) { partial = it }
                        ToggleRow("Previous Missed Fill-Ups", missed) { missed = it }
                        if (showPaymentType) {
                            SingleChoiceDialogField(
                                value = paymentType,
                                onValueChange = { paymentType = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = "Payment Type",
                                choices = paymentSuggestions,
                            )
                        }
                        if (showFuelType) {
                            if (fuelCategories.isNotEmpty()) {
                                SingleChoiceDialogField(
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
                                    modifier = Modifier.fillMaxWidth(),
                                    label = "Fuel Category",
                                    choices = fuelCategories.map(String::toFuelCategoryDisplayName),
                                    allowCustomEntry = false,
                                )
                            }
                            SingleChoiceDialogField(
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
                                modifier = Modifier.fillMaxWidth(),
                                label = "Fuel Type",
                                choices = filteredFuelTypeSuggestions,
                                allowCustomEntry = false,
                            )
                        }
                        if (showFuelAdditive) {
                            ToggleRow("Fuel Additive", hasFuelAdditive) { hasFuelAdditive = it }
                            if (hasFuelAdditive) {
                                SingleChoiceDialogField(
                                    value = fuelAdditiveName,
                                    onValueChange = { fuelAdditiveName = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    label = "Fuel Additive Name",
                                    choices = additiveSuggestions,
                                )
                            }
                        }
                        if (showFuelingStation) {
                            SingleChoiceDialogField(
                                value = fuelBrand,
                                onValueChange = { fuelBrand = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = "Fuel Brand",
                                choices = brandSuggestions,
                            )
                            SingleChoiceDialogField(
                                value = stationAddress,
                                onValueChange = { stationAddress = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = "Station Address",
                                choices = stationSuggestions,
                            )
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
                        if (showDrivingMode) {
                            SingleChoiceDialogField(
                                value = drivingMode,
                                onValueChange = { drivingMode = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = "Driving Mode",
                                choices = drivingModeSuggestions,
                            )
                        }
                        if (showAverageSpeed) {
                            NumericEntryField(
                                value = averageSpeedText,
                                onValueChange = { averageSpeedText = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = "Average Speed",
                                decimalEnabled = false,
                            )
                        }
                        if (showDrivingCondition) {
                            NumericEntryField(
                                value = cityDrivingText,
                                onValueChange = { cityDrivingText = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = "City Driving Percentage",
                                decimalEnabled = false,
                            )
                            NumericEntryField(
                                value = highwayDrivingText,
                                onValueChange = { highwayDrivingText = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = "Highway Driving Percentage",
                                decimalEnabled = false,
                            )
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
                                    ?: error("Use `yyyy-MM-dd HH:mm` for date and time.")
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

private fun parseCommaValues(raw: String): List<String> = raw
    .split(",")
    .map(String::trim)
    .filter(String::isNotBlank)
    .distinctBy(String::lowercase)
