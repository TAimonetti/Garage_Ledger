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
import com.garageledger.domain.model.RecordAttachment
import com.garageledger.domain.model.RecordFamily
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
    var fuelTypeText by rememberSaveable { mutableStateOf("") }
    var hasFuelAdditive by rememberSaveable { mutableStateOf(false) }
    var fuelAdditiveName by rememberSaveable { mutableStateOf("") }
    var drivingMode by rememberSaveable { mutableStateOf("") }
    var averageSpeedText by rememberSaveable { mutableStateOf("") }
    var cityDrivingText by rememberSaveable { mutableStateOf("") }
    var highwayDrivingText by rememberSaveable { mutableStateOf("") }
    var partial by rememberSaveable { mutableStateOf(false) }
    var missed by rememberSaveable { mutableStateOf(false) }
    var tagsText by rememberSaveable { mutableStateOf("") }
    var notesText by rememberSaveable { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var attachments by remember(recordId) { mutableStateOf(emptyList<RecordAttachment>()) }
    var attachmentsInitialized by remember(recordId) { mutableStateOf(false) }
    var showCustomizeFields by remember { mutableStateOf(false) }

    LaunchedEffect(existingRecord) {
        if (initialized) return@LaunchedEffect
        existingRecord?.let { record ->
            dateTimeText = record.dateTime.format(EditorDateFormatter)
            odometerText = record.odometerReading.toString()
            priceText = record.pricePerUnit.toString()
            volumeText = record.volume.toString()
            totalText = record.totalCost.toString()
            paymentType = record.paymentType
            fuelBrand = record.fuelBrand
            stationAddress = record.stationAddress
            fuelTypeText = record.importedFuelTypeText.orEmpty()
            hasFuelAdditive = record.hasFuelAdditive
            fuelAdditiveName = record.fuelAdditiveName
            drivingMode = record.drivingMode
            averageSpeedText = record.averageSpeed?.toString().orEmpty()
            cityDrivingText = record.cityDrivingPercentage?.toString().orEmpty()
            highwayDrivingText = record.highwayDrivingPercentage?.toString().orEmpty()
            partial = record.partial
            missed = record.previousMissedFillups
            tagsText = record.tags.joinToString(", ")
            notesText = record.notes
        }
        initialized = true
    }

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

    val vehicleName = vehicles.firstOrNull { it.id == vehicleId }?.name ?: "Fuel-Up"
    val visibleFields = preferences.visibleFields
    val showPaymentType = com.garageledger.domain.model.OptionalFieldToggle.PAYMENT_TYPE in visibleFields
    val showFuelType = com.garageledger.domain.model.OptionalFieldToggle.FUEL_TYPE in visibleFields
    val showFuelAdditive = com.garageledger.domain.model.OptionalFieldToggle.FUEL_ADDITIVE in visibleFields
    val showFuelingStation = com.garageledger.domain.model.OptionalFieldToggle.FUELING_STATION in visibleFields
    val showAverageSpeed = com.garageledger.domain.model.OptionalFieldToggle.AVERAGE_SPEED in visibleFields
    val showDrivingMode = com.garageledger.domain.model.OptionalFieldToggle.DRIVING_MODE in visibleFields
    val showDrivingCondition = com.garageledger.domain.model.OptionalFieldToggle.DRIVING_CONDITION in visibleFields
    val showTags = com.garageledger.domain.model.OptionalFieldToggle.TAGS in visibleFields
    val showNotes = com.garageledger.domain.model.OptionalFieldToggle.NOTES in visibleFields

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
                        OutlinedTextField(
                            value = dateTimeText,
                            onValueChange = { dateTimeText = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Date/Time (yyyy-MM-dd HH:mm)") },
                            singleLine = true,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Column(Modifier.weight(1f)) {
                                OutlinedTextField(
                                    value = odometerText,
                                    onValueChange = { odometerText = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    label = {
                                        Text(
                                            if (distanceMode) {
                                                "Distance From Previous (${preferences.distanceUnit.storageValue})"
                                            } else {
                                                "Odometer (${preferences.distanceUnit.storageValue})"
                                            },
                                        )
                                    },
                                    singleLine = true,
                                )
                                previousFillUp?.let { previous ->
                                    Text(
                                        "Previous fill-up: ${previous.odometerReading.toInt()} ${previous.distanceUnit.storageValue}",
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                }
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
                        OutlinedTextField(
                            value = volumeText,
                            onValueChange = {
                                volumeText = it
                                val solved = autoCompleteFuelCostFields(priceText, volumeText, totalText, changedField = FuelCostField.VOLUME)
                                priceText = solved.price
                                totalText = solved.total
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Volume (${preferences.volumeUnit.storageValue})") },
                            singleLine = true,
                        )
                        OutlinedTextField(
                            value = priceText,
                            onValueChange = {
                                priceText = it
                                val solved = autoCompleteFuelCostFields(priceText, volumeText, totalText, changedField = FuelCostField.PRICE)
                                volumeText = solved.volume
                                totalText = solved.total
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Price Per Unit") },
                            singleLine = true,
                        )
                        OutlinedTextField(
                            value = totalText,
                            onValueChange = {
                                totalText = it
                                val solved = autoCompleteFuelCostFields(priceText, volumeText, totalText, changedField = FuelCostField.TOTAL)
                                priceText = solved.price
                                volumeText = solved.volume
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Total Cost") },
                            singleLine = true,
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
                            OutlinedTextField(
                                value = paymentType,
                                onValueChange = { paymentType = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Payment Type") },
                                singleLine = true,
                            )
                            SuggestionRow(paymentSuggestions) { paymentType = it }
                        }
                        if (showFuelType) {
                            OutlinedTextField(
                                value = fuelTypeText,
                                onValueChange = { fuelTypeText = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Fuel Type") },
                                singleLine = true,
                            )
                        }
                        if (showFuelAdditive) {
                            ToggleRow("Fuel Additive", hasFuelAdditive) { hasFuelAdditive = it }
                            if (hasFuelAdditive) {
                                OutlinedTextField(
                                    value = fuelAdditiveName,
                                    onValueChange = { fuelAdditiveName = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    label = { Text("Fuel Additive Name") },
                                    singleLine = true,
                                )
                            }
                        }
                        if (showFuelingStation) {
                            OutlinedTextField(
                                value = fuelBrand,
                                onValueChange = { fuelBrand = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Fuel Brand") },
                                singleLine = true,
                            )
                            SuggestionRow(brandSuggestions) { fuelBrand = it }
                            OutlinedTextField(
                                value = stationAddress,
                                onValueChange = { stationAddress = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Station Address") },
                            )
                        }
                        if (showDrivingMode) {
                            OutlinedTextField(
                                value = drivingMode,
                                onValueChange = { drivingMode = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Driving Mode") },
                                singleLine = true,
                            )
                        }
                        if (showAverageSpeed) {
                            OutlinedTextField(
                                value = averageSpeedText,
                                onValueChange = { averageSpeedText = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Average Speed") },
                                singleLine = true,
                            )
                        }
                        if (showDrivingCondition) {
                            OutlinedTextField(
                                value = cityDrivingText,
                                onValueChange = { cityDrivingText = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("City Driving Percentage") },
                                singleLine = true,
                            )
                            OutlinedTextField(
                                value = highwayDrivingText,
                                onValueChange = { highwayDrivingText = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Highway Driving Percentage") },
                                singleLine = true,
                            )
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
                                        distanceUnit = preferences.distanceUnit,
                                        volume = volume,
                                        volumeUnit = preferences.volumeUnit,
                                        pricePerUnit = price,
                                        totalCost = total,
                                        paymentType = paymentType,
                                        partial = partial,
                                        previousMissedFillups = missed,
                                        fuelEfficiencyUnit = preferences.fuelEfficiencyUnit,
                                        fuelTypeId = existingRecord?.fuelTypeId,
                                        importedFuelTypeText = fuelTypeText.ifBlank { null },
                                        hasFuelAdditive = hasFuelAdditive,
                                        fuelAdditiveName = fuelAdditiveName,
                                        fuelBrand = fuelBrand,
                                        stationAddress = stationAddress,
                                        drivingMode = drivingMode,
                                        cityDrivingPercentage = cityDrivingText.toIntOrNull(),
                                        highwayDrivingPercentage = highwayDrivingText.toIntOrNull(),
                                        averageSpeed = averageSpeedText.toDoubleOrNull(),
                                        tags = tagsText.split(",").map { it.trim() }.filter { it.isNotBlank() },
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
