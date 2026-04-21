package com.garageledger.ui

import android.Manifest
import android.net.Uri
import android.os.Build
import android.content.pm.PackageManager
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
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.LocalGasStation
import androidx.compose.material.icons.outlined.Map
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
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.garageledger.data.GarageRepository
import com.garageledger.data.backup.LocalBackupManager
import com.garageledger.domain.model.AppPreferenceSnapshot
import com.garageledger.domain.model.ImportReport
import com.garageledger.domain.model.Vehicle
import com.garageledger.domain.model.VehicleDetailBundle
import com.garageledger.shortcuts.LaunchRequest
import com.garageledger.shortcuts.QuickActionShortcutManager
import com.garageledger.shortcuts.QuickActionTarget
import java.io.InputStream
import java.io.OutputStream
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.launch

@Composable
fun GarageLedgerApp(
    repository: GarageRepository,
    backupManager: LocalBackupManager,
    launchRequest: LaunchRequest? = null,
    onLaunchHandled: () -> Unit = {},
) {
    val navController = rememberNavController()
    LaunchedEffect(launchRequest?.token) {
        val request = launchRequest ?: return@LaunchedEffect
        navController.navigate(request.route) {
            launchSingleTop = true
        }
        onLaunchHandled()
    }
    NavHost(navController = navController, startDestination = "console") {
        composable("console") {
            ConsoleScreen(
                repository = repository,
                onOpenImport = { navController.navigate("import") },
                onOpenVehicles = { navController.navigate("vehicles") },
                onOpenBrowse = { navController.navigate("browse/-1") },
                onOpenStats = { navController.navigate("stats/$it") },
                onOpenVehicle = { navController.navigate("vehicle/$it") },
                onAddFuelUp = { navController.navigate("fuelup/$it/-1") },
                onAddService = { navController.navigate("service/$it/-1") },
                onAddExpense = { navController.navigate("expense/$it/-1") },
                onAddTrip = { navController.navigate("trip/$it/-1") },
            )
        }
        composable(
            route = "quickadd/{target}",
            arguments = listOf(navArgument("target") { type = NavType.StringType }),
        ) { backStackEntry ->
            val target = QuickActionTarget.fromRouteSegment(backStackEntry.arguments?.getString("target"))
            if (target != null) {
                QuickAddChooserScreen(
                    repository = repository,
                    target = target,
                    onBack = { navController.popBackStack() },
                    onOpenVehicleEditor = { vehicleId ->
                        navController.navigate(target.editorRoute(vehicleId)) {
                            popUpTo("quickadd/${target.routeSegment}") { inclusive = true }
                        }
                    },
                )
            }
        }
        composable("import") {
            ImportScreen(
                repository = repository,
                backupManager = backupManager,
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
            route = "stats/{vehicleId}",
            arguments = listOf(navArgument("vehicleId") { type = NavType.LongType }),
        ) { backStackEntry ->
            val vehicleId = backStackEntry.arguments?.getLong("vehicleId") ?: -1L
            StatisticsScreen(
                repository = repository,
                preselectedVehicleId = vehicleId.takeIf { it > 0L },
                onBack = { navController.popBackStack() },
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
                onOpenStats = { navController.navigate("stats/$vehicleId") },
                onOpenRecordDetail = { family, recordId ->
                    navController.navigate("record/${family.routeSegment()}/$vehicleId/$recordId")
                },
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
                    navController.navigate("record/${item.family.routeSegment()}/${item.vehicleId}/${item.recordId}")
                },
            )
        }
        composable(
            route = "record/{family}/{vehicleId}/{recordId}",
            arguments = listOf(
                navArgument("family") { type = NavType.StringType },
                navArgument("vehicleId") { type = NavType.LongType },
                navArgument("recordId") { type = NavType.LongType },
            ),
        ) { backStackEntry ->
            val family = recordFamilyFromRouteSegment(backStackEntry.arguments?.getString("family")) ?: return@composable
            val detailVehicleId = backStackEntry.arguments?.getLong("vehicleId") ?: 0L
            val detailRecordId = backStackEntry.arguments?.getLong("recordId") ?: 0L
            val detailRoute = "record/${family.routeSegment()}/$detailVehicleId/$detailRecordId"
            RecordDetailScreen(
                repository = repository,
                family = family,
                vehicleId = detailVehicleId,
                recordId = detailRecordId,
                onBack = { navController.popBackStack() },
                onEdit = {
                    val route = when (family) {
                        com.garageledger.domain.model.RecordFamily.FILL_UP -> "fuelup/$detailVehicleId/$detailRecordId"
                        com.garageledger.domain.model.RecordFamily.SERVICE -> "service/$detailVehicleId/$detailRecordId"
                        com.garageledger.domain.model.RecordFamily.EXPENSE -> "expense/$detailVehicleId/$detailRecordId"
                        com.garageledger.domain.model.RecordFamily.TRIP -> "trip/$detailVehicleId/$detailRecordId"
                    }
                    navController.navigate(route) {
                        popUpTo(detailRoute) { inclusive = true }
                    }
                },
                onDeleted = { navController.popBackStack() },
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun ConsoleScreen(
    repository: GarageRepository,
    onOpenImport: () -> Unit,
    onOpenVehicles: () -> Unit,
    onOpenBrowse: () -> Unit,
    onOpenStats: (Long) -> Unit,
    onOpenVehicle: (Long) -> Unit,
    onAddFuelUp: (Long) -> Unit,
    onAddService: (Long) -> Unit,
    onAddExpense: (Long) -> Unit,
    onAddTrip: (Long) -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val vehicles by repository.observeVehicles().collectAsStateWithLifecycle(initialValue = emptyList())
    var selectedVehicleId by rememberSaveable { mutableLongStateOf(0L) }
    var menuExpanded by remember { mutableStateOf(false) }
    var shortcutStatus by remember { mutableStateOf<String?>(null) }

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
                    onOpenStats = { onOpenStats(selectedVehicle?.id ?: -1L) },
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
                        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Pin Quick Actions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text("Create launcher shortcuts for the old aCar-style fast entry paths.")
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                QuickActionTarget.entries.forEach { target ->
                                    AssistChip(
                                        onClick = {
                                            val requested = QuickActionShortcutManager.requestPinnedShortcut(context, target)
                                            shortcutStatus = if (requested) {
                                                "${target.shortLabel} pin request sent to the launcher."
                                            } else {
                                                "Pinned shortcuts are not supported by this launcher."
                                            }
                                        },
                                        label = { Text(target.shortLabel.removePrefix("New ")) },
                                    )
                                }
                            }
                            shortcutStatus?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                        }
                    }
                }
                item {
                    Card {
                        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Upcoming Reminders", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            if (detail!!.upcomingReminders.isEmpty()) {
                                Text("No reminder schedules imported yet.")
                            } else {
                                detail!!.upcomingReminders.take(4).forEach { item ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { onOpenVehicle(detail!!.vehicle.id) },
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                    ) {
                                        Column(Modifier.weight(1f)) {
                                            Text(item.serviceTypeName)
                                            Text(
                                                listOfNotNull(
                                                    item.reminder.dueDate?.let { "Due $it" },
                                                    item.reminder.dueDistance?.let { "At ${it.toInt()} mi" },
                                                ).joinToString(" • ").ifBlank { "Scheduled" },
                                            )
                                        }
                                        Text("Open")
                                    }
                                }
                            }
                        }
                    }
                }
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
    onOpenStats: () -> Unit,
    onOpenVehicle: () -> Unit,
    onAddFuelUp: () -> Unit,
    onAddService: () -> Unit,
    onAddExpense: () -> Unit,
    onAddTrip: () -> Unit,
) {
    val actions = listOf(
        DashboardAction("Import & Export", Icons.Outlined.Archive, onOpenImport),
        DashboardAction("Browse Vehicles", Icons.Outlined.Storage, onOpenVehicles),
        DashboardAction("Browse Records", Icons.Outlined.Search, onOpenBrowse),
        DashboardAction("Statistics & Charts", Icons.Outlined.BarChart, onOpenStats),
        DashboardAction("Vehicle Details", Icons.Outlined.DirectionsCar, onOpenVehicle),
        DashboardAction("New Fuel-Up", Icons.Outlined.LocalGasStation, onAddFuelUp, enabled = hasSelectedVehicle),
        DashboardAction("New Service", Icons.Outlined.Build, onAddService, enabled = hasSelectedVehicle),
        DashboardAction("New Expense", Icons.Outlined.ReceiptLong, onAddExpense, enabled = hasSelectedVehicle),
        DashboardAction("New Trip", Icons.Outlined.Map, onAddTrip, enabled = hasSelectedVehicle),
    )
    LazyVerticalGrid(
        modifier = Modifier.height(560.dp),
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
    onOpenStats: () -> Unit,
    onOpenRecordDetail: (com.garageledger.domain.model.RecordFamily, Long) -> Unit,
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
                            AssistChip(onClick = onOpenStats, label = { Text("Statistics & Charts") })
                        }
                    }
                }
                item {
                    Card {
                        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Reminders", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            if (data.upcomingReminders.isEmpty()) {
                                Text("No reminder schedules available for this vehicle.")
                            } else {
                                data.upcomingReminders.forEach { reminder ->
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(reminder.serviceTypeName)
                                        Text(
                                            listOfNotNull(
                                                reminder.reminder.dueDate?.let { "Due $it" },
                                                reminder.reminder.dueDistance?.let { "At ${it.toInt()} ${data.vehicle.distanceUnitOverride?.storageValue ?: "mi"}" },
                                            ).joinToString(" | ").ifBlank { "Scheduled" },
                                        )
                                    }
                                }
                            }
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
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { onOpenRecordDetail(com.garageledger.domain.model.RecordFamily.FILL_UP, record.id) },
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
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { onOpenRecordDetail(com.garageledger.domain.model.RecordFamily.SERVICE, record.id) },
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
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { onOpenRecordDetail(com.garageledger.domain.model.RecordFamily.EXPENSE, record.id) },
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
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { onOpenRecordDetail(com.garageledger.domain.model.RecordFamily.TRIP, record.id) },
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
    backupManager: LocalBackupManager,
    onBack: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val preferences by repository.preferences.collectAsStateWithLifecycle(initialValue = AppPreferenceSnapshot())
    var lastReport by remember { mutableStateOf<ImportReport?>(null) }
    var importError by remember { mutableStateOf<String?>(null) }
    var exportMessage by remember { mutableStateOf<String?>(null) }
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
                exportMessage = null
            }.onFailure {
                importError = it.message
            }
        }
    }

    fun runExport(exporter: suspend (OutputStream) -> Unit, uri: Uri?, successMessage: String) {
        if (uri == null) return
        scope.launch {
            runCatching {
                val stream = context.contentResolver.openOutputStream(uri)
                    ?: error("Unable to create the selected export file.")
                stream.use { exporter(it) }
            }.onSuccess {
                exportMessage = successMessage
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
    val exportCsvLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        runExport(repository::exportSectionedCsv, uri, "Readable CSV export saved.")
    }
    val exportBackupLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        runExport(repository::exportOpenJsonBackup, uri, "Open zipped backup saved.")
    }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        scope.launch {
            repository.setNotificationsEnabled(granted)
            exportMessage = if (granted) {
                "Reminder notifications enabled."
            } else {
                "Reminder notifications stay off until Android permission is granted."
            }
        }
    }

    fun notificationPermissionGranted(): Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Import & Export Center") },
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
                        Text("Automatic Local Backups", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Scheduled backups stay local in the app documents area and rotate automatically.")
                        Text("Cadence: ${formatBackupCadence(preferences.backupFrequencyHours)}")
                        Text("History: keep ${preferences.backupHistoryCount} zip files")
                        Text("Folder: ${backupManager.backupDirectoryPath()}")
                        Button(
                            onClick = {
                                scope.launch {
                                    runCatching { backupManager.writeBackupNow() }
                                        .onSuccess { result ->
                                            exportMessage = "Local backup saved to ${result.filePath}"
                                            importError = null
                                        }
                                        .onFailure { error ->
                                            importError = error.message
                                        }
                                }
                            },
                        ) {
                            Icon(Icons.Outlined.Archive, contentDescription = null)
                            Spacer(Modifier.size(8.dp))
                            Text("Run Local Backup Now")
                        }
                    }
                }
            }
            item {
                Card {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Reminder Alerts", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            if (preferences.notificationsEnabled) {
                                "Reminder checks are on every 12 hours with ${preferences.reminderTimeAlertPercent}% time and ${preferences.reminderDistanceAlertPercent}% distance thresholds."
                            } else {
                                "Notifications are currently off."
                            },
                        )
                        Button(
                            onClick = {
                                when {
                                    preferences.notificationsEnabled -> scope.launch {
                                        repository.setNotificationsEnabled(false)
                                        exportMessage = "Reminder notifications disabled."
                                    }

                                    notificationPermissionGranted() -> scope.launch {
                                        repository.setNotificationsEnabled(true)
                                        exportMessage = "Reminder notifications enabled."
                                    }

                                    else -> notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                            },
                        ) {
                            Icon(Icons.Outlined.Storage, contentDescription = null)
                            Spacer(Modifier.size(8.dp))
                            Text(if (preferences.notificationsEnabled) "Disable Alerts" else "Enable Alerts")
                        }
                    }
                }
            }
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
            item {
                Card {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Readable CSV Export", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Write the human-readable sectioned CSV export to any local document destination.")
                        Button(onClick = { exportCsvLauncher.launch("garage-ledger-export.csv") }) {
                            Icon(Icons.Outlined.FileUpload, contentDescription = null)
                            Spacer(Modifier.size(8.dp))
                            Text("Export CSV")
                        }
                    }
                }
            }
            item {
                Card {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Open Zipped Backup", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Export the full local ledger to a documented zipped JSON bundle.")
                        Button(onClick = { exportBackupLauncher.launch("garage-ledger-backup.zip") }) {
                            Icon(Icons.Outlined.Archive, contentDescription = null)
                            Spacer(Modifier.size(8.dp))
                            Text("Export Backup Zip")
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
            exportMessage?.let { message ->
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                        Text(message, modifier = Modifier.padding(18.dp))
                    }
                }
            }
            item {
                Card {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Local-First Notes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Backups and exports stay local-first. Manual exports use the Storage Access Framework, and scheduled backups write into the app's own documents area.")
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

private fun formatBackupCadence(hours: Int): String = when {
    hours <= 0 -> "Off"
    hours % 24 == 0 -> {
        val days = hours / 24
        if (days == 1) "Every day" else "Every $days days"
    }

    else -> "Every $hours hours"
}

private data class DashboardAction(
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val onClick: () -> Unit,
    val enabled: Boolean = true,
)
