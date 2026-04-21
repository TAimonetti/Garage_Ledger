package com.garageledger.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import com.garageledger.domain.model.ExpenseRecord
import com.garageledger.domain.model.RecordAttachment
import com.garageledger.domain.model.RecordFamily
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseEditorScreen(
    repository: GarageRepository,
    vehicleId: Long,
    recordId: Long,
    onBack: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val vehicles by repository.observeVehicles().collectAsStateWithLifecycle(initialValue = emptyList())
    val preferences by repository.preferences.collectAsStateWithLifecycle(initialValue = com.garageledger.domain.model.AppPreferenceSnapshot())
    val existingRecord by produceState<ExpenseRecord?>(initialValue = null, key1 = recordId) {
        value = if (recordId > 0L) repository.getExpense(recordId) else null
    }
    val existingAttachments by produceState(initialValue = emptyList<RecordAttachment>(), key1 = recordId) {
        value = if (recordId > 0L) repository.getRecordAttachments(RecordFamily.EXPENSE, recordId) else emptyList()
    }
    val expenseTypes by produceState(initialValue = emptyList<com.garageledger.domain.model.ExpenseType>()) {
        value = repository.getExpenseTypes()
    }
    val paymentSuggestions by produceState(initialValue = emptyList<String>()) {
        value = repository.getPaymentTypeSuggestions()
    }
    val centerSuggestions by produceState(initialValue = emptyList<String>()) {
        value = repository.getExpenseCenterSuggestions()
    }

    var initialized by rememberSaveable(recordId) { mutableStateOf(false) }
    var dateTimeText by rememberSaveable { mutableStateOf("") }
    var odometerText by rememberSaveable { mutableStateOf("") }
    var totalCostText by rememberSaveable { mutableStateOf("") }
    var paymentType by rememberSaveable { mutableStateOf("") }
    var centerName by rememberSaveable { mutableStateOf("") }
    var centerAddress by rememberSaveable { mutableStateOf("") }
    var tagsText by rememberSaveable { mutableStateOf("") }
    var notesText by rememberSaveable { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var selectedTypeIds by remember { mutableStateOf(emptySet<Long>()) }
    var attachments by remember(recordId) { mutableStateOf(emptyList<RecordAttachment>()) }
    var attachmentsInitialized by remember(recordId) { mutableStateOf(false) }
    var showCustomizeFields by remember { mutableStateOf(false) }

    LaunchedEffect(existingRecord) {
        if (initialized) return@LaunchedEffect
        val record = existingRecord
        if (record != null) {
            dateTimeText = record.dateTime.format(EditorDateFormatter)
            odometerText = record.odometerReading.toString()
            totalCostText = record.totalCost.toString()
            paymentType = record.paymentType
            centerName = record.expenseCenterName
            centerAddress = record.expenseCenterAddress
            tagsText = record.tags.joinToString(", ")
            notesText = record.notes
            selectedTypeIds = record.expenseTypeIds.toSet()
        } else {
            dateTimeText = java.time.LocalDateTime.now().format(EditorDateFormatter)
        }
        initialized = true
    }

    LaunchedEffect(existingAttachments) {
        if (attachmentsInitialized) return@LaunchedEffect
        attachments = existingAttachments
        attachmentsInitialized = true
    }

    val vehicleName = vehicles.firstOrNull { it.id == vehicleId }?.name ?: "Expense"
    val visibleFields = preferences.visibleFields
    val showPaymentType = com.garageledger.domain.model.OptionalFieldToggle.PAYMENT_TYPE in visibleFields
    val showExpenseCenter = com.garageledger.domain.model.OptionalFieldToggle.EXPENSE_CENTER in visibleFields
    val showTags = com.garageledger.domain.model.OptionalFieldToggle.TAGS in visibleFields
    val showNotes = com.garageledger.domain.model.OptionalFieldToggle.NOTES in visibleFields

    if (showCustomizeFields) {
        VisibleFieldsDialog(
            title = "Customize Expense Screen",
            options = ExpenseVisibleFieldOptions,
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
                title = { Text(if (recordId > 0L) "Edit Expense" else "New Expense") },
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
                        Text("Expense records stay normalized so imports and exports keep subtype fidelity.")
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
                        OutlinedTextField(
                            value = odometerText,
                            onValueChange = { odometerText = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Odometer (${preferences.distanceUnit.storageValue})") },
                            singleLine = true,
                        )
                        OutlinedTextField(
                            value = totalCostText,
                            onValueChange = { totalCostText = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Total Cost") },
                            singleLine = true,
                        )
                        if (showPaymentType) {
                            OutlinedTextField(
                                value = paymentType,
                                onValueChange = { paymentType = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Payment Type") },
                                singleLine = true,
                            )
                            SuggestionRow(paymentSuggestions, onSelect = { paymentType = it })
                        }
                    }
                }
            }
            item {
                Card {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Expense Types", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        if (expenseTypes.isEmpty()) {
                            Text("Import a backup or seed data to load expense types.")
                        } else {
                            MultiSelectTypeChips(
                                options = expenseTypes.map { it.id to it.name },
                                selectedIds = selectedTypeIds,
                                onToggle = { id ->
                                    selectedTypeIds = selectedTypeIds.toMutableSet().also { selected ->
                                        if (!selected.add(id)) selected.remove(id)
                                    }
                                },
                            )
                        }
                    }
                }
            }
            item {
                Card {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (showExpenseCenter) {
                            OutlinedTextField(
                                value = centerName,
                                onValueChange = { centerName = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Expense Center") },
                                singleLine = true,
                            )
                            SuggestionRow(centerSuggestions, onSelect = { centerName = it })
                            OutlinedTextField(
                                value = centerAddress,
                                onValueChange = { centerAddress = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Expense Center Address") },
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
                    recordFamily = RecordFamily.EXPENSE,
                    attachments = attachments,
                    onAttachmentsChange = { attachments = it },
                    onError = { errorMessage = it },
                )
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
                                val savedId = repository.saveExpense(
                                    ExpenseRecord(
                                        id = existingRecord?.id ?: 0L,
                                        legacySourceId = existingRecord?.legacySourceId,
                                        vehicleId = vehicleId,
                                        dateTime = parseEditorDateTime(dateTimeText)
                                            ?: error("Use `yyyy-MM-dd HH:mm` for date and time."),
                                        odometerReading = odometerText.toDoubleOrNull()
                                            ?: error("Enter a valid odometer."),
                                        distanceUnit = preferences.distanceUnit,
                                        totalCost = totalCostText.toDoubleOrNull()
                                            ?: error("Enter a valid expense cost."),
                                        paymentType = paymentType,
                                        expenseCenterName = centerName,
                                        expenseCenterAddress = centerAddress,
                                        tags = tagsText.split(",").map(String::trim).filter(String::isNotBlank),
                                        notes = notesText,
                                        expenseTypeIds = selectedTypeIds.toList().sorted(),
                                    ),
                                )
                                repository.replaceRecordAttachments(
                                    vehicleId = vehicleId,
                                    recordFamily = RecordFamily.EXPENSE,
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
                    Text(if (recordId > 0L) "Save Expense" else "Add Expense")
                }
            }
        }
    }
}
