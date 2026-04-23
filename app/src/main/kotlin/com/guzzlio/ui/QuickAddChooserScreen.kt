package com.guzzlio.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.guzzlio.data.GarageRepository
import com.guzzlio.shortcuts.QuickActionTarget

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickAddChooserScreen(
    repository: GarageRepository,
    target: QuickActionTarget,
    onBack: () -> Unit,
    onOpenVehicleEditor: (Long) -> Unit,
) {
    val vehicles = repository.observeVehicles().collectAsStateWithLifecycle(initialValue = emptyList()).value

    LaunchedEffect(target, vehicles) {
        if (vehicles.size == 1) {
            onOpenVehicleEditor(vehicles.single().id)
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(target.shortLabel) },
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
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Choose Vehicle", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("Shortcuts stay fast without assuming which vehicle should receive the new record.")
                    }
                }
            }
            if (vehicles.isEmpty()) {
                item {
                    Card {
                        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("No vehicles available.", fontWeight = FontWeight.SemiBold)
                            Text("Import your history first, then the shortcut can jump straight into entry.")
                        }
                    }
                }
            } else {
                items(vehicles.size) { index ->
                    val vehicle = vehicles[index]
                    Card(modifier = Modifier.fillMaxWidth().clickable { onOpenVehicleEditor(vehicle.id) }) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column {
                                Text(vehicle.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                Text(listOfNotNull(vehicle.year?.toString(), vehicle.make, vehicle.model).joinToString(" "))
                            }
                            Text("Open")
                        }
                    }
                }
            }
        }
    }
}
