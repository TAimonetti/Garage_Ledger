package com.garageledger.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.garageledger.data.GarageRepository
import com.garageledger.domain.model.AppPreferenceSnapshot
import com.garageledger.domain.model.VehiclePredictionSummary

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PredictionsScreen(
    repository: GarageRepository,
    preselectedVehicleId: Long? = null,
    onBack: () -> Unit,
) {
    val vehicles by repository.observeVehicles().collectAsStateWithLifecycle(initialValue = emptyList())
    val preferences by repository.preferences.collectAsStateWithLifecycle(initialValue = AppPreferenceSnapshot())
    var selectedVehicleId by rememberSaveable(preselectedVehicleId) { mutableLongStateOf(preselectedVehicleId ?: 0L) }

    LaunchedEffect(vehicles, preselectedVehicleId) {
        if (selectedVehicleId <= 0L) {
            selectedVehicleId = preselectedVehicleId
                ?: vehicles.firstOrNull { it.lifecycle.name == "ACTIVE" }?.id
                ?: vehicles.firstOrNull()?.id
                ?: 0L
        }
    }

    val predictions by repository.observePredictions(selectedVehicleId.takeIf { it > 0L })
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val prediction = predictions.firstOrNull()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Predictions") },
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
                    androidx.compose.foundation.layout.Column(
                        Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text("Predictions", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("These local estimates are derived from your fill-up cadence, recorded odometer span, and overall operating costs.")
                        if (vehicles.isNotEmpty()) {
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                vehicles.forEach { vehicle ->
                                    FilterChip(
                                        selected = selectedVehicleId == vehicle.id,
                                        onClick = { selectedVehicleId = vehicle.id },
                                        label = { Text(vehicle.name) },
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (prediction == null) {
                item {
                    Card {
                        androidx.compose.foundation.layout.Column(
                            Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text("Not enough data yet.")
                            Text("Import or add fill-ups and operating costs to populate local predictions for this vehicle.")
                        }
                    }
                }
            } else {
                item {
                    PredictionSummaryCard(
                        prediction = prediction,
                        preferences = preferences,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PredictionSummaryCard(
    prediction: VehiclePredictionSummary,
    preferences: AppPreferenceSnapshot,
) {
    val nextDateLabel = prediction.nextFillUpDateTime?.formatForDisplay(preferences) ?: "n/a"
    val nextOdometerLabel = prediction.nextFillUpOdometerReading?.let {
        "${it.toStableString()} ${prediction.distanceUnitLabel}"
    } ?: "n/a"
    val rangeLabel = prediction.carRange?.let {
        "${it.formatOneDecimal()} ${prediction.distanceUnitLabel}"
    } ?: "n/a"
    val perDayLabel = prediction.tripCostPerDay?.let {
        it.asCurrency(prediction.currencySymbol)
    } ?: "n/a"
    val perDistanceLabel = prediction.tripCostPerDistanceUnit?.let {
        "${it.asCurrency(prediction.currencySymbol)}/${prediction.distanceUnitLabel}"
    } ?: "n/a"
    val per100Label = prediction.tripCostPer100DistanceUnit?.let {
        "${it.asCurrency(prediction.currencySymbol)}/100${prediction.distanceUnitLabel}"
    } ?: "n/a"

    Card {
        androidx.compose.foundation.layout.Column(
            Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(prediction.vehicleName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text("Next fuel-up", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text("Date: $nextDateLabel")
            Text("Odometer: $nextOdometerLabel")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SummaryChip("Range", rangeLabel)
                SummaryChip("Cost / Day", perDayLabel)
                SummaryChip("Cost / ${prediction.distanceUnitLabel}", perDistanceLabel)
                SummaryChip("Cost / 100${prediction.distanceUnitLabel}", per100Label)
            }
        }
    }
}
