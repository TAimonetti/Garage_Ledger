package com.guzzlio.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.guzzlio.data.GarageRepository
import com.guzzlio.domain.model.ExpenseRecord
import com.guzzlio.domain.model.RecordAttachment
import com.guzzlio.domain.model.RecordFamily
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseEditorScreen(
    repository: GarageRepository,
    vehicleId: Long,
    recordId: Long,
    onOpenExpenseTypes: () -> Unit,
    onBack: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val vehicles by repository.observeVehicles().collectAsStateWithLifecycle(initialValue = emptyList())
    val preferences by repository.preferences.collectAsStateWithLifecycle(initialValue = com.guzzlio.domain.model.AppPreferenceSnapshot())
    val existingRecord by produceState<ExpenseRecord?>(initialValue = null, key1 = recordId) {
        value = if (recordId > 0L) repository.getExpense(recordId) else null
    }
    val existingAttachments by produceState(initialValue = emptyList<RecordAttachment>(), key1 = recordId) {
        value = if (recordId > 0L) repository.getRecordAttachments(RecordFamily.EXPENSE, recordId) else emptyList()
    }
    val suggestedOdometer by produceState<Double?>(initialValue = null, key1 = vehicleId) {
        value = repository.getSuggestedCurrentOdometer(vehicleId)
    }
    val expenseTypes by repository.observeExpenseTypes().collectAsStateWithLifecycle(initialValue = emptyList())
    val paymentSuggestions by produceState(initialValue = emptyList<String>()) {
        value = repository.getPaymentTypeSuggestions()
    }
    val centerSuggestions by produceState(initialValue = emptyList<String>()) {
        value = repository.getExpenseCenterSuggestions()
    }
    val centerAddressSuggestions by produceState(initialValue = emptyList<String>()) {
        value = repository.getExpenseCenterAddressSuggestions()
    }
    val tagSuggestions by produceState(initialValue = emptyList<String>()) {
        value = repository.getTagSuggestions()
    }

    LaunchedEffect(Unit) {
        repository.ensureExpenseTypeCatalogChoices()
    }

    var initialized by rememberSaveable(recordId) { mutableStateOf(false) }
    var dateTimeText by rememberSaveable { mutableStateOf("") }
    var odometerText by rememberSaveable { mutableStateOf("") }
    var totalCostText by rememberSaveable { mutableStateOf("") }
    var paymentType by rememberSaveable { mutableStateOf("") }
    var centerName by rememberSaveable { mutableStateOf("") }
    var centerAddress by rememberSaveable { mutableStateOf("") }
    var latitude by rememberSaveable { mutableStateOf<Double?>(null) }
    var longitude by rememberSaveable { mutableStateOf<Double?>(null) }
    var tagsText by rememberSaveable { mutableStateOf("") }
    var notesText by rememberSaveable { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var selectedTypeIds by remember { mutableStateOf(emptySet<Long>()) }
    var attachments by remember(recordId) { mutableStateOf(emptyList<RecordAttachment>()) }
    var attachmentsInitialized by remember(recordId) { mutableStateOf(false) }
    var showCustomizeFields by remember { mutableStateOf(false) }

    LaunchedEffect(existingRecord, suggestedOdometer) {
        if (initialized) return@LaunchedEffect
        val record = existingRecord
        if (record != null) {
            dateTimeText = record.dateTime.format(EditorDateFormatter)
            odometerText = record.odometerReading.toString()
            totalCostText = record.totalCost.toString()
            paymentType = record.paymentType
            centerName = record.expenseCenterName
            centerAddress = record.expenseCenterAddress
            latitude = record.latitude
            longitude = record.longitude
            tagsText = record.tags.joinToString(", ")
            notesText = record.notes
            selectedTypeIds = record.expenseTypeIds.toSet()
        } else {
            dateTimeText = java.time.LocalDateTime.now().format(EditorDateFormatter)
            suggestedOdometer?.let { odometerText = it.toString() }
        }
        initialized = true
    }

    LaunchedEffect(existingAttachments) {
        if (attachmentsInitialized) return@LaunchedEffect
        attachments = existingAttachments
        attachmentsInitialized = true
    }

    val visibleFields = preferences.visibleFields
    val showPaymentType = com.guzzlio.domain.model.OptionalFieldToggle.PAYMENT_TYPE in visibleFields
    val showExpenseCenter = com.guzzlio.domain.model.OptionalFieldToggle.EXPENSE_CENTER in visibleFields
    val showTags = com.guzzlio.domain.model.OptionalFieldToggle.TAGS in visibleFields
    val showNotes = com.guzzlio.domain.model.OptionalFieldToggle.NOTES in visibleFields
    val selectedTags = remember(tagsText) { parseCommaValues(tagsText) }
    val expenseTypeOptions = remember(expenseTypes) { expenseTypes.map { it.id to it.name } }

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
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    CompactDateTimeChooserRow(
                        label = "Date/Time",
                        value = dateTimeText,
                        onValueChange = { dateTimeText = it },
                    )
                    CompactNumericDialogRow(
                        label = "Odometer",
                        value = odometerText,
                        onValueChange = { odometerText = it },
                        decimalEnabled = true,
                        valueSuffix = preferences.distanceUnit.storageValue,
                    )
                    suggestedOdometer?.let { suggested ->
                        CompactFormReadoutRow(
                            label = "Suggested",
                            value = "${suggested.toStableString()} ${preferences.distanceUnit.storageValue}",
                        )
                    }
                    CompactNumericDialogRow(
                        label = "Total Cost",
                        value = totalCostText,
                        onValueChange = { totalCostText = it },
                        decimalEnabled = true,
                    )
                    if (showPaymentType) {
                        CompactChoiceDialogRow(
                            label = "Payment Type",
                            value = paymentType,
                            onValueChange = { paymentType = it },
                            choices = paymentSuggestions,
                        )
                    }
                }
            }
            item {
                CompactFormSectionHeader("Expense Types")
            }
            item {
                CompactMultiChoiceSelectionRow(
                    selectedIds = selectedTypeIds,
                    options = expenseTypeOptions,
                    onSelectionChange = { selectedTypeIds = it },
                    label = "Subtypes",
                    emptyChoicesMessage = "Import a backup or seed data to load expense types.",
                    extraActionLabel = "Manage Expense Types",
                    onExtraAction = onOpenExpenseTypes,
                )
            }
            if (showExpenseCenter) {
                item { CompactFormSectionHeader("Expense Center") }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        CompactChoiceDialogRow(
                            label = "Center",
                            value = centerName,
                            onValueChange = { centerName = it },
                            choices = centerSuggestions,
                        )
                        CompactChoiceDialogRow(
                            label = "Address",
                            value = centerAddress,
                            onValueChange = { centerAddress = it },
                            choices = centerAddressSuggestions,
                        )
                    }
                }
                item {
                    LocationActionSection(
                        title = "Expense Coordinates",
                        locationEnabled = preferences.useLocation,
                        latitude = latitude,
                        longitude = longitude,
                        mapLabel = centerAddress.ifBlank { centerName.ifBlank { "Expense" } },
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
            if (showTags || showNotes) {
                item { CompactFormSectionHeader("Notes and Tags") }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
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
            }
            item { CompactFormSectionHeader("Attachments") }
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
                                            ?: error("Use yyyy-MM-dd HH:mm for date and time."),
                                        odometerReading = odometerText.toDoubleOrNull()
                                            ?: error("Enter a valid odometer."),
                                        distanceUnit = preferences.distanceUnit,
                                        totalCost = totalCostText.toDoubleOrNull()
                                            ?: error("Enter a valid expense cost."),
                                        paymentType = paymentType,
                                        expenseCenterName = centerName,
                                        expenseCenterAddress = centerAddress,
                                        latitude = latitude,
                                        longitude = longitude,
                                        tags = parseCommaValues(tagsText),
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
