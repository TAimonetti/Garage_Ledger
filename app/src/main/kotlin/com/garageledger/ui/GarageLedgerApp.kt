package com.garageledger.ui

import android.Manifest
import android.net.Uri
import android.os.Build
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.LocalGasStation
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.garageledger.R
import com.garageledger.core.model.VolumeUnit
import com.garageledger.data.GarageRepository
import com.garageledger.data.backup.LocalBackupManager
import com.garageledger.domain.model.AppPreferenceSnapshot
import com.garageledger.domain.model.FuellyCsvImportConfig
import com.garageledger.domain.model.FuellyCsvPreview
import com.garageledger.domain.model.FuellyImportField
import com.garageledger.domain.model.ImportReport
import com.garageledger.domain.model.OptionalFieldToggle
import com.garageledger.domain.model.Vehicle
import com.garageledger.domain.model.VehicleDetailBundle
import com.garageledger.domain.model.VehicleLifecycle
import com.garageledger.shortcuts.LaunchRequest
import com.garageledger.shortcuts.QuickActionShortcutManager
import com.garageledger.shortcuts.QuickActionTarget
import java.io.InputStream
import java.io.OutputStream
import kotlinx.coroutines.launch

private const val PostNotificationsPermission: String = "android.permission.POST_NOTIFICATIONS"

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
                onOpenPredictions = { navController.navigate("predictions/$it") },
                onOpenReminders = { navController.navigate("reminders/$it") },
                onOpenSettings = { navController.navigate("settings") },
                onOpenTypes = { navController.navigate("types") },
                onOpenVehicleParts = { navController.navigate("vehicle-parts/$it") },
                onOpenVehicle = { navController.navigate("vehicle/$it") },
                onAddVehicle = { navController.navigate("vehicle-edit/-1") },
                onAddFuelUp = { navController.navigate("fuelup/$it/-1") },
                onAddService = { navController.navigate(serviceEditorRoute(it, -1L)) },
                onAddExpense = { navController.navigate("expense/$it/-1") },
                onAddTrip = { navController.navigate("trip/$it/-1") },
                onEditTrip = { vehicleId, tripId -> navController.navigate(tripEditorRoute(vehicleId, tripId, finishMode = true)) },
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
        composable("settings") {
            SettingsScreen(
                repository = repository,
                onBack = { navController.popBackStack() },
            )
        }
        composable("types") {
            TypeManagementScreen(
                repository = repository,
                onBack = { navController.popBackStack() },
            )
        }
        composable("vehicles") {
            VehiclesScreen(
                repository = repository,
                onBack = { navController.popBackStack() },
                onOpenVehicle = { navController.navigate("vehicle/$it") },
                onAddVehicle = { navController.navigate("vehicle-edit/-1") },
            )
        }
        composable(
            route = "vehicle-edit/{vehicleId}",
            arguments = listOf(navArgument("vehicleId") { type = NavType.LongType }),
        ) { backStackEntry ->
            val editVehicleId = backStackEntry.arguments?.getLong("vehicleId") ?: -1L
            val editRoute = "vehicle-edit/$editVehicleId"
            VehicleEditorScreen(
                repository = repository,
                vehicleId = editVehicleId,
                onBack = { navController.popBackStack() },
                onSaved = { savedVehicleId ->
                    navController.navigate("vehicle/$savedVehicleId") {
                        popUpTo(editRoute) { inclusive = true }
                        launchSingleTop = true
                    }
                },
            )
        }
        composable(
            route = "vehicle-parts/{vehicleId}",
            arguments = listOf(navArgument("vehicleId") { type = NavType.LongType }),
        ) { backStackEntry ->
            val vehicleId = backStackEntry.arguments?.getLong("vehicleId") ?: 0L
            VehiclePartsScreen(
                repository = repository,
                vehicleId = vehicleId,
                onBack = { navController.popBackStack() },
                onAddPart = { navController.navigate("vehicle-part/$vehicleId/-1") },
                onEditPart = { navController.navigate("vehicle-part/$vehicleId/$it") },
            )
        }
        composable(
            route = "vehicle-part/{vehicleId}/{partId}",
            arguments = listOf(
                navArgument("vehicleId") { type = NavType.LongType },
                navArgument("partId") { type = NavType.LongType },
            ),
        ) { backStackEntry ->
            val vehicleId = backStackEntry.arguments?.getLong("vehicleId") ?: 0L
            VehiclePartEditorScreen(
                repository = repository,
                vehicleId = vehicleId,
                partId = backStackEntry.arguments?.getLong("partId") ?: -1L,
                onBack = { navController.popBackStack() },
                onSaved = { navController.popBackStack() },
            )
        }
        composable(
            route = "reminders/{vehicleId}",
            arguments = listOf(navArgument("vehicleId") { type = NavType.LongType }),
        ) { backStackEntry ->
            val vehicleId = backStackEntry.arguments?.getLong("vehicleId") ?: -1L
            RemindersCenterScreen(
                repository = repository,
                preselectedVehicleId = vehicleId.takeIf { it > 0L },
                onBack = { navController.popBackStack() },
                onAddReminder = { selectedVehicleId ->
                    navController.navigate("reminder-edit/$selectedVehicleId/-1")
                },
                onEditReminder = { selectedVehicleId, reminderId ->
                    navController.navigate("reminder-edit/$selectedVehicleId/$reminderId")
                },
                onCreateService = { selectedVehicleId, serviceTypeId ->
                    navController.navigate(serviceEditorRoute(selectedVehicleId, -1L, serviceTypeId))
                },
            )
        }
        composable(
            route = "reminder-edit/{vehicleId}/{reminderId}",
            arguments = listOf(
                navArgument("vehicleId") { type = NavType.LongType },
                navArgument("reminderId") { type = NavType.LongType },
            ),
        ) { backStackEntry ->
            val vehicleId = backStackEntry.arguments?.getLong("vehicleId") ?: -1L
            ReminderEditorScreen(
                repository = repository,
                vehicleId = vehicleId,
                reminderId = backStackEntry.arguments?.getLong("reminderId") ?: -1L,
                onBack = { navController.popBackStack() },
                onSaved = { navController.popBackStack() },
            )
        }
        composable(
            route = "predictions/{vehicleId}",
            arguments = listOf(navArgument("vehicleId") { type = NavType.LongType }),
        ) { backStackEntry ->
            val vehicleId = backStackEntry.arguments?.getLong("vehicleId") ?: -1L
            PredictionsScreen(
                repository = repository,
                preselectedVehicleId = vehicleId.takeIf { it > 0L },
                onBack = { navController.popBackStack() },
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
                onOpenPredictions = { navController.navigate("predictions/$vehicleId") },
                onOpenRecordDetail = { family, recordId ->
                    navController.navigate("record/${family.routeSegment()}/$vehicleId/$recordId")
                },
                onEditFuelUp = { navController.navigate("fuelup/$vehicleId/$it") },
                onEditService = { navController.navigate(serviceEditorRoute(vehicleId, it)) },
                onEditExpense = { navController.navigate("expense/$vehicleId/$it") },
                onEditTrip = { navController.navigate("trip/$vehicleId/$it") },
                onEditPart = { navController.navigate("vehicle-part/$vehicleId/$it") },
                onEditReminder = { navController.navigate("reminder-edit/$vehicleId/$it") },
                onEditVehicle = { navController.navigate("vehicle-edit/$vehicleId") },
                onDeletedVehicle = { navController.popBackStack() },
                onManageParts = { navController.navigate("vehicle-parts/$vehicleId") },
                onManageReminders = { navController.navigate("reminders/$vehicleId") },
                onAddFuelUp = { navController.navigate("fuelup/$vehicleId/-1") },
                onAddService = { navController.navigate(serviceEditorRoute(vehicleId, -1L)) },
                onAddExpense = { navController.navigate("expense/$vehicleId/-1") },
                onAddTrip = { navController.navigate("trip/$vehicleId/-1") },
                onAddPart = { navController.navigate("vehicle-part/$vehicleId/-1") },
                onAddReminder = { navController.navigate("reminder-edit/$vehicleId/-1") },
                onCreateServiceFromReminder = { serviceTypeId ->
                    navController.navigate(serviceEditorRoute(vehicleId, -1L, serviceTypeId))
                },
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
                onEditRecord = { item ->
                    val route = when (item.family) {
                        com.garageledger.domain.model.RecordFamily.FILL_UP -> "fuelup/${item.vehicleId}/${item.recordId}"
                        com.garageledger.domain.model.RecordFamily.SERVICE -> serviceEditorRoute(item.vehicleId, item.recordId)
                        com.garageledger.domain.model.RecordFamily.EXPENSE -> "expense/${item.vehicleId}/${item.recordId}"
                        com.garageledger.domain.model.RecordFamily.TRIP -> tripEditorRoute(item.vehicleId, item.recordId)
                    }
                    navController.navigate(route)
                },
                onCopyTrip = { item ->
                    navController.navigate(tripEditorRoute(item.vehicleId, -1L, copyFromId = item.recordId))
                },
                onFinishTrip = { item ->
                    navController.navigate(tripEditorRoute(item.vehicleId, item.recordId, finishMode = true))
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
                        com.garageledger.domain.model.RecordFamily.SERVICE -> serviceEditorRoute(detailVehicleId, detailRecordId)
                        com.garageledger.domain.model.RecordFamily.EXPENSE -> "expense/$detailVehicleId/$detailRecordId"
                        com.garageledger.domain.model.RecordFamily.TRIP -> "trip/$detailVehicleId/$detailRecordId"
                    }
                    navController.navigate(route) {
                        popUpTo(detailRoute) { inclusive = true }
                    }
                },
                onDeleted = { navController.popBackStack() },
                onCopyTrip = {
                    navController.navigate(tripEditorRoute(detailVehicleId, -1L, copyFromId = detailRecordId))
                },
                onFinishTrip = {
                    navController.navigate(tripEditorRoute(detailVehicleId, detailRecordId, finishMode = true)) {
                        popUpTo(detailRoute) { inclusive = true }
                    }
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
            route = "service/{vehicleId}/{recordId}?seedTypeId={seedTypeId}",
            arguments = listOf(
                navArgument("vehicleId") { type = NavType.LongType },
                navArgument("recordId") { type = NavType.LongType },
                navArgument("seedTypeId") {
                    type = NavType.LongType
                    defaultValue = -1L
                },
            ),
        ) { backStackEntry ->
            ServiceEditorScreen(
                repository = repository,
                vehicleId = backStackEntry.arguments?.getLong("vehicleId") ?: 0L,
                recordId = backStackEntry.arguments?.getLong("recordId") ?: -1L,
                seedServiceTypeId = backStackEntry.arguments?.getLong("seedTypeId")?.takeIf { it > 0L },
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
            route = "trip/{vehicleId}/{recordId}?copyFromId={copyFromId}&finishMode={finishMode}",
            arguments = listOf(
                navArgument("vehicleId") { type = NavType.LongType },
                navArgument("recordId") { type = NavType.LongType },
                navArgument("copyFromId") {
                    type = NavType.LongType
                    defaultValue = -1L
                },
                navArgument("finishMode") {
                    type = NavType.BoolType
                    defaultValue = false
                },
            ),
        ) { backStackEntry ->
            TripEditorScreen(
                repository = repository,
                vehicleId = backStackEntry.arguments?.getLong("vehicleId") ?: 0L,
                recordId = backStackEntry.arguments?.getLong("recordId") ?: -1L,
                copyFromTripId = backStackEntry.arguments?.getLong("copyFromId")?.takeIf { it > 0L },
                finishMode = backStackEntry.arguments?.getBoolean("finishMode") == true,
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
    onOpenPredictions: (Long) -> Unit,
    onOpenReminders: (Long) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenTypes: () -> Unit,
    onOpenVehicleParts: (Long) -> Unit,
    onOpenVehicle: (Long) -> Unit,
    onAddVehicle: () -> Unit,
    onAddFuelUp: (Long) -> Unit,
    onAddService: (Long) -> Unit,
    onAddExpense: (Long) -> Unit,
    onAddTrip: (Long) -> Unit,
    onEditTrip: (Long, Long) -> Unit,
) {
    val vehicles by repository.observeVehicles().collectAsStateWithLifecycle(initialValue = emptyList())
    val preferences by repository.preferences.collectAsStateWithLifecycle(initialValue = AppPreferenceSnapshot())
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
    val predictionsFlow = remember(selectedVehicle?.id) {
        selectedVehicle?.id?.let(repository::observePredictions)
    }
    val predictions by (predictionsFlow?.collectAsStateWithLifecycle(initialValue = emptyList())
        ?: remember { mutableStateOf(emptyList()) })
    val prediction = predictions.firstOrNull()
    val openTrip = detail?.recentTrips?.firstOrNull { it.endDateTime == null || it.endOdometerReading == null }

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
                .padding(padding)
                .navigationBarsPadding(),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
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
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("Console", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Box {
                                TextButton(onClick = { menuExpanded = true }) {
                                    Icon(Icons.Outlined.DirectionsCar, contentDescription = null)
                                    Spacer(Modifier.size(6.dp))
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
                        }
                        if (detail != null) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                openTrip?.let { trip ->
                                    AssistChip(
                                        onClick = { onEditTrip(trip.vehicleId, trip.id) },
                                        label = { Text("Finish Trip") },
                                    )
                                }
                                if (detail!!.upcomingReminders.isNotEmpty()) {
                                    AssistChip(
                                        onClick = { onOpenReminders(detail!!.vehicle.id) },
                                        label = { Text("${detail!!.upcomingReminders.size} Reminders") },
                                    )
                                }
                                prediction?.let { predictionSummary ->
                                    AssistChip(
                                        onClick = { onOpenPredictions(predictionSummary.vehicleId) },
                                        label = {
                                            Text(
                                                predictionSummary.nextFillUpDateTime?.let { "Next Fill-Up" }
                                                    ?: "Predictions",
                                            )
                                        },
                                    )
                                }
                            }
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
                    onOpenPredictions = { onOpenPredictions(selectedVehicle?.id ?: -1L) },
                    onOpenReminders = { onOpenReminders(selectedVehicle?.id ?: -1L) },
                    onOpenSettings = onOpenSettings,
                    onOpenTypes = onOpenTypes,
                    onOpenVehicleParts = { selectedVehicle?.id?.let(onOpenVehicleParts) },
                    onOpenVehicle = { selectedVehicle?.id?.let(onOpenVehicle) },
                    onAddVehicle = onAddVehicle,
                    onAddFuelUp = { selectedVehicle?.id?.let(onAddFuelUp) },
                    onAddService = { selectedVehicle?.id?.let(onAddService) },
                    onAddExpense = { selectedVehicle?.id?.let(onAddExpense) },
                    onAddTrip = { selectedVehicle?.id?.let(onAddTrip) },
                )
            }
            if (detail != null) {
                item {
                    ConsoleLastFillUpSummary(
                        detail = detail!!,
                        preferences = preferences,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ActionGrid(
    hasSelectedVehicle: Boolean,
    onOpenImport: () -> Unit,
    onOpenVehicles: () -> Unit,
    onOpenBrowse: () -> Unit,
    onOpenStats: () -> Unit,
    onOpenPredictions: () -> Unit,
    onOpenReminders: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenTypes: () -> Unit,
    onOpenVehicleParts: () -> Unit,
    onOpenVehicle: () -> Unit,
    onAddVehicle: () -> Unit,
    onAddFuelUp: () -> Unit,
    onAddService: () -> Unit,
    onAddExpense: () -> Unit,
    onAddTrip: () -> Unit,
) {
    val actions = listOf(
        DashboardAction(
            label = "Fuel-Up",
            iconRes = R.drawable.console_action_fuel_up,
            onClick = onAddFuelUp,
            enabled = hasSelectedVehicle,
        ),
        DashboardAction(
            label = "Service",
            iconRes = R.drawable.console_action_service,
            onClick = onAddService,
            enabled = hasSelectedVehicle,
        ),
        DashboardAction(
            label = "Expense",
            iconRes = R.drawable.console_action_expense,
            onClick = onAddExpense,
            enabled = hasSelectedVehicle,
        ),
        DashboardAction(
            label = "Trip",
            iconRes = R.drawable.console_action_trip,
            onClick = onAddTrip,
            enabled = hasSelectedVehicle,
        ),
        DashboardAction(
            label = "Add Vehicle",
            iconRes = R.drawable.console_action_add_vehicle,
            onClick = onAddVehicle,
        ),
        DashboardAction(
            label = "Browse",
            iconRes = R.drawable.console_action_browse,
            onClick = onOpenBrowse,
        ),
        DashboardAction(
            label = "Stats",
            iconRes = R.drawable.console_action_stats,
            onClick = onOpenStats,
            enabled = hasSelectedVehicle,
        ),
        DashboardAction(
            label = "Charts",
            iconRes = R.drawable.console_action_charts,
            onClick = onOpenStats,
            enabled = hasSelectedVehicle,
        ),
        DashboardAction(
            label = "Details",
            iconRes = R.drawable.console_action_details,
            onClick = onOpenVehicle,
            enabled = hasSelectedVehicle,
        ),
        DashboardAction(
            label = "Reminders",
            iconRes = R.drawable.console_action_reminders,
            onClick = onOpenReminders,
            enabled = hasSelectedVehicle,
        ),
        DashboardAction(
            label = "Parts",
            iconRes = R.drawable.console_action_parts,
            onClick = onOpenVehicleParts,
            enabled = hasSelectedVehicle,
        ),
        DashboardAction(
            label = "Predictions",
            iconRes = R.drawable.console_action_predictions,
            onClick = onOpenPredictions,
            enabled = hasSelectedVehicle,
        ),
        DashboardAction(
            label = "Import",
            iconRes = R.drawable.console_action_import,
            onClick = onOpenImport,
        ),
        DashboardAction(
            label = "Vehicles",
            iconRes = R.drawable.console_action_vehicles,
            onClick = onOpenVehicles,
        ),
        DashboardAction(
            label = "Settings",
            iconRes = R.drawable.console_action_settings,
            onClick = onOpenSettings,
        ),
        DashboardAction(
            label = "Types",
            iconRes = R.drawable.console_action_types,
            onClick = onOpenTypes,
        ),
    )
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val columns = if (maxWidth >= 360.dp) 4 else 3
        val gap = 6.dp
        val tileWidth = (maxWidth - gap * (columns - 1)) / columns
        val tileHeight = 94.dp
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(gap),
            verticalArrangement = Arrangement.spacedBy(gap),
            maxItemsInEachRow = columns,
        ) {
            actions.forEach { action ->
                Card(
                    modifier = Modifier
                        .width(tileWidth)
                        .height(tileHeight)
                        .alpha(if (action.enabled) 1f else 0.5f)
                        .clickable(enabled = action.enabled, onClick = action.onClick),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFDF9)),
                    border = BorderStroke(1.dp, Color(0xFFD8CFC2)),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 4.dp, vertical = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Image(
                            painter = painterResource(id = action.iconRes),
                            contentDescription = action.label,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(42.dp)
                                .padding(horizontal = 6.dp),
                        )
                        Text(
                            text = action.label,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontSize = 11.sp,
                                lineHeight = 12.sp,
                            ),
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF4A4033),
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 2.dp),
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ConsoleLastFillUpSummary(
    detail: VehicleDetailBundle,
    preferences: AppPreferenceSnapshot,
) {
    val latestFillUp = detail.recentFillUps.firstOrNull()
    val distanceUnitLabel = detail.vehicle.distanceUnitOverride?.storageValue
        ?: latestFillUp?.distanceUnit?.storageValue
        ?: preferences.distanceUnit.storageValue
    val fuelEfficiencyUnitLabel = detail.vehicle.fuelEfficiencyUnitOverride?.storageValue
        ?: latestFillUp?.fuelEfficiencyUnit?.storageValue
        ?: detail.recentFillUps.firstNotNullOfOrNull { it.fuelEfficiencyUnit?.storageValue }
        ?: preferences.fuelEfficiencyUnit.storageValue
    val lastMiles = latestFillUp?.distanceSincePrevious?.formatOneDecimal()?.let { value ->
        "$value $distanceUnitLabel"
    } ?: "n/a"
    val lastFuelCost = latestFillUp?.totalCost?.asCurrency(preferences.currencySymbol) ?: "n/a"
    val lastMpgValue = latestFillUp?.fuelEfficiency
        ?: latestFillUp?.importedFuelEfficiency
        ?: detail.stats.lastFuelEfficiency
    val lastMpg = lastMpgValue?.formatOneDecimal()?.let { value ->
        "$value $fuelEfficiencyUnitLabel"
    } ?: "n/a"

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
        ) {
            val gap = 10.dp
            val cellWidth = (maxWidth - (gap * 2)) / 3
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(gap),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ConsoleSummaryCell(
                    label = "Last Miles",
                    value = lastMiles,
                    modifier = Modifier.width(cellWidth),
                )
                ConsoleSummaryCell(
                    label = "Last Fuel Cost",
                    value = lastFuelCost,
                    modifier = Modifier.width(cellWidth),
                )
                ConsoleSummaryCell(
                    label = "Last MPG",
                    value = lastMpg,
                    modifier = Modifier.width(cellWidth),
                )
            }
        }
    }
}

@Composable
private fun ConsoleSummaryCell(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall.copy(fontSize = 13.sp),
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StatsRow(detail: VehicleDetailBundle) {
    val fuelEfficiencyUnitLabel = detail.vehicle.fuelEfficiencyUnitOverride?.storageValue
        ?: detail.recentFillUps.firstNotNullOfOrNull { it.fuelEfficiencyUnit?.storageValue }
    val tripDistanceUnitLabel = detail.vehicle.distanceUnitOverride?.storageValue
        ?: detail.recentTrips.firstOrNull()?.distanceUnit?.storageValue
        ?: detail.recentFillUps.firstOrNull()?.distanceUnit?.storageValue
    FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SummaryChip("Fill-Ups", detail.stats.fillUpCount.toString())
        SummaryChip(
            "Avg Eff.",
            detail.stats.averageFuelEfficiency?.formatOneDecimal()?.let { value ->
                fuelEfficiencyUnitLabel?.let { "$value $it" } ?: value
            } ?: "n/a",
        )
        SummaryChip(
            "Last Eff.",
            detail.stats.lastFuelEfficiency?.formatOneDecimal()?.let { value ->
                fuelEfficiencyUnitLabel?.let { "$value $it" } ?: value
            } ?: "n/a",
        )
        SummaryChip("Fuel Cost", detail.stats.totalFuelCost.asCurrency())
        SummaryChip("Service Cost", detail.stats.serviceCostTotal.asCurrency())
        SummaryChip("Expense Cost", detail.stats.expenseCostTotal.asCurrency())
        SummaryChip(
            "Trip Distance",
            detail.stats.tripDistanceTotal.formatOneDecimal().let { value ->
                tripDistanceUnitLabel?.let { "$value $it" } ?: value
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VehiclesScreen(
    repository: GarageRepository,
    onBack: () -> Unit,
    onOpenVehicle: (Long) -> Unit,
    onAddVehicle: () -> Unit,
) {
    val vehicles by repository.observeVehicles().collectAsStateWithLifecycle(initialValue = emptyList())
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Vehicles") },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("Back") }
                },
                actions = {
                    TextButton(onClick = onAddVehicle) { Text("Add") }
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
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        VehiclePhotoThumbnail(
                            photoUri = vehicle.profilePhotoUri,
                            modifier = Modifier.size(64.dp),
                        )
                        Column(modifier = Modifier.weight(1f)) {
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
    onOpenPredictions: () -> Unit,
    onOpenRecordDetail: (com.garageledger.domain.model.RecordFamily, Long) -> Unit,
    onEditFuelUp: (Long) -> Unit,
    onEditService: (Long) -> Unit,
    onEditExpense: (Long) -> Unit,
    onEditTrip: (Long) -> Unit,
    onEditPart: (Long) -> Unit,
    onEditReminder: (Long) -> Unit,
    onEditVehicle: () -> Unit,
    onDeletedVehicle: () -> Unit,
    onManageParts: () -> Unit,
    onManageReminders: () -> Unit,
    onAddFuelUp: () -> Unit,
    onAddService: () -> Unit,
    onAddExpense: () -> Unit,
    onAddTrip: () -> Unit,
    onAddPart: () -> Unit,
    onAddReminder: () -> Unit,
    onCreateServiceFromReminder: (Long) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val detail by repository.observeVehicleDetail(vehicleId).collectAsStateWithLifecycle(initialValue = null)
    val preferences by repository.preferences.collectAsStateWithLifecycle(initialValue = AppPreferenceSnapshot())
    var showDeleteDialog by remember { mutableStateOf(false) }
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(detail?.vehicle?.name ?: "Vehicle") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } },
            )
        },
    ) { padding ->
        detail?.let { data ->
            val visibleFields = preferences.visibleFields
            val openTrip = data.recentTrips.firstOrNull { it.endDateTime == null || it.endOdometerReading == null }
            val showLicensePlate = OptionalFieldToggle.VEHICLE_LICENSE_PLATE in visibleFields
            val showVin = OptionalFieldToggle.VEHICLE_VIN in visibleFields
            val showInsurance = OptionalFieldToggle.VEHICLE_INSURANCE_POLICY in visibleFields
            val showBodyStyle = OptionalFieldToggle.VEHICLE_BODY_STYLE in visibleFields
            val showColor = OptionalFieldToggle.VEHICLE_COLOR in visibleFields
            val showEngineDisplacement = OptionalFieldToggle.VEHICLE_ENGINE_DISPLACEMENT in visibleFields
            val showFuelTankCapacity = OptionalFieldToggle.VEHICLE_FUEL_TANK_CAPACITY in visibleFields
            val showPurchaseInfo = OptionalFieldToggle.VEHICLE_PURCHASE_INFO in visibleFields
            val showSellingInfo = OptionalFieldToggle.VEHICLE_SELLING_INFO in visibleFields
            if (showDeleteDialog) {
                AlertDialog(
                    onDismissRequest = { showDeleteDialog = false },
                    title = { Text("Delete Vehicle?") },
                    text = {
                        Text(
                            "This permanently deletes ${data.vehicle.name} and all of its local history, including " +
                                "${data.stats.fillUpCount} fill-ups, ${data.recentServices.size} recent services shown, " +
                                "${data.recentExpenses.size} recent expenses shown, ${data.recentTrips.size} recent trips shown, " +
                                "${data.parts.size} parts, reminders, and attachments.",
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                scope.launch {
                                    repository.deleteVehicle(data.vehicle.id)
                                    showDeleteDialog = false
                                    onDeletedVehicle()
                                }
                            },
                        ) { Text("Delete") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
                    },
                )
            }
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    Card {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            VehiclePhotoThumbnail(
                                photoUri = data.vehicle.profilePhotoUri,
                                modifier = Modifier.size(88.dp),
                            )
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Vehicle Details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                Text(listOfNotNull(data.vehicle.year?.toString(), data.vehicle.make, data.vehicle.model, data.vehicle.submodel).joinToString(" "))
                                Text("Lifecycle: ${data.vehicle.lifecycle.name.lowercase().replaceFirstChar(Char::uppercase)}")
                                data.vehicle.type.takeIf { it.isNotBlank() }?.let { Text("Type: $it") }
                                data.vehicle.country.takeIf { it.isNotBlank() }?.let { Text("Country: $it") }
                                if (showLicensePlate && data.vehicle.licensePlate.isNotBlank()) Text("Plate: ${data.vehicle.licensePlate}")
                                if (showVin && data.vehicle.vin.isNotBlank()) Text("VIN: ${data.vehicle.vin}")
                                if (showInsurance && data.vehicle.insurancePolicy.isNotBlank()) Text("Insurance: ${data.vehicle.insurancePolicy}")
                                if (showBodyStyle && data.vehicle.bodyStyle.isNotBlank()) Text("Body Style: ${data.vehicle.bodyStyle}")
                                if (showColor && data.vehicle.color.isNotBlank()) Text("Color: ${data.vehicle.color}")
                                if (showEngineDisplacement && data.vehicle.engineDisplacement.isNotBlank()) Text("Engine: ${data.vehicle.engineDisplacement}")
                                if (showFuelTankCapacity && data.vehicle.fuelTankCapacity != null) Text("Tank: ${data.vehicle.fuelTankCapacity.toStableString()}")
                                if (showPurchaseInfo) {
                                    val purchaseSummary = listOfNotNull(
                                        data.vehicle.purchaseDate?.formatForDisplay(preferences),
                                        data.vehicle.purchasePrice?.let(Double::asCurrency),
                                        data.vehicle.purchaseOdometer?.let(Double::toStableString),
                                    ).joinToString(" | ")
                                    if (purchaseSummary.isNotBlank()) Text("Purchase: $purchaseSummary")
                                }
                                if (showSellingInfo) {
                                    val sellingSummary = listOfNotNull(
                                        data.vehicle.sellingDate?.formatForDisplay(preferences),
                                        data.vehicle.sellingPrice?.let(Double::asCurrency),
                                        data.vehicle.sellingOdometer?.let(Double::toStableString),
                                    ).joinToString(" | ")
                                    if (sellingSummary.isNotBlank()) Text("Selling: $sellingSummary")
                                }
                                if (data.vehicle.notes.isNotBlank()) Text(data.vehicle.notes)
                            }
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
                            AssistChip(onClick = onEditVehicle, label = { Text("Edit Vehicle") })
                            AssistChip(
                                onClick = {
                                    scope.launch {
                                        repository.setVehicleLifecycle(
                                            data.vehicle.id,
                                            if (data.vehicle.lifecycle == VehicleLifecycle.ACTIVE) VehicleLifecycle.RETIRED else VehicleLifecycle.ACTIVE,
                                        )
                                    }
                                },
                                label = { Text(if (data.vehicle.lifecycle == VehicleLifecycle.ACTIVE) "Retire" else "Reactivate") },
                            )
                            AssistChip(onClick = { showDeleteDialog = true }, label = { Text("Delete Vehicle") })
                            AssistChip(onClick = onAddFuelUp, label = { Text("New Fuel-Up") })
                            AssistChip(onClick = onAddService, label = { Text("New Service") })
                            AssistChip(onClick = onAddExpense, label = { Text("New Expense") })
                            AssistChip(onClick = onAddTrip, label = { Text("New Trip") })
                            AssistChip(onClick = onAddPart, label = { Text("New Part") })
                            AssistChip(onClick = onAddReminder, label = { Text("New Reminder") })
                            if (openTrip != null) {
                                AssistChip(onClick = { onEditTrip(openTrip.id) }, label = { Text("Finish Open Trip") })
                            }
                            AssistChip(onClick = onManageParts, label = { Text("Manage Parts") })
                            AssistChip(onClick = onManageReminders, label = { Text("Reminders") })
                            AssistChip(onClick = onBrowseRecords, label = { Text("Browse Records") })
                            AssistChip(onClick = onOpenStats, label = { Text("Statistics & Charts") })
                            AssistChip(onClick = onOpenPredictions, label = { Text("Predictions") })
                        }
                    }
                }
                openTrip?.let { trip ->
                    item {
                        Card {
                            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("Open Trip", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                Text(
                                    listOfNotNull(
                                        trip.startDateTime.formatForDisplay(preferences),
                                        trip.startLocation.takeIf { it.isNotBlank() },
                                        trip.startOdometerReading.toStableString() + " " + trip.distanceUnit.storageValue,
                                    ).joinToString(" | "),
                                )
                                AssistChip(onClick = { onEditTrip(trip.id) }, label = { Text("Finish Open Trip") })
                            }
                        }
                    }
                }
                item {
                    Card {
                        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Vehicle Parts", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                AssistChip(onClick = onAddPart, label = { Text("Add Part") })
                                AssistChip(onClick = onManageParts, label = { Text("Manage Parts") })
                            }
                            if (data.parts.isEmpty()) {
                                Text("No vehicle parts recorded.")
                            } else {
                                data.parts.take(4).forEach { part ->
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(part.name, fontWeight = FontWeight.Medium)
                                        val partSummary = listOfNotNull(
                                            part.type.takeIf { it.isNotBlank() },
                                            part.brand.takeIf { it.isNotBlank() },
                                            part.partNumber.takeIf { it.isNotBlank() },
                                        ).joinToString(" | ")
                                        if (partSummary.isNotBlank()) {
                                            Text(partSummary)
                                        }
                                        if (part.notes.isNotBlank()) {
                                            Text(part.notes, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            AssistChip(onClick = { onEditPart(part.id) }, label = { Text("Edit") })
                                        }
                                    }
                                }
                                if (data.parts.size > 4) {
                                    Text("Showing 4 of ${data.parts.size} parts.")
                                }
                            }
                        }
                    }
                }
                item {
                    Card {
                        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Reminders", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                AssistChip(onClick = onAddReminder, label = { Text("Add Reminder") })
                                AssistChip(onClick = onManageReminders, label = { Text("Manage Reminders") })
                            }
                            if (data.upcomingReminders.isEmpty()) {
                                Text("No reminder schedules available for this vehicle.")
                            } else {
                                data.upcomingReminders.take(4).forEach { reminder ->
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(reminder.serviceTypeName)
                                        Text(
                                            listOfNotNull(
                                                reminder.reminder.dueDate?.let { "Due ${it.formatForDisplay(preferences, compact = true)}" },
                                                reminder.reminder.dueDistance?.let {
                                                    "At ${it.toInt()} ${data.vehicle.distanceUnitOverride?.storageValue ?: preferences.distanceUnit.storageValue}"
                                                },
                                            ).joinToString(" | ").ifBlank { "Scheduled" },
                                        )
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            AssistChip(onClick = { onEditReminder(reminder.reminder.id) }, label = { Text("Edit") })
                                            AssistChip(
                                                onClick = { onCreateServiceFromReminder(reminder.reminder.serviceTypeId) },
                                                label = { Text("New Service") },
                                            )
                                        }
                                    }
                                }
                                if (data.upcomingReminders.size > 4) {
                                    Text("Showing 4 of ${data.upcomingReminders.size} reminders.")
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
                                            Text(record.dateTime.formatForDisplay(preferences))
                                            Text("${record.odometerReading.toInt()} ${record.distanceUnit.storageValue} | ${record.totalCost.asCurrency()}")
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
                                            Text(record.dateTime.formatForDisplay(preferences))
                                            Text(
                                                listOfNotNull(
                                                    record.serviceCenterName.takeIf { it.isNotBlank() },
                                                    record.totalCost.asCurrency(),
                                                ).joinToString(" | "),
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
                                            Text(record.dateTime.formatForDisplay(preferences))
                                            Text(
                                                listOfNotNull(
                                                    record.expenseCenterName.takeIf { it.isNotBlank() },
                                                    record.totalCost.asCurrency(),
                                                ).joinToString(" | "),
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
                                            Text(record.startDateTime.formatForDisplay(preferences))
                                            Text(
                                                listOfNotNull(
                                                    listOf(
                                                        record.startLocation.takeIf { it.isNotBlank() },
                                                        record.endLocation.takeIf { it.isNotBlank() },
                                                    ).filterNotNull().takeIf { it.isNotEmpty() }?.joinToString(" -> "),
                                                    record.distance?.let { "${it.toStableString()} ${record.distanceUnit.storageValue}" },
                                                ).joinToString(" | "),
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
    val vehicles by repository.observeVehicles().collectAsStateWithLifecycle(initialValue = emptyList())
    var lastReport by remember { mutableStateOf<ImportReport?>(null) }
    var importError by remember { mutableStateOf<String?>(null) }
    var exportMessage by remember { mutableStateOf<String?>(null) }
    var fuellyPreview by remember { mutableStateOf<FuellyCsvPreview?>(null) }
    var fuellyFileUri by remember { mutableStateOf<Uri?>(null) }
    var fuellyFileLabel by remember { mutableStateOf<String?>(null) }
    var fuellySelectedVehicleId by remember { mutableStateOf<Long?>(null) }
    var fuellyFieldMapping by remember { mutableStateOf<Map<FuellyImportField, String?>>(emptyMap()) }
    var fuellyDistanceUnit by remember { mutableStateOf(preferences.distanceUnit) }
    var fuellyVolumeUnit by remember { mutableStateOf(if (preferences.volumeUnit == VolumeUnit.LITERS) VolumeUnit.LITERS else VolumeUnit.GALLONS_US) }
    val context = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(vehicles) {
        if (vehicles.isNotEmpty() && vehicles.none { it.id == fuellySelectedVehicleId }) {
            fuellySelectedVehicleId = vehicles.first().id
        }
    }

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

    fun runFuellyImport(uri: Uri?, config: FuellyCsvImportConfig) {
        if (uri == null) return
        scope.launch {
            runCatching {
                val stream = context.contentResolver.openInputStream(uri)
                    ?: error("Unable to open selected Fuelly CSV.")
                stream.use { repository.importFuellyCsv(it, config) }
            }.onSuccess {
                lastReport = it
                importError = null
                exportMessage = null
                fuellyPreview = null
                fuellyFileUri = null
                fuellyFileLabel = null
                fuellyFieldMapping = emptyMap()
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
    val openBackupImportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        runImport(repository::importOpenJsonBackup, uri)
    }
    val fuellyCsvLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            runCatching {
                val stream = context.contentResolver.openInputStream(uri)
                    ?: error("Unable to open selected Fuelly CSV.")
                stream.use { repository.previewFuellyCsv(it) }
            }.onSuccess { preview ->
                fuellyFileUri = uri
                fuellyFileLabel = uri.lastPathSegment?.substringAfterLast('/')
                fuellyPreview = preview
                fuellyFieldMapping = FuellyImportField.entries.associateWith { preview.suggestedMapping[it] }
                fuellyDistanceUnit = preview.suggestedDistanceUnit ?: preferences.distanceUnit
                fuellyVolumeUnit = preview.suggestedVolumeUnit
                    ?: if (preferences.volumeUnit == VolumeUnit.LITERS) VolumeUnit.LITERS else VolumeUnit.GALLONS_US
                importError = null
                exportMessage = null
            }.onFailure {
                importError = it.message
            }
        }
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
        ContextCompat.checkSelfPermission(context, PostNotificationsPermission) == PackageManager.PERMISSION_GRANTED

    val fuellyConfig = fuellySelectedVehicleId?.let { vehicleId ->
        FuellyCsvImportConfig(
            vehicleId = vehicleId,
            fieldMapping = fuellyFieldMapping.mapNotNull { (field, header) -> header?.let { field to it } }.toMap(),
            distanceUnit = fuellyDistanceUnit,
            volumeUnit = fuellyVolumeUnit,
        )
    }
    val fuellyValidationErrors = fuellyPreview?.let { preview ->
        buildList {
            if (vehicles.isNotEmpty() && fuellySelectedVehicleId == null) {
                add("Choose a vehicle for the imported Fuelly fill-ups.")
            }
            if (vehicles.isEmpty()) {
                add("At least one vehicle must exist before Fuelly data can be imported.")
            }
            if (fuellyConfig != null) {
                addAll(fuellyConfig.validationErrors(preview.headers))
            }
        }
    }.orEmpty()

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

                                    else -> notificationPermissionLauncher.launch(PostNotificationsPermission)
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
                FuellyImportCard(
                    vehicles = vehicles,
                    fileLabel = fuellyFileLabel,
                    preview = fuellyPreview,
                    selectedVehicleId = fuellySelectedVehicleId,
                    onVehicleSelected = { fuellySelectedVehicleId = it },
                    fieldMapping = fuellyFieldMapping,
                    onFieldMapped = { field, header ->
                        fuellyFieldMapping = fuellyFieldMapping.toMutableMap().apply { put(field, header) }
                    },
                    distanceUnit = fuellyDistanceUnit,
                    onDistanceUnitSelected = { fuellyDistanceUnit = it },
                    volumeUnit = fuellyVolumeUnit,
                    onVolumeUnitSelected = { fuellyVolumeUnit = it },
                    validationErrors = fuellyValidationErrors,
                    onPickFile = { fuellyCsvLauncher.launch(arrayOf("text/*", "*/*")) },
                    onImport = {
                        fuellyConfig?.let { config ->
                            runFuellyImport(fuellyFileUri, config)
                        }
                    },
                )
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
                        Text("Export the full local ledger to a documented zipped JSON bundle, or restore one back into local storage.")
                        Button(onClick = { openBackupImportLauncher.launch(arrayOf("application/zip", "*/*")) }) {
                            Icon(Icons.Outlined.FileUpload, contentDescription = null)
                            Spacer(Modifier.size(8.dp))
                            Text("Restore Backup Zip")
                        }
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
                            if (report.attachmentsImported > 0) {
                                SummaryChip("Attachments", report.attachmentsImported.toString())
                            }
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

private fun serviceEditorRoute(
    vehicleId: Long,
    recordId: Long,
    seedTypeId: Long? = null,
): String = buildString {
    append("service/")
    append(vehicleId)
    append("/")
    append(recordId)
    seedTypeId?.takeIf { it > 0L }?.let {
        append("?seedTypeId=")
        append(it)
    }
}

private fun tripEditorRoute(
    vehicleId: Long,
    recordId: Long,
    copyFromId: Long? = null,
    finishMode: Boolean = false,
): String = buildString {
    append("trip/")
    append(vehicleId)
    append("/")
    append(recordId)
    var hasQuery = false
    copyFromId?.takeIf { it > 0L }?.let {
        append("?copyFromId=")
        append(it)
        hasQuery = true
    }
    if (finishMode) {
        append(if (hasQuery) "&" else "?")
        append("finishMode=true")
    }
}

private data class DashboardAction(
    val label: String,
    @DrawableRes val iconRes: Int,
    val onClick: () -> Unit,
    val enabled: Boolean = true,
)

private data class DashboardActionStyle(
    val tileColor: Color,
    val badgeColor: Color,
    val iconTint: Color = Color.White,
    val labelColor: Color,
) {
    companion object {
        val FuelUp = DashboardActionStyle(
            tileColor = Color(0xFF1FA67A),
            badgeColor = Color(0xFF118A63),
            labelColor = Color(0xFF0E5A42),
        )
        val Service = DashboardActionStyle(
            tileColor = Color(0xFF3A7BD5),
            badgeColor = Color(0xFF2F63AE),
            labelColor = Color(0xFF244B83),
        )
        val Expense = DashboardActionStyle(
            tileColor = Color(0xFFE0A13A),
            badgeColor = Color(0xFFC1841F),
            labelColor = Color(0xFF8A5D0A),
        )
        val Trip = DashboardActionStyle(
            tileColor = Color(0xFFE36A3E),
            badgeColor = Color(0xFFC24F27),
            labelColor = Color(0xFF8E3415),
        )
        val AddVehicle = DashboardActionStyle(
            tileColor = Color(0xFFD24F57),
            badgeColor = Color(0xFFB93C45),
            labelColor = Color(0xFF8A2930),
        )
        val Browse = DashboardActionStyle(
            tileColor = Color(0xFF2C90C9),
            badgeColor = Color(0xFF1E78AC),
            labelColor = Color(0xFF16597F),
        )
        val Statistics = DashboardActionStyle(
            tileColor = Color(0xFF4AAA67),
            badgeColor = Color(0xFF2E8B4A),
            labelColor = Color(0xFF236639),
        )
        val Charts = DashboardActionStyle(
            tileColor = Color(0xFF39B7BD),
            badgeColor = Color(0xFF1E9297),
            labelColor = Color(0xFF15696D),
        )
        val Details = DashboardActionStyle(
            tileColor = Color(0xFFB95A4A),
            badgeColor = Color(0xFF984536),
            labelColor = Color(0xFF703126),
        )
        val Reminders = DashboardActionStyle(
            tileColor = Color(0xFFD89B27),
            badgeColor = Color(0xFFB87A12),
            labelColor = Color(0xFF85570A),
        )
        val Parts = DashboardActionStyle(
            tileColor = Color(0xFF7C6BC5),
            badgeColor = Color(0xFF6252A9),
            labelColor = Color(0xFF473D7A),
        )
        val Predictions = DashboardActionStyle(
            tileColor = Color(0xFF5B7BE0),
            badgeColor = Color(0xFF4462C6),
            labelColor = Color(0xFF324890),
        )
        val ImportExport = DashboardActionStyle(
            tileColor = Color(0xFF4B9BC1),
            badgeColor = Color(0xFF327DA0),
            labelColor = Color(0xFF255C75),
        )
        val Vehicles = DashboardActionStyle(
            tileColor = Color(0xFF6A8AA3),
            badgeColor = Color(0xFF547087),
            labelColor = Color(0xFF3C5261),
        )
        val Settings = DashboardActionStyle(
            tileColor = Color(0xFF7A7F88),
            badgeColor = Color(0xFF5F646C),
            labelColor = Color(0xFF474B52),
        )
        val Types = DashboardActionStyle(
            tileColor = Color(0xFF6A72D9),
            badgeColor = Color(0xFF5058BF),
            labelColor = Color(0xFF3C4390),
        )
    }
}
