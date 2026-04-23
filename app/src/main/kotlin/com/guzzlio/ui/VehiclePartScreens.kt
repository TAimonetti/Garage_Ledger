package com.guzzlio.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.guzzlio.R
import com.guzzlio.data.GarageRepository
import com.guzzlio.domain.model.Vehicle
import com.guzzlio.domain.model.VehiclePart
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MoreVert
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehiclePartsScreen(
    repository: GarageRepository,
    vehicleId: Long,
    onBack: () -> Unit,
    onAddPart: () -> Unit,
    onEditPart: (Long) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val vehicle by produceState<Vehicle?>(initialValue = null, key1 = vehicleId) {
        value = repository.getVehicle(vehicleId)
    }
    val parts by repository.observeVehicleParts(vehicleId).collectAsStateWithLifecycle(initialValue = emptyList())
    var deleteTarget by remember { mutableStateOf<VehiclePart?>(null) }

    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Delete Vehicle Part") },
            text = { Text("Delete ${target.name} from this vehicle?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            repository.deleteVehiclePart(target.id)
                            deleteTarget = null
                        }
                    },
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) {
                    Text("Cancel")
                }
            },
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(vehicle?.name?.let { "$it Parts" } ?: "Vehicle Parts") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } },
                actions = {
                    TextButton(onClick = onAddPart) {
                        Text("Add")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (parts.isEmpty()) {
                item {
                    Card {
                        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("No vehicle parts recorded.")
                            Text("Add the parts you want to track for this vehicle.")
                        }
                    }
                }
            } else {
                items(parts.size) { index ->
                    val part = parts[index]
                    CompactVehiclePartRow(
                        part = part,
                        index = index,
                        onEditPart = onEditPart,
                        onDeletePart = { deleteTarget = part },
                    )
                }
            }
        }
    }
}

@Composable
private fun CompactVehiclePartRow(
    part: VehiclePart,
    index: Int,
    onEditPart: (Long) -> Unit,
    onDeletePart: () -> Unit,
) {
    var menuExpanded by remember(part.id) { mutableStateOf(false) }
    val rowColor = if (index % 2 == 0) {
        MaterialTheme.colorScheme.surface
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
    }
    val description = listOfNotNull(
        part.type.takeIf { it.isNotBlank() },
        part.brand.takeIf { it.isNotBlank() },
        part.partNumber.takeIf { it.isNotBlank() },
        part.notes.takeIf { it.isNotBlank() },
    ).joinToString(" | ")
    val specLeft = listOfNotNull(
        part.size.takeIf { it.isNotBlank() },
        part.quantity?.let { "Qty $it" },
    ).joinToString(" | ")
    val specRight = listOfNotNull(
        part.volume?.let { "Vol ${it.toStableString()}" },
        part.pressure?.let { "PSI ${it.toStableString()}" },
    ).joinToString(" | ")

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = rowColor,
        shape = MaterialTheme.shapes.small,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.console_icon_parts),
                contentDescription = "Part",
                modifier = Modifier.size(22.dp),
                tint = androidx.compose.ui.graphics.Color.Unspecified,
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    part.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (description.isNotBlank()) {
                    Text(
                        description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        specLeft,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        specRight,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Box {
                IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(Icons.Outlined.MoreVert, contentDescription = "Part actions")
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                ) {
                    DropdownMenuItem(
                        text = { Text("Edit") },
                        onClick = {
                            menuExpanded = false
                            onEditPart(part.id)
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Delete") },
                        onClick = {
                            menuExpanded = false
                            onDeletePart()
                        },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehiclePartEditorScreen(
    repository: GarageRepository,
    vehicleId: Long,
    partId: Long,
    onBack: () -> Unit,
    onSaved: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val vehicle by produceState<Vehicle?>(initialValue = null, key1 = vehicleId) {
        value = repository.getVehicle(vehicleId)
    }
    val existingPart by produceState<VehiclePart?>(initialValue = null, key1 = partId) {
        value = if (partId > 0L) repository.getVehiclePart(partId) else null
    }

    var initialized by rememberSaveable(partId) { mutableStateOf(false) }
    var name by rememberSaveable { mutableStateOf("") }
    var partNumber by rememberSaveable { mutableStateOf("") }
    var type by rememberSaveable { mutableStateOf("") }
    var brand by rememberSaveable { mutableStateOf("") }
    var color by rememberSaveable { mutableStateOf("") }
    var size by rememberSaveable { mutableStateOf("") }
    var volumeText by rememberSaveable { mutableStateOf("") }
    var pressureText by rememberSaveable { mutableStateOf("") }
    var quantityText by rememberSaveable { mutableStateOf("") }
    var notes by rememberSaveable { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(existingPart) {
        if (initialized) return@LaunchedEffect
        existingPart?.let { part ->
            name = part.name
            partNumber = part.partNumber
            type = part.type
            brand = part.brand
            color = part.color
            size = part.size
            volumeText = part.volume?.toStableString().orEmpty()
            pressureText = part.pressure?.toStableString().orEmpty()
            quantityText = part.quantity?.toString().orEmpty()
            notes = part.notes
        }
        initialized = true
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(if (partId > 0L) "Edit Vehicle Part" else "Add Vehicle Part") },
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
                        Text(vehicle?.name ?: "Vehicle", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("Capture part details for later service reference and migration parity.")
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
                            label = { Text("Part Name") },
                            singleLine = true,
                        )
                        OutlinedTextField(
                            value = partNumber,
                            onValueChange = { partNumber = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Part Number") },
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
                            value = brand,
                            onValueChange = { brand = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Brand") },
                            singleLine = true,
                        )
                        OutlinedTextField(
                            value = color,
                            onValueChange = { color = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Color") },
                            singleLine = true,
                        )
                        OutlinedTextField(
                            value = size,
                            onValueChange = { size = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Size") },
                            singleLine = true,
                        )
                        OutlinedTextField(
                            value = volumeText,
                            onValueChange = { volumeText = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Volume") },
                            singleLine = true,
                        )
                        OutlinedTextField(
                            value = pressureText,
                            onValueChange = { pressureText = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Pressure") },
                            singleLine = true,
                        )
                        OutlinedTextField(
                            value = quantityText,
                            onValueChange = { quantityText = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Quantity") },
                            singleLine = true,
                        )
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
                                repository.saveVehiclePart(
                                    VehiclePart(
                                        id = existingPart?.id ?: 0L,
                                        legacySourceId = existingPart?.legacySourceId,
                                        vehicleId = vehicleId,
                                        name = name.trim(),
                                        partNumber = partNumber.trim(),
                                        type = type.trim(),
                                        brand = brand.trim(),
                                        color = color.trim(),
                                        size = size.trim(),
                                        volume = volumeText.trim().toDoubleOrNull(),
                                        pressure = pressureText.trim().toDoubleOrNull(),
                                        quantity = quantityText.trim().toIntOrNull(),
                                        notes = notes.trim(),
                                    ),
                                )
                            }.onSuccess {
                                errorMessage = null
                                onSaved()
                            }.onFailure {
                                errorMessage = it.message
                            }
                        }
                    },
                ) {
                    Text(if (partId > 0L) "Save Part" else "Add Part")
                }
            }
        }
    }
}
