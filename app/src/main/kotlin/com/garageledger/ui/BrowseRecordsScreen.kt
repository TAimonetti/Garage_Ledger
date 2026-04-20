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
import com.garageledger.domain.model.BrowseRecordItem
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

    val filteredRecords = remember(records, selectedVehicleId, selectedFamily, queryText, tagText, fromDateText, toDateText) {
        val normalizedQuery = queryText.trim().lowercase()
        val normalizedTag = tagText.trim().lowercase()
        val fromDate = parseFilterDate(fromDateText)
        val toDate = parseFilterDate(toDateText)
        records.filter { item ->
            (selectedVehicleId == 0L || item.vehicleId == selectedVehicleId) &&
                (selectedFamily == null || item.family == selectedFamily) &&
                (normalizedQuery.isBlank() || item.searchText.contains(normalizedQuery)) &&
                (normalizedTag.isBlank() || item.tags.any { it.lowercase().contains(normalizedTag) }) &&
                (fromDate == null || !item.occurredAt.toLocalDate().isBefore(fromDate)) &&
                (toDate == null || !item.occurredAt.toLocalDate().isAfter(toDate))
        }
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
                        OutlinedTextField(
                            value = tagText,
                            onValueChange = { tagText = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Tag contains") },
                        )
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
                                "${item.vehicleName} • ${item.family.displayLabel()} • ${
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
                                Text(metadata.joinToString(" • "), color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}
