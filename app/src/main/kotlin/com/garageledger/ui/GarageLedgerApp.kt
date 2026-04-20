package com.garageledger.ui

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.LocalGasStation
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.garageledger.data.GarageRepository
import com.garageledger.domain.model.ImportReport
import com.garageledger.domain.model.Vehicle
import com.garageledger.domain.model.VehicleDetailBundle
import java.io.InputStream
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.launch

@Composable
fun GarageLedgerApp(
    repository: GarageRepository,
) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "console") {
        composable("console") {
            ConsoleScreen(
                repository = repository,
                onOpenImport = { navController.navigate("import") },
                onOpenVehicles = { navController.navigate("vehicles") },
                onOpenBrowse = { navController.navigate("browse/-1") },
                onOpenVehicle = { navController.navigate("vehicle/$it") },
                onAddFuelUp = { navController.navigate("fuelup/$it/-1") },
                onAddService = { navController.navigate("service/$it/-1") },
                onAddExpense = { navController.navigate("expense/$it/-1") },
                onAddTrip = { navController.navigate("trip/$it/-1") },
            )
        }
        composable("import") {
            ImportScreen(
                repository = repository,
                onBack = { navController.popBackStack() },
            )
        }
        composable("vehicles") {
            VehiclesScreen(
                repository = repository,
                onBack = { navController.popBackStack() },
                onOpenVehicle = { navController.navigate("vehicle/$it") },
            )
        }
        composable(
            route = "vehicle/{vehicleId}",
            arguments = listOf(navArgument("vehicleId") { type = NavType.LongType }),
        ) { backStackEntry ->
            val vehicleId = backStackEntry.arguments?.getLong("vehicleId") ?: 0L
            VehicleDetailScreen(
                repository = repository,
                vehicleId = vehicleId,
                onBack = { navController.popBackStack() },
                onBrowseRecords = { navController.navigate("browse/$vehicleId") },
                onEditFuelUp = { navController.navigate("fuelup/$vehicleId/$it") },
                onEditService = { navController.navigate("service/$vehicleId/$it") },
                onEditExpense = { navController.navigate("expense/$vehicleId/$it") },
                onEditTrip = { navController.navigate("trip/$vehicleId/$it") },
                onAddFuelUp = { navController.navigate("fuelup/$vehicleId/-1") },
                onAddService = { navController.navigate("service/$vehicleId/-1") },
                onAddExpense = { navController.navigate("expense/$vehicleId/-1") },
                onAddTrip = { navController.navigate("trip/$vehicleId/-1") },
            )
        }
        composable(
            route = "browse/{vehicleId}",
            arguments = listOf(navArgument("vehicleId") { type = NavType.LongType }),
        ) { backStackEntry ->
            val vehicleId = backStackEntry.arguments?.getLong("vehicleId") ?: -1L
            BrowseRecordsScreen(
                repository = repository,
                preselectedVehicleId = vehicleId.takeIf { it > 0L },
                onBack = { navController.popBackStack() },
                onOpenRecord = { item ->
                    val route = when (item.family) {
                        com.garageledger.domain.model.RecordFamily.FILL_UP -> "fuelup/${item.vehicleId}/${item.recordId}"
                        com.garageledger.domain.model.RecordFamily.SERVICE -> "service/${item.vehicleId}/${item.recordId}"
                        com.garageledger.domain.model.RecordFamily.EXPENSE -> "expense/${item.vehicleId}/${item.recordId}"
                        com.garageledger.domain.model.RecordFamily.TRIP -> "trip/${item.vehicleId}/${item.recordId}"
                    }
                    navController.navigate(route)
                },
            )
        }
        composable(
            route = "fuelup/{vehicleId}/{recordId}",
            arguments = listOf(
                navArgument("vehicleId") { type = NavType.LongType },
                navArgument("recordId") { type = NavType.LongType },
            ),
        ) { backStackEntry ->
            FuelUpEditorScreen(
                repository = repository,
                vehicleId = backStackEntry.arguments?.getLong("vehicleId") ?: 0L,
                recordId = backStackEntry.arguments?.getLong("recordId") ?: -1L,
                onBack = { navController.popBackStack() },
            )
        }
        composable(
            route = "service/{vehicleId}/{recordId}",
            arguments = listOf(
                navArgument("vehicleId") { type = NavType.LongType },
                navArgument("recordId") { type = NavType.LongType },
            ),
        ) { backStackEntry ->
            ServiceEditorScreen(
                repository = repository,
                vehicleId = backStackEntry.arguments?.getLong("vehicleId") ?: 0L,
                recordId = backStackEntry.arguments?.getLong("recordId") ?: -1L,
                onBack = { navController.popBackStack() },
            )
        }
        composable(
            route = "expense/{vehicleId}/{recordId}",
            arguments = listOf(
                navArgument("vehicleId") { type = NavType.LongType },
                navArgument("recordId") { type = NavType.LongType },
            ),
        ) { backStackEntry ->
            ExpenseEditorScreen(
                repository = repository,
                vehicleId = backStackEntry.arguments?.getLong("vehicleId") ?: 0L,
                recordId = backStackEntry.arguments?.getLong("recordId") ?: -1L,
                onBack = { navController.popBackStack() },
            )
        }
        composable(
            route = "trip/{vehicleId}/{recordId}",
            arguments = listOf(
                navArgument("vehicleId") { type = NavType.LongType },
                navArgument("recordId") { type = NavType.LongType },
            ),
        ) { backStackEntry ->
            TripEditorScreen(
                repository = repository,
                vehicleId = backStackEntry.arguments?.getLong("vehicleId") ?: 0L,
                recordId = backStackEntry.arguments?.getLong("recordId") ?: -1L,
                onBack = { navController.popBackStack() },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConsoleScreen(
    repository: GarageRepository,
    onOpenImport: () -> Unit,
    onOpenVehicles: () -> Unit,
    onOpenBrowse: () -> Unit,
    onOpenVehicle: (Long) -> Unit,
    onAddFuelUp: (Long) -> Unit,
    onAddService: (Long) -> Unit,
    onAddExpense: (Long) -> Unit,
    onAddTrip: (Long) -> Unit,
) {
    val vehicles by repository.observeVehicles().collectAsStateWithLifecycle(initialValue = emptyList())
    var selectedVehicleId by rememberSaveable { mutableLongStateOf(0L) }
    var menuExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(vehicles) {
        if (selectedVehicleId == 0L && vehicles.isNotEmpty()) {
            selectedVehicleId = vehicles.first().id
        }
    }

    val selectedVehicle = vehicles.firstOrNull { it.id == selectedVehicleId } ?: vehicles.firstOrNull()
    val detailFlow = remember(selectedVehicle?.id) {
        selectedVehicle?.id?.let(repository::observeVehicleDetail)
    }
    val detail by (detailFlow?.collectAsStateWithLifecycle(initialValue = null)
        ?: remember { mutableStateOf<VehicleDetailBundle?>(null) })

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Garage Ledger") },
                actions = {
                    IconButton(onClick = onOpenImport) {
                        Icon(Icons.Outlined.FileUpload, contentDescription = "Import")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text("Console", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Text("Dashboard-first, local-first, and ready for import parity.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Box {
                            TextButton(onClick = { menuExpanded = true }) {
                                Icon(Icons.Outlined.DirectionsCar, contentDescription = null)
                                Spacer(Modifier.size(8.dp))
                                Text(selectedVehicle?.name ?: "Choose Vehicle")
                            }
                            DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                                vehicles.forEach { vehicle ->
                                    DropdownMenuItem(
                                        text = { Text(vehicle.name) },
                                        onClick = {
                                            selectedVehicleId = vehicle.id
                                            menuExpanded = false
                                        },
                                    )
                                }
                            }
                        }
                        if (detail != null) {
                            StatsRow(detail = detail!!)
                        } else {
                            Text("Import data or add a vehicle to get started.")
                        }
                    }
                }
            }
            item {
                ActionGrid(
                    hasSelectedVehicle = selectedVehicle != null,
                    onOpenImport = onOpenImport,
                    onOpenVehicles = onOpenVehicles,
                    onOpenBrowse = onOpenBrowse,
                    onOpenVehicle = { selectedVehicle?.id?.let(onOpenVehicle) },
                    onAddFuelUp = { selectedVehicle?.id?.let(onAddFuelUp) },
                    onAddService = { selectedVehicle?.id?.let(onAddService) },
                    onAddExpense = { selectedVehicle?.id?.let(onAddExpense) },
                    onAddTrip = { selectedVehicle?.id?.let(onAddTrip) },
                )
            }
            if (detail != null) {
                item {
                    Card {
                        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Recent Fill-Ups", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            if (detail!!.recentFillUps.isEmpty()) {
                                Text("No fuel-ups yet.")
                            } else {
                                detail!!.recentFillUps.take(6).forEach { record ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { onOpenVehicle(detail!!.vehicle.id) },
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                    ) {
                                        Column {
                                            Text(record.dateTime.format(DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm")))
                                            Text("${record.odometerReading.toInt()} ${record.distanceUnit.storageValue} • ${record.volume} ${record.volumeUnit.storageValue}")
                                        }
                                        Text(record.totalCost.asCurrency())
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionGrid(
    hasSelectedVehicle: Boolean,
    onOpenImport: () -> Unit,
    onOpenVehicles: () -> Unit,
    onOpenBrowse: () -> Unit,
    onOpenVehicle: () -> Unit,
    onAddFuelUp: () -> Unit,
    onAddService: () -> Unit,
    onAddExpense: () -> Unit,
    onAddTrip: () -> Unit,
) {
    val actions = listOf(
        DashboardAction("Import Center", Icons.Outlined.Archive, onOpenImport),
        DashboardAction("Browse Vehicles", Icons.Outlined.Storage, onOpenVehicles),
        DashboardAction("Browse Records", Icons.Outlined.Search, onOpenBrowse),
        DashboardAction("Vehicle Details", Icons.Outlined.DirectionsCar, onOpenVehicle),
        DashboardAction("New Fuel-Up", Icons.Outlined.LocalGasStation, onAddFuelUp, enabled = hasSelectedVehicle),
        DashboardAction("New Service", Icons.Outlined.Build, onAddService, enabled = hasSelectedVehicle),
        DashboardAction("New Expense", Icons.Outlined.ReceiptLong, onAddExpense, enabled = hasSelectedVehicle),
        DashboardAction("New Trip", Icons.Outlined.Map, onAddTrip, enabled = hasSelectedVehicle),
    )
    LazyVerticalGrid(
        modifier = Modifier.height(440.dp),
        columns = GridCells.Fixed(2),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        userScrollEnabled = false,
    ) {
        items(actions) { action ->
            Card(
                modifier = Modifier.fillMaxSize().clickable(enabled = action.enabled, onClick = action.onClick),
                colors = CardDefaults.cardColors(
                    containerColor = if (action.enabled) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant,
                ),
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(18.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    Icon(action.icon, contentDescription = null)
                    Text(action.label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StatsRow(detail: VehicleDetailBundle) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SummaryChip("Fill-Ups", detail.stats.fillUpCount.toString())
        SummaryChip("Avg MPG", detail.stats.averageFuelEfficiency?.formatOneDecimal() ?: "n/a")
        SummaryChip("Last MPG", detail.stats.lastFuelEfficiency?.formatOneDecimal() ?: "n/a")
        SummaryChip("Fuel Cost", detail.stats.totalFuelCost.asCurrency())
        SummaryChip("Service Cost", detail.stats.serviceCostTotal.asCurrency())
        SummaryChip("Expense Cost", detail.stats.expenseCostTotal.asCurrency())
        SummaryChip("Trip Miles", detail.stats.tripDistanceTotal.formatOneDecimal())
    }
}

@Composable
private fun SummaryChip(label: String, value: String) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall)
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VehiclesScreen(
    repository: GarageRepository,
    onBack: () -> Unit,
    onOpenVehicle: (Long) -> Unit,
) {
    val vehicles by repository.observeVehicles().collectAsStateWithLifecycle(initialValue = emptyList())
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Vehicles") },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("Back") }
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
            items(vehicles.size) { index ->
                val vehicle = vehicles[index]
                Card(modifier = Modifier.fillMaxWidth().clickable { onOpenVehicle(vehicle.id) }) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column {
                            Text(vehicle.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text(listOfNotNull(vehicle.year?.toString(), vehicle.make, vehicle.model).joinToString(" "))
                        }
                        Text(vehicle.lifecycle.name.lowercase().replaceFirstChar(Char::uppercase))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun VehicleDetailScreen(
    repository: GarageRepository,
    vehicleId: Long,
    onBack: () -> Unit,
    onBrowseRecords: () -> Unit,
    onEditFuelUp: (Long) -> Unit,
    onEditService: (Long) -> Unit,
    onEditExpense: (Long) -> Unit,
    onEditTrip: (Long) -> Unit,
    onAddFuelUp: () -> Unit,
    onAddService: () -> Unit,
    onAddExpense: () -> Unit,
    onAddTrip: () -> Unit,
) {
    val detail by repository.observeVehicleDetail(vehicleId).collectAsStateWithLifecycle(initialValue = null)
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(detail?.vehicle?.name ?: "Vehicle") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } },
            )
        },
    ) { padding ->
        detail?.let { data ->
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
                            Text("Vehicle Details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text(listOfNotNull(data.vehicle.year?.toString(), data.vehicle.make, data.vehicle.model, data.vehicle.submodel).joinToString(" "))
                            if (data.vehicle.licensePlate.isNotBlank()) Text("Plate: ${data.vehicle.licensePlate}")
                            if (data.vehicle.notes.isNotBlank()) Text(data.vehicle.notes)
                        }
                    }
                }
                item { StatsRow(detail = data) }
                item {
                    Card {
                        FlowRow(
                            modifier = Modifier.padding(18.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            AssistChip(onClick = onAddFuelUp, label = { Text("New Fuel-Up") })
                            AssistChip(onClick = onAddService, label = { Text("New Service") })
                            AssistChip(onClick = onAddExpense, label = { Text("New Expense") })
                            AssistChip(onClick = onAddTrip, label = { Text("New Trip") })
                            AssistChip(onClick = onBrowseRecords, label = { Text("Browse Records") })
                        }
                    }
                }
                item {
                    Card {
                        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Recent Fill-Ups", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            if (data.recentFillUps.isEmpty()) {
                                Text("No fill-ups imported yet.")
                            } else {
                                data.recentFillUps.forEach { record ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Column(Modifier.weight(1f)) {
                                            Text(record.dateTime.format(DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm")))
                                            Text("${record.odometerReading.toInt()} ${record.distanceUnit.storageValue} • ${record.totalCost.asCurrency()}")
                                        }
                                        IconButton(onClick = { onEditFuelUp(record.id) }) {
                                            Icon(Icons.Outlined.Edit, contentDescription = "Edit fuel-up")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                item {
                    Card {
                        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Recent Services", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            if (data.recentServices.isEmpty()) {
                                Text("No services imported yet.")
                            } else {
                                data.recentServices.forEach { record ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Column(Modifier.weight(1f)) {
                                            Text(record.dateTime.format(DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm")))
                                            Text(
                                                listOfNotNull(
                                                    record.serviceCenterName.takeIf { it.isNotBlank() },
                                                    record.totalCost.asCurrency(),
                                                ).joinToString(" • "),
                                            )
                                        }
                                        IconButton(onClick = { onEditService(record.id) }) {
                                            Icon(Icons.Outlined.Edit, contentDescription = "Edit service")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                item {
                    Card {
                        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Recent Expenses", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            if (data.recentExpenses.isEmpty()) {
                                Text("No expenses imported yet.")
                            } else {
                                data.recentExpenses.forEach { record ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Column(Modifier.weight(1f)) {
                                            Text(record.dateTime.format(DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm")))
                                            Text(
                                                listOfNotNull(
                                                    record.expenseCenterName.takeIf { it.isNotBlank() },
                                                    record.totalCost.asCurrency(),
                                                ).joinToString(" • "),
                                            )
                                        }
                                        IconButton(onClick = { onEditExpense(record.id) }) {
                                            Icon(Icons.Outlined.Edit, contentDescription = "Edit expense")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                item {
                    Card {
                        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Recent Trips", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            if (data.recentTrips.isEmpty()) {
                                Text("No trips imported yet.")
                            } else {
                                data.recentTrips.forEach { record ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Column(Modifier.weight(1f)) {
                                            Text(record.startDateTime.format(DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm")))
                                            Text(
                                                listOfNotNull(
                                                    listOf(
                                                        record.startLocation.takeIf { it.isNotBlank() },
                                                        record.endLocation.takeIf { it.isNotBlank() },
                                                    ).filterNotNull().takeIf { it.isNotEmpty() }?.joinToString(" → "),
                                                    record.distance?.let { "${it.toStableString()} ${record.distanceUnit.storageValue}" },
                                                ).joinToString(" • "),
                                            )
                                        }
                                        IconButton(onClick = { onEditTrip(record.id) }) {
                                            Icon(Icons.Outlined.Edit, contentDescription = "Edit trip")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ImportScreen(
    repository: GarageRepository,
    onBack: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var lastReport by remember { mutableStateOf<ImportReport?>(null) }
    var importError by remember { mutableStateOf<String?>(null) }
    val context = androidx.compose.ui.platform.LocalContext.current

    fun runImport(importer: suspend (InputStream) -> ImportReport, uri: Uri?) {
        if (uri == null) return
        scope.launch {
            runCatching {
                val stream = context.contentResolver.openInputStream(uri)
                    ?: error("Unable to open selected file.")
                stream.use { importer(it) }
            }.onSuccess {
                lastReport = it
                importError = null
            }.onFailure {
                importError = it.message
            }
        }
    }

    val acarBackupLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        runImport(repository::importAcarAbp, uri)
    }
    val acarCsvLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        runImport(repository::importAcarCsv, uri)
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Import Center") },
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
                        Text("aCar Restore", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Import a full `.abp` backup and replace current local data.")
                        Button(onClick = { acarBackupLauncher.launch(arrayOf("*/*")) }) {
                            Icon(Icons.Outlined.Archive, contentDescription = null)
                            Spacer(Modifier.size(8.dp))
                            Text("Choose aCar Backup")
                        }
                    }
                }
            }
            item {
                Card {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("aCar Records CSV", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Import the sectioned CSV export and merge vehicles and records by name.")
                        Button(onClick = { acarCsvLauncher.launch(arrayOf("text/*", "*/*")) }) {
                            Icon(Icons.Outlined.FileUpload, contentDescription = null)
                            Spacer(Modifier.size(8.dp))
                            Text("Choose aCar CSV")
                        }
                    }
                }
            }
            importError?.let { message ->
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                        Text(message, modifier = Modifier.padding(18.dp))
                    }
                }
            }
            lastReport?.let { report ->
                item {
                    Card {
                        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(report.sourceLabel, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            SummaryChip("Vehicles", report.vehiclesImported.toString())
                            SummaryChip("Fill-Ups", report.fillUpsImported.toString())
                            SummaryChip("Services", report.serviceRecordsImported.toString())
                            SummaryChip("Expenses", report.expenseRecordsImported.toString())
                            SummaryChip("Trips", report.tripRecordsImported.toString())
                            if (report.issues.isNotEmpty()) {
                                Text("Import Notes", fontWeight = FontWeight.SemiBold)
                                report.issues.take(8).forEach { issue ->
                                    Text("• ${issue.section.ifBlank { "General" }}: ${issue.message}")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private data class DashboardAction(
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val onClick: () -> Unit,
    val enabled: Boolean = true,
)
