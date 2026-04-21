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
import com.garageledger.domain.model.OptionalFieldToggle
import com.garageledger.domain.model.Vehicle
import com.garageledger.domain.model.VehicleLifecycle
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun VehicleEditorScreen(
    repository: GarageRepository,
    vehicleId: Long,
    onBack: () -> Unit,
    onSaved: (Long) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val preferences by repository.preferences.collectAsStateWithLifecycle(initialValue = com.garageledger.domain.model.AppPreferenceSnapshot())
    val existingVehicle by produceState<Vehicle?>(initialValue = null, key1 = vehicleId) {
        value = if (vehicleId > 0L) repository.getVehicle(vehicleId) else null
    }

    var initialized by rememberSaveable(vehicleId) { mutableStateOf(false) }
    var name by rememberSaveable { mutableStateOf("") }
    var type by rememberSaveable { mutableStateOf("") }
    var yearText by rememberSaveable { mutableStateOf("") }
    var make by rememberSaveable { mutableStateOf("") }
    var model by rememberSaveable { mutableStateOf("") }
    var submodel by rememberSaveable { mutableStateOf("") }
    var country by rememberSaveable { mutableStateOf("") }
    var licensePlate by rememberSaveable { mutableStateOf("") }
    var vin by rememberSaveable { mutableStateOf("") }
    var insurancePolicy by rememberSaveable { mutableStateOf("") }
    var bodyStyle by rememberSaveable { mutableStateOf("") }
    var color by rememberSaveable { mutableStateOf("") }
    var engineDisplacement by rememberSaveable { mutableStateOf("") }
    var fuelTankCapacityText by rememberSaveable { mutableStateOf("") }
    var purchasePriceText by rememberSaveable { mutableStateOf("") }
    var purchaseOdometerText by rememberSaveable { mutableStateOf("") }
    var purchaseDateText by rememberSaveable { mutableStateOf("") }
    var sellingPriceText by rememberSaveable { mutableStateOf("") }
    var sellingOdometerText by rememberSaveable { mutableStateOf("") }
    var sellingDateText by rememberSaveable { mutableStateOf("") }
    var notes by rememberSaveable { mutableStateOf("") }
    var distanceUnitOverride by rememberSaveable { mutableStateOf<DistanceUnit?>(null) }
    var volumeUnitOverride by rememberSaveable { mutableStateOf<VolumeUnit?>(null) }
    var fuelEfficiencyUnitOverride by rememberSaveable { mutableStateOf<FuelEfficiencyUnit?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showCustomizeFields by remember { mutableStateOf(false) }

    LaunchedEffect(existingVehicle) {
        if (initialized) return@LaunchedEffect
        existingVehicle?.let { vehicle ->
            name = vehicle.name
            type = vehicle.type
            yearText = vehicle.year?.toString().orEmpty()
            make = vehicle.make
            model = vehicle.model
            submodel = vehicle.submodel
            country = vehicle.country
            licensePlate = vehicle.licensePlate
            vin = vehicle.vin
            insurancePolicy = vehicle.insurancePolicy
            bodyStyle = vehicle.bodyStyle
            color = vehicle.color
            engineDisplacement = vehicle.engineDisplacement
            fuelTankCapacityText = vehicle.fuelTankCapacity?.toString().orEmpty()
            purchasePriceText = vehicle.purchasePrice?.toString().orEmpty()
            purchaseOdometerText = vehicle.purchaseOdometer?.toString().orEmpty()
            purchaseDateText = vehicle.purchaseDate?.toString().orEmpty()
            sellingPriceText = vehicle.sellingPrice?.toString().orEmpty()
            sellingOdometerText = vehicle.sellingOdometer?.toString().orEmpty()
            sellingDateText = vehicle.sellingDate?.toString().orEmpty()
            notes = vehicle.notes
            distanceUnitOverride = vehicle.distanceUnitOverride
            volumeUnitOverride = vehicle.volumeUnitOverride
            fuelEfficiencyUnitOverride = vehicle.fuelEfficiencyUnitOverride
        }
        initialized = true
    }

    if (showCustomizeFields) {
        VisibleFieldsDialog(
            title = "Customize Vehicle Screen",
            options = VehicleVisibleFieldOptions,
            visibleFields = preferences.visibleFields,
            onToggle = { toggle, visible ->
                scope.launch { repository.setVisibleField(toggle, visible) }
            },
            onDismiss = { showCustomizeFields = false },
        )
    }

    val visibleFields = preferences.visibleFields
    val showLicensePlate = OptionalFieldToggle.VEHICLE_LICENSE_PLATE in visibleFields
    val showVin = OptionalFieldToggle.VEHICLE_VIN in visibleFields
    val showInsurance = OptionalFieldToggle.VEHICLE_INSURANCE_POLICY in visibleFields
    val showBodyStyle = OptionalFieldToggle.VEHICLE_BODY_STYLE in visibleFields
    val showColor = OptionalFieldToggle.VEHICLE_COLOR in visibleFields
    val showEngineDisplacement = OptionalFieldToggle.VEHICLE_ENGINE_DISPLACEMENT in visibleFields
    val showFuelTankCapacity = OptionalFieldToggle.VEHICLE_FUEL_TANK_CAPACITY in visibleFields
    val showPurchaseInfo = OptionalFieldToggle.VEHICLE_PURCHASE_INFO in visibleFields
    val showSellingInfo = OptionalFieldToggle.VEHICLE_SELLING_INFO in visibleFields
    val showNotes = OptionalFieldToggle.NOTES in visibleFields

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(if (vehicleId > 0L) "Edit Vehicle" else "Add Vehicle") },
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
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Vehicle Profile", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("Add the core vehicle details first, then keep the optional identity and purchase fields as dense or as minimal as you want.")
                        existingVehicle?.let { vehicle ->
                            Text("Lifecycle: ${vehicle.lifecycle.name.lowercase().replaceFirstChar(Char::uppercase)}")
                        }
                    }
                }
            }
            item {
                Card {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Vehicle Name") },
                            singleLine = true,
                        )
                        OutlinedTextField(
                            value = type,
                            onValueChange = { type = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Type") },
                            singleLine = true,
                        )
                        OutlinedTextField(
                            value = yearText,
                            onValueChange = { yearText = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Year") },
                            singleLine = true,
                        )
                        OutlinedTextField(
                            value = make,
                            onValueChange = { make = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Make") },
                            singleLine = true,
                        )
                        OutlinedTextField(
                            value = model,
                            onValueChange = { model = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Model") },
                            singleLine = true,
                        )
                        OutlinedTextField(
                            value = submodel,
                            onValueChange = { submodel = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Submodel") },
                            singleLine = true,
                        )
                        OutlinedTextField(
                            value = country,
                            onValueChange = { country = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Country") },
                            singleLine = true,
                        )
                    }
                }
            }
            item {
                Card {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Per-Vehicle Units", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        VehicleEnumSection(
                            title = "Distance Unit Override",
                            options = DistanceUnit.entries,
                            selected = distanceUnitOverride,
                            onSelect = { distanceUnitOverride = it },
                        ) { option -> option?.storageValue ?: "App Default" }
                        VehicleEnumSection(
                            title = "Volume Unit Override",
                            options = VolumeUnit.entries,
                            selected = volumeUnitOverride,
                            onSelect = { volumeUnitOverride = it },
                        ) { option -> option?.storageValue ?: "App Default" }
                        VehicleEnumSection(
                            title = "Fuel Efficiency Override",
                            options = FuelEfficiencyUnit.entries,
                            selected = fuelEfficiencyUnitOverride,
                            onSelect = { fuelEfficiencyUnitOverride = it },
                        ) { option -> option?.storageValue ?: "App Default" }
                    }
                }
            }
            item {
                Card {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (showLicensePlate) {
                            OutlinedTextField(
                                value = licensePlate,
                                onValueChange = { licensePlate = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("License Plate") },
                                singleLine = true,
                            )
                        }
                        if (showVin) {
                            OutlinedTextField(
                                value = vin,
                                onValueChange = { vin = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("VIN") },
                                singleLine = true,
                            )
                        }
                        if (showInsurance) {
                            OutlinedTextField(
                                value = insurancePolicy,
                                onValueChange = { insurancePolicy = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Insurance Policy") },
                                singleLine = true,
                            )
                        }
                        if (showBodyStyle) {
                            OutlinedTextField(
                                value = bodyStyle,
                                onValueChange = { bodyStyle = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Body Style") },
                                singleLine = true,
                            )
                        }
                        if (showColor) {
                            OutlinedTextField(
                                value = color,
                                onValueChange = { color = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Color") },
                                singleLine = true,
                            )
                        }
                        if (showEngineDisplacement) {
                            OutlinedTextField(
                                value = engineDisplacement,
                                onValueChange = { engineDisplacement = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Engine Displacement") },
                                singleLine = true,
                            )
                        }
                        if (showFuelTankCapacity) {
                            OutlinedTextField(
                                value = fuelTankCapacityText,
                                onValueChange = { fuelTankCapacityText = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Fuel Tank Capacity") },
                                singleLine = true,
                            )
                        }
                    }
                }
            }
            if (showPurchaseInfo) {
                item {
                    Card {
                        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Purchase Info", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            OutlinedTextField(
                                value = purchasePriceText,
                                onValueChange = { purchasePriceText = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Purchase Price") },
                                singleLine = true,
                            )
                            OutlinedTextField(
                                value = purchaseOdometerText,
                                onValueChange = { purchaseOdometerText = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Purchase Odometer") },
                                singleLine = true,
                            )
                            OutlinedTextField(
                                value = purchaseDateText,
                                onValueChange = { purchaseDateText = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Purchase Date (yyyy-MM-dd)") },
                                singleLine = true,
                            )
                        }
                    }
                }
            }
            if (showSellingInfo) {
                item {
                    Card {
                        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Selling Info", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            OutlinedTextField(
                                value = sellingPriceText,
                                onValueChange = { sellingPriceText = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Selling Price") },
                                singleLine = true,
                            )
                            OutlinedTextField(
                                value = sellingOdometerText,
                                onValueChange = { sellingOdometerText = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Selling Odometer") },
                                singleLine = true,
                            )
                            OutlinedTextField(
                                value = sellingDateText,
                                onValueChange = { sellingDateText = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Selling Date (yyyy-MM-dd)") },
                                singleLine = true,
                            )
                        }
                    }
                }
            }
            if (showNotes) {
                item {
                    Card {
                        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = notes,
                                onValueChange = { notes = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Notes") },
                                minLines = 3,
                            )
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
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        scope.launch {
                            runCatching {
                                val savedId = repository.saveVehicle(
                                    Vehicle(
                                        id = existingVehicle?.id ?: 0L,
                                        legacySourceId = existingVehicle?.legacySourceId,
                                        name = name.trim().ifBlank { error("Vehicle name is required.") },
                                        type = type.trim(),
                                        year = yearText.toIntOrNull(),
                                        make = make.trim(),
                                        model = model.trim(),
                                        submodel = submodel.trim(),
                                        lifecycle = existingVehicle?.lifecycle ?: VehicleLifecycle.ACTIVE,
                                        country = country.trim(),
                                        licensePlate = licensePlate.trim(),
                                        vin = vin.trim(),
                                        insurancePolicy = insurancePolicy.trim(),
                                        bodyStyle = bodyStyle.trim(),
                                        color = color.trim(),
                                        engineDisplacement = engineDisplacement.trim(),
                                        fuelTankCapacity = fuelTankCapacityText.toDoubleOrNull(),
                                        purchasePrice = purchasePriceText.toDoubleOrNull(),
                                        purchaseOdometer = purchaseOdometerText.toDoubleOrNull(),
                                        purchaseDate = purchaseDateText.takeIf(String::isNotBlank)?.let {
                                            parseFilterDate(it) ?: error("Use yyyy-MM-dd for purchase date.")
                                        },
                                        sellingPrice = sellingPriceText.toDoubleOrNull(),
                                        sellingOdometer = sellingOdometerText.toDoubleOrNull(),
                                        sellingDate = sellingDateText.takeIf(String::isNotBlank)?.let {
                                            parseFilterDate(it) ?: error("Use yyyy-MM-dd for selling date.")
                                        },
                                        notes = notes.trim(),
                                        profilePhotoUri = existingVehicle?.profilePhotoUri,
                                        distanceUnitOverride = distanceUnitOverride,
                                        volumeUnitOverride = volumeUnitOverride,
                                        fuelEfficiencyUnitOverride = fuelEfficiencyUnitOverride,
                                    ),
                                )
                                savedId
                            }.onSuccess { savedId ->
                                errorMessage = null
                                onSaved(savedId)
                            }.onFailure { error ->
                                errorMessage = error.message
                            }
                        }
                    },
                ) {
                    Text(if (vehicleId > 0L) "Save Vehicle" else "Add Vehicle")
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> VehicleEnumSection(
    title: String,
    options: List<T>,
    selected: T?,
    onSelect: (T?) -> Unit,
    label: (T?) -> String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, fontWeight = FontWeight.Medium)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(
                selected = selected == null,
                onClick = { onSelect(null) },
                label = { Text(label(null)) },
            )
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
