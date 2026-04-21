package com.garageledger.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.garageledger.core.model.DistanceUnit
import com.garageledger.core.model.VolumeUnit
import com.garageledger.domain.model.FuellyCsvPreview
import com.garageledger.domain.model.FuellyImportField
import com.garageledger.domain.model.ImportIssue
import com.garageledger.domain.model.Vehicle

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun FuellyImportCard(
    vehicles: List<Vehicle>,
    fileLabel: String?,
    preview: FuellyCsvPreview?,
    selectedVehicleId: Long?,
    onVehicleSelected: (Long) -> Unit,
    fieldMapping: Map<FuellyImportField, String?>,
    onFieldMapped: (FuellyImportField, String?) -> Unit,
    distanceUnit: DistanceUnit,
    onDistanceUnitSelected: (DistanceUnit) -> Unit,
    volumeUnit: VolumeUnit,
    onVolumeUnitSelected: (VolumeUnit) -> Unit,
    validationErrors: List<String>,
    onPickFile: () -> Unit,
    onImport: () -> Unit,
) {
    Card {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Fuelly CSV", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("Preview a Fuelly-style CSV, map its columns locally, and import fill-ups into an existing vehicle.")
            if (vehicles.isEmpty()) {
                Text("Add a vehicle before importing Fuelly fill-ups.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Button(onClick = onPickFile) {
                Icon(Icons.Outlined.FileUpload, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text(if (preview == null) "Choose Fuelly CSV" else "Choose Another Fuelly CSV")
            }

            preview?.let { currentPreview ->
                fileLabel?.takeIf { it.isNotBlank() }?.let { label ->
                    Text("Selected file: $label", style = MaterialTheme.typography.bodyMedium)
                }
                Text(
                    "${currentPreview.headers.size} columns detected across ${currentPreview.sampleRows.size} preview rows.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                VehicleSelector(
                    vehicles = vehicles,
                    selectedVehicleId = selectedVehicleId,
                    onVehicleSelected = onVehicleSelected,
                )

                ImportUnitSection(
                    title = "Distance Unit",
                    options = DistanceUnit.entries,
                    selected = distanceUnit,
                    onSelect = onDistanceUnitSelected,
                    label = ::distanceUnitLabel,
                )
                ImportUnitSection(
                    title = "Volume Unit",
                    options = listOf(VolumeUnit.GALLONS_US, VolumeUnit.LITERS),
                    selected = volumeUnit,
                    onSelect = onVolumeUnitSelected,
                    label = ::volumeUnitLabel,
                )

                Text("Column Mapping", fontWeight = FontWeight.SemiBold)
                FuellyImportField.entries.forEach { field ->
                    FieldMappingRow(
                        field = field,
                        headers = currentPreview.headers,
                        selectedHeader = fieldMapping[field],
                        onHeaderSelected = { onFieldMapped(field, it) },
                    )
                }

                if (currentPreview.sampleRows.isNotEmpty()) {
                    Text("Sample Rows", fontWeight = FontWeight.SemiBold)
                    currentPreview.sampleRows.forEach { row ->
                        Card {
                            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Row ${row.rowNumber}", fontWeight = FontWeight.Medium)
                                FuellyImportField.entries.forEach { field ->
                                    val mappedHeader = fieldMapping[field] ?: return@forEach
                                    val value = row.values[mappedHeader].orEmpty()
                                    Text("${field.label}: ${value.ifBlank { "-" }}")
                                }
                            }
                        }
                    }
                }

                val previewIssues = currentPreview.issues.filterNot { it.severity == ImportIssue.Severity.INFO }
                if (previewIssues.isNotEmpty() || validationErrors.isNotEmpty()) {
                    Text("Import Notes", fontWeight = FontWeight.SemiBold)
                    previewIssues.forEach { issue ->
                        Text(formatIssue(issue))
                    }
                    validationErrors.forEach { error ->
                        Text(error)
                    }
                }

                Button(
                    onClick = onImport,
                    enabled = vehicles.isNotEmpty() && selectedVehicleId != null && validationErrors.isEmpty(),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Import Fuelly Fill-Ups")
                }
            }
        }
    }
}

@Composable
private fun VehicleSelector(
    vehicles: List<Vehicle>,
    selectedVehicleId: Long?,
    onVehicleSelected: (Long) -> Unit,
) {
    if (vehicles.isEmpty()) return
    var expanded by remember { mutableStateOf(false) }
    val selectedVehicle = vehicles.firstOrNull { it.id == selectedVehicleId }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Import Into Vehicle", fontWeight = FontWeight.Medium)
        Box {
            TextButton(onClick = { expanded = true }) {
                Text(selectedVehicle?.name ?: "Choose Vehicle")
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                vehicles.forEach { vehicle ->
                    DropdownMenuItem(
                        text = { Text(vehicle.name) },
                        onClick = {
                            onVehicleSelected(vehicle.id)
                            expanded = false
                        },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> ImportUnitSection(
    title: String,
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    label: (T) -> String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, fontWeight = FontWeight.Medium)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
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

@Composable
private fun FieldMappingRow(
    field: FuellyImportField,
    headers: List<String>,
    selectedHeader: String?,
    onHeaderSelected: (String?) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Column {
            Text(field.label, fontWeight = FontWeight.Medium)
            Text(
                if (field.required) "Required" else "Optional",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Box {
            TextButton(onClick = { expanded = true }) {
                Text(selectedHeader ?: "Not mapped")
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                DropdownMenuItem(
                    text = { Text("Not mapped") },
                    onClick = {
                        onHeaderSelected(null)
                        expanded = false
                    },
                )
                headers.forEach { header ->
                    DropdownMenuItem(
                        text = { Text(header) },
                        onClick = {
                            onHeaderSelected(header)
                            expanded = false
                        },
                    )
                }
            }
        }
    }
}

private fun distanceUnitLabel(unit: DistanceUnit): String = when (unit) {
    DistanceUnit.MILES -> "Miles"
    DistanceUnit.KILOMETERS -> "Kilometers"
}

private fun volumeUnitLabel(unit: VolumeUnit): String = when (unit) {
    VolumeUnit.GALLONS_US -> "Gallons (US)"
    VolumeUnit.GALLONS_UK -> "Gallons (UK)"
    VolumeUnit.LITERS -> "Liters"
}

private fun formatIssue(issue: ImportIssue): String = buildString {
    append(issue.section.ifBlank { "Import" })
    issue.rowNumber?.let { append(" row $it") }
    append(": ")
    append(issue.message)
}
