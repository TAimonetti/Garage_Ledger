package com.garageledger.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.garageledger.data.GarageRepository
import com.garageledger.domain.model.CostStatisticsSummary
import com.garageledger.domain.model.FillUpStatisticsSummary
import com.garageledger.domain.model.StatisticsChart
import com.garageledger.domain.model.StatisticsChartStyle
import com.garageledger.domain.model.StatisticsDashboard
import com.garageledger.domain.model.StatisticsFilter
import com.garageledger.domain.model.StatisticsTimeframe
import com.garageledger.domain.model.TripStatisticsSummary
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun StatisticsScreen(
    repository: GarageRepository,
    preselectedVehicleId: Long? = null,
    onBack: () -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val vehicles by repository.observeVehicles().collectAsStateWithLifecycle(initialValue = emptyList())

    var selectedVehicleId by rememberSaveable { mutableLongStateOf(preselectedVehicleId ?: 0L) }
    var vehicleMenuExpanded by remember { mutableStateOf(false) }
    var timeframe by rememberSaveable { mutableStateOf(StatisticsTimeframe.ALL_TIME) }
    var exportMessage by remember { mutableStateOf<String?>(null) }
    var exportError by remember { mutableStateOf<String?>(null) }

    val dashboardFlow = remember(repository, selectedVehicleId, timeframe) {
        repository.observeStatisticsDashboard(
            vehicleId = selectedVehicleId.takeIf { it > 0L },
            timeframe = timeframe,
        )
    }
    val dashboard by dashboardFlow.collectAsStateWithLifecycle(initialValue = null)

    fun runExport(
        uri: Uri?,
        successMessage: String,
        exporter: suspend (java.io.OutputStream) -> Unit,
    ) {
        if (uri == null) return
        scope.launch {
            runCatching {
                val stream = context.contentResolver.openOutputStream(uri)
                    ?: error("Unable to create the selected statistics export file.")
                stream.use { outputStream ->
                    exporter(outputStream)
                }
            }.onSuccess {
                exportMessage = successMessage
                exportError = null
            }.onFailure { error ->
                exportError = error.message
            }
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        runExport(
            uri = uri,
            successMessage = "Statistics CSV export saved.",
            exporter = {
                repository.exportStatisticsCsv(
                    outputStream = it,
                    filter = StatisticsFilter(
                        vehicleId = selectedVehicleId.takeIf { id -> id > 0L },
                        timeframe = timeframe,
                    ),
                )
            },
        )
    }
    val htmlExportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/html")) { uri ->
        runExport(
            uri = uri,
            successMessage = "Statistics HTML export saved.",
            exporter = {
                repository.exportStatisticsHtml(
                    outputStream = it,
                    filter = StatisticsFilter(
                        vehicleId = selectedVehicleId.takeIf { id -> id > 0L },
                        timeframe = timeframe,
                    ),
                )
            },
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Statistics & Charts") },
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
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text("Filters", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Box {
                            TextButton(onClick = { vehicleMenuExpanded = true }) {
                                Text(vehicles.firstOrNull { it.id == selectedVehicleId }?.name ?: "All Vehicles")
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
                            StatisticsTimeframe.entries.forEach { option ->
                                FilterChip(
                                    selected = timeframe == option,
                                    onClick = { timeframe = option },
                                    label = { Text(option.label) },
                                )
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                dashboard?.let { "${it.scopeLabel} | ${it.periodDescription}" } ?: "Preparing statistics...",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = { exportLauncher.launch("garage-ledger-statistics.csv") }) {
                                    Text("Export CSV")
                                }
                                Button(onClick = { htmlExportLauncher.launch("garage-ledger-statistics.html") }) {
                                    Text("Export HTML")
                                }
                            }
                        }
                    }
                }
            }
            exportError?.let { message ->
                item {
                    Card(colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                        Text(message, modifier = Modifier.padding(18.dp))
                    }
                }
            }
            exportMessage?.let { message ->
                item {
                    Card(colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                        Text(message, modifier = Modifier.padding(18.dp))
                    }
                }
            }
            dashboard?.let { report ->
                item { OverallSummaryCard(report) }
                item { FillUpSummaryCard(report) }
                item { CostSummaryCard("Services", report.services, report.preferences.currencySymbol) }
                item { CostSummaryCard("Expenses", report.expenses, report.preferences.currencySymbol) }
                item { TripSummaryCard(report.trips, report) }
                items(report.charts.size) { index ->
                    StatisticsChartCard(
                        chart = report.charts[index],
                        currencySymbol = report.preferences.currencySymbol,
                    )
                }
            } ?: item {
                Card {
                    Text("Loading statistics...", modifier = Modifier.padding(18.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OverallSummaryCard(report: StatisticsDashboard) {
    Card {
        androidx.compose.foundation.layout.Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Overall", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SummaryChip("Vehicles", report.overall.vehicleCount.toString())
                SummaryChip("Records", report.overall.totalRecordCount.toString())
                SummaryChip("Fuel-Ups", report.overall.fillUpCount.toString())
                SummaryChip("Services", report.overall.serviceCount.toString())
                SummaryChip("Expenses", report.overall.expenseCount.toString())
                SummaryChip("Trips", report.overall.tripCount.toString())
                SummaryChip("Operating Cost", report.overall.totalOperatingCost.asCurrency(report.preferences.currencySymbol))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FillUpSummaryCard(report: StatisticsDashboard) {
    val fillUps = report.fillUps
    Card {
        androidx.compose.foundation.layout.Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Fill-Ups", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SummaryChip("Count", fillUps.count.toString())
                SummaryChip("Volume", "${fillUps.totalVolume.toStableString()} ${report.preferences.volumeUnit.storageValue}")
                SummaryChip("Fuel Cost", fillUps.totalCost.asCurrency(report.preferences.currencySymbol))
                SummaryChip("Distance", "${fillUps.totalDistance.toStableString()} ${report.preferences.distanceUnit.storageValue}")
                SummaryChip(
                    "Avg Efficiency",
                    fillUps.averageFuelEfficiency?.formatOneDecimal()?.plus(" ${report.preferences.fuelEfficiencyUnit.storageValue}") ?: "n/a",
                )
                SummaryChip(
                    "Last Efficiency",
                    fillUps.lastFuelEfficiency?.formatOneDecimal()?.plus(" ${report.preferences.fuelEfficiencyUnit.storageValue}") ?: "n/a",
                )
                SummaryChip(
                    "Avg Price",
                    fillUps.averagePricePerUnit?.let {
                        "${it.toStableString()} ${report.preferences.currencySymbol}/${report.preferences.volumeUnit.storageValue}"
                    } ?: "n/a",
                )
                SummaryChip(
                    "Avg Fill-Up",
                    fillUps.averageCostPerFillUp?.asCurrency(report.preferences.currencySymbol) ?: "n/a",
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CostSummaryCard(
    title: String,
    summary: CostStatisticsSummary,
    currencySymbol: String,
) {
    Card {
        androidx.compose.foundation.layout.Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SummaryChip("Count", summary.count.toString())
                SummaryChip("Total Cost", summary.totalCost.asCurrency(currencySymbol))
                SummaryChip("Average Cost", summary.averageCost?.asCurrency(currencySymbol) ?: "n/a")
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TripSummaryCard(
    summary: TripStatisticsSummary,
    report: StatisticsDashboard,
) {
    Card {
        androidx.compose.foundation.layout.Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Trips", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SummaryChip("Count", summary.count.toString())
                SummaryChip("Distance", "${summary.totalDistance.toStableString()} ${report.preferences.distanceUnit.storageValue}")
                SummaryChip(
                    "Avg Distance",
                    summary.averageDistance?.toStableString()?.plus(" ${report.preferences.distanceUnit.storageValue}") ?: "n/a",
                )
                SummaryChip("Reimbursement", summary.totalReimbursement.asCurrency(report.preferences.currencySymbol))
                SummaryChip("Tax Deduction", summary.totalTaxDeduction.asCurrency(report.preferences.currencySymbol))
                SummaryChip("Paid Trips", summary.paidCount.toString())
                SummaryChip("Open Trips", summary.openCount.toString())
            }
        }
    }
}

@Composable
private fun StatisticsChartCard(
    chart: StatisticsChart,
    currencySymbol: String,
) {
    Card {
        androidx.compose.foundation.layout.Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(chart.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            if (chart.points.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("No records in the current scope.")
                }
            } else {
                StatisticsMiniChart(chart = chart)
                Text(
                    "${chart.points.size} points | ${chart.points.first().label} to ${chart.points.last().label}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "Average: ${chart.points.map { it.value }.average().toDisplayValue(chart.unitLabel, currencySymbol)}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun StatisticsMiniChart(chart: StatisticsChart) {
    val primaryColor = when (chart.style) {
        StatisticsChartStyle.LINE -> MaterialTheme.colorScheme.primary
        StatisticsChartStyle.BAR -> MaterialTheme.colorScheme.tertiary
    }
    val markerColor = MaterialTheme.colorScheme.surface
    val axisColor = MaterialTheme.colorScheme.outline
    val gridColor = MaterialTheme.colorScheme.outlineVariant

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp),
    ) {
        if (chart.points.isEmpty()) return@Canvas

        val left = 18.dp.toPx()
        val right = size.width - 12.dp.toPx()
        val top = 18.dp.toPx()
        val bottom = size.height - 18.dp.toPx()
        val pointCount = chart.points.size
        val minValue = chart.points.minOf { it.value }
        val maxValue = chart.points.maxOf { it.value }
        val range = (maxValue - minValue).takeIf { it > 0.0 } ?: 1.0

        fun xAt(index: Int): Float = if (pointCount == 1) {
            (left + right) / 2f
        } else {
            left + ((right - left) * (index.toFloat() / (pointCount - 1).toFloat()))
        }

        fun yAt(value: Double): Float = bottom - (((value - minValue) / range).toFloat() * (bottom - top))

        repeat(4) { index ->
            val fraction = index / 3f
            val y = bottom - ((bottom - top) * fraction)
            drawLine(
                color = gridColor,
                start = Offset(left, y),
                end = Offset(right, y),
                strokeWidth = 1.dp.toPx(),
            )
        }
        drawLine(
            color = axisColor,
            start = Offset(left, bottom),
            end = Offset(right, bottom),
            strokeWidth = 1.dp.toPx(),
        )

        when (chart.style) {
            StatisticsChartStyle.LINE -> {
                val path = Path()
                chart.points.forEachIndexed { index, point ->
                    val x = xAt(index)
                    val y = yAt(point.value)
                    if (index == 0) {
                        path.moveTo(x, y)
                    } else {
                        path.lineTo(x, y)
                    }
                }
                drawPath(
                    path = path,
                    color = primaryColor,
                    style = Stroke(
                        width = 4.dp.toPx(),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round,
                    ),
                )
                chart.points.forEachIndexed { index, point ->
                    val x = xAt(index)
                    val y = yAt(point.value)
                    drawCircle(
                        color = markerColor,
                        radius = 5.dp.toPx(),
                        center = Offset(x, y),
                    )
                    drawCircle(
                        color = primaryColor,
                        radius = 3.dp.toPx(),
                        center = Offset(x, y),
                    )
                }
            }

            StatisticsChartStyle.BAR -> {
                val slotWidth = if (pointCount == 1) (right - left) else (right - left) / pointCount
                val barWidth = minOf(slotWidth * 0.6f, 26.dp.toPx())
                chart.points.forEachIndexed { index, point ->
                    val x = xAt(index)
                    val y = yAt(point.value)
                    drawRoundRect(
                        color = primaryColor,
                        topLeft = Offset(x - (barWidth / 2f), y),
                        size = Size(barWidth, bottom - y),
                        cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx()),
                    )
                }
            }
        }
    }
}

private fun Double.toDisplayValue(unitLabel: String, currencySymbol: String): String = when {
    unitLabel == currencySymbol -> asCurrency(currencySymbol)
    unitLabel.startsWith("$") || unitLabel.startsWith(currencySymbol) -> "${toStableString()} $unitLabel"
    else -> "${toStableString()} $unitLabel"
}
