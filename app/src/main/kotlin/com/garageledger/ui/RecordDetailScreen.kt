package com.garageledger.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.garageledger.data.GarageRepository
import com.garageledger.domain.model.ExpenseRecord
import com.garageledger.domain.model.FillUpRecord
import com.garageledger.domain.model.RecordAttachment
import com.garageledger.domain.model.RecordFamily
import com.garageledger.domain.model.ServiceRecord
import com.garageledger.domain.model.TripRecord
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordDetailScreen(
    repository: GarageRepository,
    family: RecordFamily,
    vehicleId: Long,
    recordId: Long,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDeleted: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val vehicles by repository.observeVehicles().collectAsStateWithLifecycle(initialValue = emptyList())
    val preferences by repository.preferences.collectAsStateWithLifecycle(initialValue = com.garageledger.domain.model.AppPreferenceSnapshot())
    var refreshSignal by remember { mutableIntStateOf(0) }
    var confirmDelete by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshSignal++
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val fillUp by produceState<FillUpRecord?>(initialValue = null, key1 = family, key2 = recordId, key3 = refreshSignal) {
        value = if (family == RecordFamily.FILL_UP) repository.getFillUp(recordId) else null
    }
    val service by produceState<ServiceRecord?>(initialValue = null, key1 = family, key2 = recordId, key3 = refreshSignal) {
        value = if (family == RecordFamily.SERVICE) repository.getService(recordId) else null
    }
    val expense by produceState<ExpenseRecord?>(initialValue = null, key1 = family, key2 = recordId, key3 = refreshSignal) {
        value = if (family == RecordFamily.EXPENSE) repository.getExpense(recordId) else null
    }
    val trip by produceState<TripRecord?>(initialValue = null, key1 = family, key2 = recordId, key3 = refreshSignal) {
        value = if (family == RecordFamily.TRIP) repository.getTrip(recordId) else null
    }
    val attachments by produceState(initialValue = emptyList<RecordAttachment>(), key1 = family, key2 = recordId, key3 = refreshSignal) {
        value = repository.getRecordAttachments(family, recordId)
    }
    val serviceTypes by produceState(initialValue = emptyList<com.garageledger.domain.model.ServiceType>(), key1 = family, key2 = refreshSignal) {
        value = if (family == RecordFamily.SERVICE) repository.getServiceTypes() else emptyList()
    }
    val expenseTypes by produceState(initialValue = emptyList<com.garageledger.domain.model.ExpenseType>(), key1 = family, key2 = refreshSignal) {
        value = if (family == RecordFamily.EXPENSE) repository.getExpenseTypes() else emptyList()
    }
    val tripTypes by produceState(initialValue = emptyList<com.garageledger.domain.model.TripType>(), key1 = family, key2 = refreshSignal) {
        value = if (family == RecordFamily.TRIP) repository.getTripTypes() else emptyList()
    }

    val vehicleName = vehicles.firstOrNull { it.id == vehicleId }?.name ?: "Vehicle"
    val title = when (family) {
        RecordFamily.FILL_UP -> "Fuel-Up Details"
        RecordFamily.SERVICE -> "Service Details"
        RecordFamily.EXPENSE -> "Expense Details"
        RecordFamily.TRIP -> "Trip Details"
    }

    val recordFound = when (family) {
        RecordFamily.FILL_UP -> fillUp != null
        RecordFamily.SERVICE -> service != null
        RecordFamily.EXPENSE -> expense != null
        RecordFamily.TRIP -> trip != null
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete Record?") },
            text = { Text("This action is permanent and cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            runCatching {
                                when (family) {
                                    RecordFamily.FILL_UP -> repository.deleteFillUp(recordId)
                                    RecordFamily.SERVICE -> repository.deleteService(recordId)
                                    RecordFamily.EXPENSE -> repository.deleteExpense(recordId)
                                    RecordFamily.TRIP -> repository.deleteTrip(recordId)
                                }
                            }.onSuccess {
                                confirmDelete = false
                                errorMessage = null
                                onDeleted()
                            }.onFailure {
                                errorMessage = it.message
                            }
                        }
                    },
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Cancel") }
            },
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(title) },
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
                        Text(vehicleName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(
                            when (family) {
                                RecordFamily.FILL_UP -> "Review the saved fuel-up before editing or deleting it."
                                RecordFamily.SERVICE -> "Review the saved service record and linked service subtypes."
                                RecordFamily.EXPENSE -> "Review the saved expense record and normalized expense subtypes."
                                RecordFamily.TRIP -> "Review the saved trip, reimbursement data, and calculated distance."
                            },
                        )
                    }
                }
            }
            if (!recordFound) {
                item {
                    Card {
                        Text("This record could not be found.", modifier = Modifier.padding(18.dp))
                    }
                }
            } else {
                item {
                    Card {
                        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            when (family) {
                                RecordFamily.FILL_UP -> FuelUpDetailContent(fillUp = fillUp!!, currencySymbol = preferences.currencySymbol)
                                RecordFamily.SERVICE -> ServiceDetailContent(
                                    service = service!!,
                                    currencySymbol = preferences.currencySymbol,
                                    serviceNames = serviceTypes.associate { it.id to it.name },
                                )
                                RecordFamily.EXPENSE -> ExpenseDetailContent(
                                    expense = expense!!,
                                    currencySymbol = preferences.currencySymbol,
                                    expenseNames = expenseTypes.associate { it.id to it.name },
                                )
                                RecordFamily.TRIP -> TripDetailContent(
                                    trip = trip!!,
                                    currencySymbol = preferences.currencySymbol,
                                    tripNames = tripTypes.associate { it.id to it.name },
                                )
                            }
                        }
                    }
                }
                if (attachments.isNotEmpty()) {
                    item {
                        Card {
                            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("Attachments", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                attachments.forEach { attachment ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                    ) {
                                        Column(Modifier.weight(1f)) {
                                            Text(attachment.displayName.ifBlank { "Attachment" })
                                            if (attachment.mimeType.isNotBlank()) {
                                                Text(attachment.mimeType, style = MaterialTheme.typography.bodySmall)
                                            }
                                        }
                                        TextButton(onClick = { context.openAttachment(attachment) }) {
                                            Text("Open")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                errorMessage?.let { message ->
                    item {
                        Card(colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                            Text(message, modifier = Modifier.padding(18.dp))
                        }
                    }
                }
                item {
                    Card {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Button(modifier = Modifier.weight(1f), onClick = onEdit) {
                                Text("Edit")
                            }
                            Button(
                                modifier = Modifier.weight(1f),
                                onClick = { confirmDelete = true },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer,
                                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                                ),
                            ) {
                                Text("Delete")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FuelUpDetailContent(
    fillUp: FillUpRecord,
    currencySymbol: String,
) {
    DetailValue("Date / Time", fillUp.dateTime.format(DetailDateFormatter))
    DetailValue("Odometer", "${fillUp.odometerReading.toStableString()} ${fillUp.distanceUnit.storageValue}")
    DetailValue("Volume", "${fillUp.volume.toStableString()} ${fillUp.volumeUnit.storageValue}")
    DetailValue("Price / Unit", fillUp.pricePerUnit.asCurrency(currencySymbol))
    DetailValue("Total Cost", fillUp.totalCost.asCurrency(currencySymbol))
    fillUp.fuelEfficiency?.let { DetailValue("Fuel Efficiency", it.toStableString()) }
    fillUp.paymentType.takeIf(String::isNotBlank)?.let { DetailValue("Payment Type", it) }
    fillUp.importedFuelTypeText?.takeIf(String::isNotBlank)?.let { DetailValue("Fuel Type", it) }
    if (fillUp.hasFuelAdditive) {
        DetailValue("Fuel Additive", fillUp.fuelAdditiveName.ifBlank { "Yes" })
    }
    fillUp.fuelBrand.takeIf(String::isNotBlank)?.let { DetailValue("Fuel Brand", it) }
    fillUp.stationAddress.takeIf(String::isNotBlank)?.let { DetailValue("Station Address", it) }
    fillUp.drivingMode.takeIf(String::isNotBlank)?.let { DetailValue("Driving Mode", it) }
    fillUp.averageSpeed?.let { DetailValue("Average Speed", it.toStableString()) }
    fillUp.cityDrivingPercentage?.let { DetailValue("City Driving %", it.toString()) }
    fillUp.highwayDrivingPercentage?.let { DetailValue("Highway Driving %", it.toString()) }
    if (fillUp.partial) DetailValue("Partial Fill-Up", "Yes")
    if (fillUp.previousMissedFillups) DetailValue("Previous Missed Fill-Ups", "Yes")
    if (fillUp.tags.isNotEmpty()) DetailValue("Tags", fillUp.tags.joinToString(", "))
    fillUp.notes.takeIf(String::isNotBlank)?.let { DetailValue("Notes", it) }
}

@Composable
private fun ServiceDetailContent(
    service: ServiceRecord,
    currencySymbol: String,
    serviceNames: Map<Long, String>,
) {
    DetailValue("Date / Time", service.dateTime.format(DetailDateFormatter))
    DetailValue("Odometer", "${service.odometerReading.toStableString()} ${service.distanceUnit.storageValue}")
    DetailValue("Total Cost", service.totalCost.asCurrency(currencySymbol))
    if (service.serviceTypeIds.isNotEmpty()) {
        DetailValue("Subtypes", service.serviceTypeIds.mapNotNull(serviceNames::get).ifEmpty { service.serviceTypeIds.map(Long::toString) }.joinToString(", "))
    }
    service.paymentType.takeIf(String::isNotBlank)?.let { DetailValue("Payment Type", it) }
    service.serviceCenterName.takeIf(String::isNotBlank)?.let { DetailValue("Service Center", it) }
    service.serviceCenterAddress.takeIf(String::isNotBlank)?.let { DetailValue("Service Address", it) }
    if (service.tags.isNotEmpty()) DetailValue("Tags", service.tags.joinToString(", "))
    service.notes.takeIf(String::isNotBlank)?.let { DetailValue("Notes", it) }
}

@Composable
private fun ExpenseDetailContent(
    expense: ExpenseRecord,
    currencySymbol: String,
    expenseNames: Map<Long, String>,
) {
    DetailValue("Date / Time", expense.dateTime.format(DetailDateFormatter))
    DetailValue("Odometer", "${expense.odometerReading.toStableString()} ${expense.distanceUnit.storageValue}")
    DetailValue("Total Cost", expense.totalCost.asCurrency(currencySymbol))
    if (expense.expenseTypeIds.isNotEmpty()) {
        DetailValue("Subtypes", expense.expenseTypeIds.mapNotNull(expenseNames::get).ifEmpty { expense.expenseTypeIds.map(Long::toString) }.joinToString(", "))
    }
    expense.paymentType.takeIf(String::isNotBlank)?.let { DetailValue("Payment Type", it) }
    expense.expenseCenterName.takeIf(String::isNotBlank)?.let { DetailValue("Expense Center", it) }
    expense.expenseCenterAddress.takeIf(String::isNotBlank)?.let { DetailValue("Expense Address", it) }
    if (expense.tags.isNotEmpty()) DetailValue("Tags", expense.tags.joinToString(", "))
    expense.notes.takeIf(String::isNotBlank)?.let { DetailValue("Notes", it) }
}

@Composable
private fun TripDetailContent(
    trip: TripRecord,
    currencySymbol: String,
    tripNames: Map<Long, String>,
) {
    DetailValue("Start", trip.startDateTime.format(DetailDateFormatter))
    DetailValue("Start Odometer", "${trip.startOdometerReading.toStableString()} ${trip.distanceUnit.storageValue}")
    trip.startLocation.takeIf(String::isNotBlank)?.let { DetailValue("Start Location", it) }
    trip.endDateTime?.let { DetailValue("End", it.format(DetailDateFormatter)) }
    trip.endOdometerReading?.let { DetailValue("End Odometer", "${it.toStableString()} ${trip.distanceUnit.storageValue}") }
    trip.endLocation.takeIf(String::isNotBlank)?.let { DetailValue("End Location", it) }
    trip.tripTypeId?.let { id -> tripNames[id]?.let { DetailValue("Trip Type", it) } }
    trip.distance?.let { DetailValue("Distance", "${it.toStableString()} ${trip.distanceUnit.storageValue}") }
    trip.durationMillis?.let {
        val hours = it / (60 * 60 * 1000)
        val minutes = (it / (60 * 1000)) % 60
        DetailValue("Duration", "${hours}h ${minutes}m")
    }
    trip.purpose.takeIf(String::isNotBlank)?.let { DetailValue("Purpose", it) }
    trip.client.takeIf(String::isNotBlank)?.let { DetailValue("Client", it) }
    trip.taxDeductionRate?.let { DetailValue("Tax Deduction Rate", it.toStableString()) }
    trip.taxDeductionAmount?.let { DetailValue("Tax Deduction Amount", it.asCurrency(currencySymbol)) }
    trip.reimbursementRate?.let { DetailValue("Reimbursement Rate", it.toStableString()) }
    trip.reimbursementAmount?.let { DetailValue("Reimbursement Amount", it.asCurrency(currencySymbol)) }
    DetailValue("Paid", if (trip.paid) "Yes" else "No")
    if (trip.tags.isNotEmpty()) DetailValue("Tags", trip.tags.joinToString(", "))
    trip.notes.takeIf(String::isNotBlank)?.let { DetailValue("Notes", it) }
}

@Composable
private fun DetailValue(
    label: String,
    value: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}

private val DetailDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm")
