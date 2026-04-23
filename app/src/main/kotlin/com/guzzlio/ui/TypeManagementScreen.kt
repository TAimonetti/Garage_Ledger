package com.guzzlio.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.guzzlio.data.GarageRepository
import com.guzzlio.domain.model.ExpenseType
import com.guzzlio.domain.model.FuelType
import com.guzzlio.domain.model.ServiceType
import com.guzzlio.domain.model.TripType
import kotlinx.coroutines.launch

enum class TypeManagementTab(val label: String, val routeSegment: String) {
    FUEL("Fuel Types", "fuel"),
    SERVICE("Services", "service"),
    EXPENSE("Expenses", "expense"),
    TRIP("Trip Types", "trip");

    companion object {
        fun fromRouteSegment(raw: String?): TypeManagementTab? =
            entries.firstOrNull { it.routeSegment.equals(raw, ignoreCase = true) }
    }
}

private data class PendingDelete(
    val tab: TypeManagementTab,
    val id: Long,
    val label: String,
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TypeManagementScreen(
    repository: GarageRepository,
    initialTab: TypeManagementTab = TypeManagementTab.FUEL,
    onBack: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val fuelTypes by repository.observeFuelTypes().collectAsStateWithLifecycle(initialValue = emptyList())
    val serviceTypes by repository.observeServiceTypes().collectAsStateWithLifecycle(initialValue = emptyList())
    val expenseTypes by repository.observeExpenseTypes().collectAsStateWithLifecycle(initialValue = emptyList())
    val tripTypes by repository.observeTripTypes().collectAsStateWithLifecycle(initialValue = emptyList())

    var selectedTab by remember(initialTab) { mutableStateOf(initialTab) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    var pendingDelete by remember { mutableStateOf<PendingDelete?>(null) }

    var showFuelDialog by remember { mutableStateOf(false) }
    var editingFuel by remember { mutableStateOf<FuelType?>(null) }
    var showServiceDialog by remember { mutableStateOf(false) }
    var editingService by remember { mutableStateOf<ServiceType?>(null) }
    var showExpenseDialog by remember { mutableStateOf(false) }
    var editingExpense by remember { mutableStateOf<ExpenseType?>(null) }
    var showTripDialog by remember { mutableStateOf(false) }
    var editingTrip by remember { mutableStateOf<TripType?>(null) }

    LaunchedEffect(selectedTab) {
        when (selectedTab) {
            TypeManagementTab.FUEL -> repository.ensureFuelTypeCatalogChoices()
            TypeManagementTab.SERVICE -> repository.ensureServiceTypeCatalogChoices()
            TypeManagementTab.EXPENSE -> repository.ensureExpenseTypeCatalogChoices()
            TypeManagementTab.TRIP -> repository.ensureTripTypeCatalogChoices()
        }
    }

    pendingDelete?.let { pending ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete Type?") },
            text = { Text("Delete `${pending.label}` from ${pending.tab.label}? This is blocked if records already use it.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            runCatching {
                                when (pending.tab) {
                                    TypeManagementTab.FUEL -> repository.deleteFuelType(pending.id)
                                    TypeManagementTab.SERVICE -> repository.deleteServiceType(pending.id)
                                    TypeManagementTab.EXPENSE -> repository.deleteExpenseType(pending.id)
                                    TypeManagementTab.TRIP -> repository.deleteTripType(pending.id)
                                }
                            }.onSuccess {
                                successMessage = "${pending.label} deleted."
                                errorMessage = null
                                pendingDelete = null
                            }.onFailure { error ->
                                errorMessage = error.message
                                successMessage = null
                                pendingDelete = null
                            }
                        }
                    },
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Cancel") }
            },
        )
    }

    if (showFuelDialog) {
        FuelTypeDialog(
            initial = editingFuel,
            onDismiss = {
                showFuelDialog = false
                editingFuel = null
            },
            onSave = { type ->
                scope.launch {
                    runCatching { repository.saveFuelType(type) }
                        .onSuccess {
                            successMessage = if (type.id == 0L) "Fuel type added." else "Fuel type updated."
                            errorMessage = null
                            showFuelDialog = false
                            editingFuel = null
                        }
                        .onFailure {
                            errorMessage = it.message
                            successMessage = null
                        }
                }
            },
        )
    }
    if (showServiceDialog) {
        ServiceTypeDialog(
            initial = editingService,
            onDismiss = {
                showServiceDialog = false
                editingService = null
            },
            onSave = { type ->
                scope.launch {
                    runCatching { repository.saveServiceType(type) }
                        .onSuccess {
                            successMessage = if (type.id == 0L) "Service type added." else "Service type updated."
                            errorMessage = null
                            showServiceDialog = false
                            editingService = null
                        }
                        .onFailure {
                            errorMessage = it.message
                            successMessage = null
                        }
                }
            },
        )
    }
    if (showExpenseDialog) {
        ExpenseTypeDialog(
            initial = editingExpense,
            onDismiss = {
                showExpenseDialog = false
                editingExpense = null
            },
            onSave = { type ->
                scope.launch {
                    runCatching { repository.saveExpenseType(type) }
                        .onSuccess {
                            successMessage = if (type.id == 0L) "Expense type added." else "Expense type updated."
                            errorMessage = null
                            showExpenseDialog = false
                            editingExpense = null
                        }
                        .onFailure {
                            errorMessage = it.message
                            successMessage = null
                        }
                }
            },
        )
    }
    if (showTripDialog) {
        TripTypeDialog(
            initial = editingTrip,
            onDismiss = {
                showTripDialog = false
                editingTrip = null
            },
            onSave = { type ->
                scope.launch {
                    runCatching { repository.saveTripType(type) }
                        .onSuccess {
                            successMessage = if (type.id == 0L) "Trip type added." else "Trip type updated."
                            errorMessage = null
                            showTripDialog = false
                            editingTrip = null
                        }
                        .onFailure {
                            errorMessage = it.message
                            successMessage = null
                        }
                }
            },
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Type Management") },
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
                        Text("Catalogs", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text("Manage the local master lists for fuel types, services, expenses, and trip types.")
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            TypeManagementTab.entries.forEach { tab ->
                                FilterChip(
                                    selected = selectedTab == tab,
                                    onClick = { selectedTab = tab },
                                    label = { Text(tab.label) },
                                )
                            }
                        }
                        Button(
                            onClick = {
                                when (selectedTab) {
                                    TypeManagementTab.FUEL -> {
                                        editingFuel = null
                                        showFuelDialog = true
                                    }
                                    TypeManagementTab.SERVICE -> {
                                        editingService = null
                                        showServiceDialog = true
                                    }
                                    TypeManagementTab.EXPENSE -> {
                                        editingExpense = null
                                        showExpenseDialog = true
                                    }
                                    TypeManagementTab.TRIP -> {
                                        editingTrip = null
                                        showTripDialog = true
                                    }
                                }
                            },
                        ) {
                            Text("Add ${selectedTab.label.dropLastWhile { it == 's' }}")
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
            successMessage?.let { message ->
                item {
                    Card(colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                        Text(message, modifier = Modifier.padding(18.dp))
                    }
                }
            }
            when (selectedTab) {
                TypeManagementTab.FUEL -> {
                    if (fuelTypes.isEmpty()) {
                        item { EmptyTypeCard(selectedTab.label) }
                    } else {
                        items(fuelTypes, key = FuelType::id) { item ->
                            TypeRow(
                                title = item.displayName,
                                subtitle = item.notes,
                                onEdit = {
                                    editingFuel = item
                                    showFuelDialog = true
                                },
                                onDelete = { pendingDelete = PendingDelete(TypeManagementTab.FUEL, item.id, item.displayName) },
                            )
                        }
                    }
                }
                TypeManagementTab.SERVICE -> {
                    if (serviceTypes.isEmpty()) {
                        item { EmptyTypeCard(selectedTab.label) }
                    } else {
                        items(serviceTypes, key = ServiceType::id) { item ->
                            val defaults = listOfNotNull(
                                item.defaultTimeReminderMonths.takeIf { it > 0 }?.let { "$it months" },
                                item.defaultDistanceReminder?.let { "${it.toStableString()} distance" },
                            ).joinToString(" • ")
                            TypeRow(
                                title = item.name,
                                subtitle = listOfNotNull(defaults.takeIf { it.isNotBlank() }, item.notes.takeIf { it.isNotBlank() }).joinToString(" • "),
                                onEdit = {
                                    editingService = item
                                    showServiceDialog = true
                                },
                                onDelete = { pendingDelete = PendingDelete(TypeManagementTab.SERVICE, item.id, item.name) },
                            )
                        }
                    }
                }
                TypeManagementTab.EXPENSE -> {
                    if (expenseTypes.isEmpty()) {
                        item { EmptyTypeCard(selectedTab.label) }
                    } else {
                        items(expenseTypes, key = ExpenseType::id) { item ->
                            TypeRow(
                                title = item.name,
                                subtitle = item.notes,
                                onEdit = {
                                    editingExpense = item
                                    showExpenseDialog = true
                                },
                                onDelete = { pendingDelete = PendingDelete(TypeManagementTab.EXPENSE, item.id, item.name) },
                            )
                        }
                    }
                }
                TypeManagementTab.TRIP -> {
                    if (tripTypes.isEmpty()) {
                        item { EmptyTypeCard(selectedTab.label) }
                    } else {
                        items(tripTypes, key = TripType::id) { item ->
                            val summary = listOfNotNull(
                                item.defaultTaxDeductionRate?.let { "Default tax ${it.toStableString()}" },
                                item.notes.takeIf { it.isNotBlank() },
                            ).joinToString(" • ")
                            TypeRow(
                                title = item.name,
                                subtitle = summary,
                                onEdit = {
                                    editingTrip = item
                                    showTripDialog = true
                                },
                                onDelete = { pendingDelete = PendingDelete(TypeManagementTab.TRIP, item.id, item.name) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyTypeCard(label: String) {
    Card {
        Text("No $label yet.", modifier = Modifier.padding(18.dp))
    }
}

@Composable
private fun TypeRow(
    title: String,
    subtitle: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Card {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            if (subtitle.isNotBlank()) {
                Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TextButton(onClick = onEdit) { Text("Edit") }
                TextButton(onClick = onDelete) { Text("Delete") }
            }
        }
    }
}

@Composable
private fun FuelTypeDialog(
    initial: FuelType?,
    onDismiss: () -> Unit,
    onSave: (FuelType) -> Unit,
) {
    var category by remember(initial) { mutableStateOf(initial?.category.orEmpty()) }
    var grade by remember(initial) { mutableStateOf(initial?.grade.orEmpty()) }
    var octaneText by remember(initial) { mutableStateOf(initial?.octane?.toString().orEmpty()) }
    var cetaneText by remember(initial) { mutableStateOf(initial?.cetane?.toString().orEmpty()) }
    var notes by remember(initial) { mutableStateOf(initial?.notes.orEmpty()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Add Fuel Type" else "Edit Fuel Type") },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(
                        FuelType(
                            id = initial?.id ?: 0L,
                            legacySourceId = initial?.legacySourceId,
                            category = category.trim(),
                            grade = grade.trim(),
                            octane = octaneText.toIntOrNull(),
                            cetane = cetaneText.toIntOrNull(),
                            notes = notes.trim(),
                        ),
                    )
                },
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("Category") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = grade, onValueChange = { grade = it }, label = { Text("Grade") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = octaneText, onValueChange = { octaneText = it }, label = { Text("Octane (optional)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = cetaneText, onValueChange = { cetaneText = it }, label = { Text("Cetane (optional)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Notes") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
            }
        },
    )
}

@Composable
private fun ServiceTypeDialog(
    initial: ServiceType?,
    onDismiss: () -> Unit,
    onSave: (ServiceType) -> Unit,
) {
    var name by remember(initial) { mutableStateOf(initial?.name.orEmpty()) }
    var notes by remember(initial) { mutableStateOf(initial?.notes.orEmpty()) }
    var monthsText by remember(initial) { mutableStateOf(initial?.defaultTimeReminderMonths?.toString().orEmpty()) }
    var distanceText by remember(initial) { mutableStateOf(initial?.defaultDistanceReminder?.toString().orEmpty()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Add Service Type" else "Edit Service Type") },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(
                        ServiceType(
                            id = initial?.id ?: 0L,
                            legacySourceId = initial?.legacySourceId,
                            name = name.trim(),
                            notes = notes.trim(),
                            defaultTimeReminderMonths = monthsText.toIntOrNull() ?: 0,
                            defaultDistanceReminder = distanceText.toDoubleOrNull(),
                        ),
                    )
                },
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = monthsText, onValueChange = { monthsText = it }, label = { Text("Default Reminder Months") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = distanceText, onValueChange = { distanceText = it }, label = { Text("Default Reminder Distance") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Notes") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
            }
        },
    )
}

@Composable
private fun ExpenseTypeDialog(
    initial: ExpenseType?,
    onDismiss: () -> Unit,
    onSave: (ExpenseType) -> Unit,
) {
    var name by remember(initial) { mutableStateOf(initial?.name.orEmpty()) }
    var notes by remember(initial) { mutableStateOf(initial?.notes.orEmpty()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Add Expense Type" else "Edit Expense Type") },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(
                        ExpenseType(
                            id = initial?.id ?: 0L,
                            legacySourceId = initial?.legacySourceId,
                            name = name.trim(),
                            notes = notes.trim(),
                        ),
                    )
                },
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Notes") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
            }
        },
    )
}

@Composable
private fun TripTypeDialog(
    initial: TripType?,
    onDismiss: () -> Unit,
    onSave: (TripType) -> Unit,
) {
    var name by remember(initial) { mutableStateOf(initial?.name.orEmpty()) }
    var rateText by remember(initial) { mutableStateOf(initial?.defaultTaxDeductionRate?.toString().orEmpty()) }
    var notes by remember(initial) { mutableStateOf(initial?.notes.orEmpty()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Add Trip Type" else "Edit Trip Type") },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(
                        TripType(
                            id = initial?.id ?: 0L,
                            legacySourceId = initial?.legacySourceId,
                            name = name.trim(),
                            defaultTaxDeductionRate = rateText.toDoubleOrNull(),
                            notes = notes.trim(),
                        ),
                    )
                },
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = rateText, onValueChange = { rateText = it }, label = { Text("Default Tax Deduction Rate") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Notes") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
            }
        },
    )
}
