package com.garageledger.data

import androidx.room.withTransaction
import com.garageledger.data.export.ExportSnapshot
import com.garageledger.data.export.OpenJsonBackupExporter
import com.garageledger.data.export.SectionedCsvExporter
import com.garageledger.data.export.StatisticsCsvExporter
import com.garageledger.data.export.StatisticsHtmlExporter
import com.garageledger.data.importer.AcarAbpImporter
import com.garageledger.data.importer.AcarCsvImporter
import com.garageledger.data.importer.FuellyCsvImporter
import com.garageledger.data.local.ExpenseRecordEntity
import com.garageledger.data.local.ExpenseRecordTypeCrossRef
import com.garageledger.data.local.FillUpRecordEntity
import com.garageledger.data.local.GarageDatabase
import com.garageledger.data.local.ServiceRecordEntity
import com.garageledger.data.local.ServiceRecordTypeCrossRef
import com.garageledger.data.local.ServiceReminderEntity
import com.garageledger.data.local.ExpenseTypeEntity
import com.garageledger.data.local.FuelTypeEntity
import com.garageledger.data.local.ServiceTypeEntity
import com.garageledger.data.local.TripTypeEntity
import com.garageledger.data.local.TripRecordEntity
import com.garageledger.data.local.VehicleEntity
import com.garageledger.data.local.VehiclePartEntity
import com.garageledger.data.local.toDomain
import com.garageledger.data.local.toEntity
import com.garageledger.data.preferences.AppPreferencesRepository
import com.garageledger.core.model.FuelEfficiencyUnit
import com.garageledger.domain.calc.ChronoOdometerRecord
import com.garageledger.domain.calc.FuelEfficiencyCalculator
import com.garageledger.domain.calc.RecordConsistencyValidator
import com.garageledger.domain.calc.ReminderAlertEvaluator
import com.garageledger.domain.calc.ReminderScheduler
import com.garageledger.domain.calc.StatisticsReportBuilder
import com.garageledger.domain.calc.TripCostBreakdown
import com.garageledger.domain.calc.TripCostCalculator
import com.garageledger.domain.model.AppPreferenceSnapshot
import com.garageledger.domain.model.BrowseRecordItem
import com.garageledger.domain.model.ReminderAlert
import com.garageledger.domain.model.ReminderDisplayItem
import com.garageledger.domain.model.ReminderWidgetItem
import com.garageledger.domain.model.ExpenseRecord
import com.garageledger.domain.model.ExpenseType
import com.garageledger.domain.model.FillUpRecord
import com.garageledger.domain.model.FuelWidgetItem
import com.garageledger.domain.model.FuelWidgetMetric
import com.garageledger.domain.model.FuellyCsvImportConfig
import com.garageledger.domain.model.FuellyCsvPreview
import com.garageledger.domain.model.FuelType
import com.garageledger.domain.model.ImportIssue
import com.garageledger.domain.model.ImportReport
import com.garageledger.domain.model.ImportedGarageData
import com.garageledger.domain.model.OptionalFieldToggle
import com.garageledger.domain.model.RecordFamily
import com.garageledger.domain.model.RecordAttachment
import com.garageledger.domain.model.ServiceRecord
import com.garageledger.domain.model.ServiceType
import com.garageledger.domain.model.StatisticsDashboard
import com.garageledger.domain.model.StatisticsFilter
import com.garageledger.domain.model.StatisticsSource
import com.garageledger.domain.model.StatisticsTimeframe
import com.garageledger.domain.model.TripRecord
import com.garageledger.domain.model.TripType
import com.garageledger.domain.model.Vehicle
import com.garageledger.domain.model.VehicleDetailBundle
import com.garageledger.domain.model.VehicleStatistics
import java.io.InputStream
import java.io.OutputStream
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class GarageRepository(
    private val database: GarageDatabase,
    private val preferencesRepository: AppPreferencesRepository,
    private val acarCsvImporter: AcarCsvImporter = AcarCsvImporter(),
    private val acarAbpImporter: AcarAbpImporter = AcarAbpImporter(),
    private val fuellyCsvImporter: FuellyCsvImporter = FuellyCsvImporter(),
    private val sectionedCsvExporter: SectionedCsvExporter = SectionedCsvExporter(),
    private val openJsonBackupExporter: OpenJsonBackupExporter = OpenJsonBackupExporter(),
    private val statisticsCsvExporter: StatisticsCsvExporter = StatisticsCsvExporter(),
    private val statisticsHtmlExporter: StatisticsHtmlExporter = StatisticsHtmlExporter(),
    private val statisticsReportBuilder: StatisticsReportBuilder = StatisticsReportBuilder(),
    private val onLedgerChanged: suspend () -> Unit = {},
) {
    private val dao = database.garageDao()

    val preferences = preferencesRepository.preferences

    fun observeVehicles(): Flow<List<Vehicle>> = dao.observeVehicles().map { list -> list.map(VehicleEntity::toDomain) }

    fun observeFuelTypes(): Flow<List<FuelType>> = dao.observeFuelTypes().map { list -> list.map(FuelTypeEntity::toDomain) }

    fun observeServiceTypes(): Flow<List<ServiceType>> = dao.observeServiceTypes().map { list -> list.map(ServiceTypeEntity::toDomain) }

    fun observeExpenseTypes(): Flow<List<ExpenseType>> = dao.observeExpenseTypes().map { list -> list.map(ExpenseTypeEntity::toDomain) }

    fun observeTripTypes(): Flow<List<TripType>> = dao.observeTripTypes().map { list -> list.map(TripTypeEntity::toDomain) }

    fun observeVehicleDetail(vehicleId: Long): Flow<VehicleDetailBundle?> = combine(
        combine(
            combine(
                dao.observeVehicle(vehicleId),
                dao.observeVehicleParts(vehicleId),
                dao.observeVehicleReminders(vehicleId),
                dao.observeVehicleFillUps(vehicleId),
                dao.observeVehicleServices(vehicleId),
            ) { vehicle, parts, reminders, fillUps, services ->
                VehicleDetailPrimarySnapshot(
                    vehicle = vehicle,
                    parts = parts,
                    reminders = reminders,
                    fillUps = fillUps,
                    services = services,
                    serviceTypes = emptyList(),
                )
            },
            dao.observeServiceTypes(),
        ) { primary, serviceTypes ->
            VehicleDetailPrimarySnapshot(
                vehicle = primary.vehicle,
                parts = primary.parts,
                reminders = primary.reminders,
                fillUps = primary.fillUps,
                services = primary.services,
                serviceTypes = serviceTypes,
            )
        },
        combine(
            dao.observeVehicleExpenses(vehicleId),
            dao.observeVehicleTrips(vehicleId),
        ) { expenses, trips ->
            VehicleDetailSecondarySnapshot(
                expenses = expenses,
                trips = trips,
            )
        },
    ) { primary, secondary ->
        val currentVehicle = primary.vehicle?.toDomain() ?: return@combine null
        val domainFillUps = primary.fillUps.map(FillUpRecordEntity::toDomain)
        val domainServices = primary.services.map { it.toDomain(emptyList()) }
        val domainExpenses = secondary.expenses.map { it.toDomain(emptyList()) }
        val domainTrips = secondary.trips.map(TripRecordEntity::toDomain)
        val stats = buildStats(vehicleId, domainFillUps, domainServices, domainExpenses, domainTrips)
        val reminderDisplays = buildReminderDisplays(
            reminders = primary.reminders,
            serviceTypes = primary.serviceTypes,
        )
        VehicleDetailBundle(
            vehicle = currentVehicle,
            parts = primary.parts.map(VehiclePartEntity::toDomain),
            reminders = primary.reminders.map(ServiceReminderEntity::toDomain),
            upcomingReminders = reminderDisplays,
            recentFillUps = domainFillUps.sortedByDescending { it.dateTime }.take(15),
            recentServices = domainServices.sortedByDescending { it.dateTime }.take(10),
            recentExpenses = domainExpenses.sortedByDescending { it.dateTime }.take(10),
            recentTrips = domainTrips.sortedByDescending { it.startDateTime }.take(10),
            stats = stats,
        )
    }

    suspend fun getFillUp(recordId: Long): FillUpRecord? = dao.getFillUp(recordId)?.toDomain()

    suspend fun getService(recordId: Long): ServiceRecord? {
        val entity = dao.getService(recordId) ?: return null
        val typeIds = dao.getServiceRecordCrossRefs(listOf(recordId))
            .filter { it.serviceRecordId == recordId }
            .map(ServiceRecordTypeCrossRef::serviceTypeId)
        return entity.toDomain(typeIds)
    }

    suspend fun getExpense(recordId: Long): ExpenseRecord? {
        val entity = dao.getExpense(recordId) ?: return null
        val typeIds = dao.getExpenseRecordCrossRefs(listOf(recordId))
            .filter { it.expenseRecordId == recordId }
            .map(ExpenseRecordTypeCrossRef::expenseTypeId)
        return entity.toDomain(typeIds)
    }

    suspend fun getTrip(recordId: Long): TripRecord? = dao.getTrip(recordId)?.toDomain()

    suspend fun getRecordAttachments(recordFamily: RecordFamily, recordId: Long): List<RecordAttachment> =
        dao.getRecordAttachments(recordFamily, recordId).map(com.garageledger.data.local.RecordAttachmentEntity::toDomain)

    suspend fun estimateTripCost(record: TripRecord): TripCostBreakdown {
        val normalized = normalizeTrip(record)
        val endOdometer = normalized.endOdometerReading ?: normalized.startOdometerReading
        val fillUps = dao.getVehicleFillUpsAscending(record.vehicleId).map(FillUpRecordEntity::toDomain)
        val services = dao.getVehicleServicesAscending(record.vehicleId).map { it.toDomain(emptyList()) }
        val directExpenses = dao.getVehicleExpensesAscending(record.vehicleId)
            .map { it.toDomain(emptyList()) }
            .filter { it.odometerReading in normalized.startOdometerReading..endOdometer }
        return TripCostCalculator.calculate(
            trip = normalized,
            directExpenses = directExpenses,
            fuelCostPerDistance = TripCostCalculator.deriveFuelCostPerDistance(fillUps),
            serviceCostPerDistance = TripCostCalculator.deriveServiceCostPerDistance(services),
        )
    }

    suspend fun getVehicleFillUps(vehicleId: Long): List<FillUpRecord> =
        dao.getVehicleFillUpsAscending(vehicleId).map(FillUpRecordEntity::toDomain)

    suspend fun getVehicleTrips(vehicleId: Long): List<TripRecord> =
        dao.getVehicleTripsAscending(vehicleId).map(TripRecordEntity::toDomain)

    suspend fun getPaymentTypeSuggestions(): List<String> = dao.getPaymentTypeSuggestions()

    suspend fun getFuelBrandSuggestions(): List<String> = dao.getFuelBrandSuggestions()

    suspend fun getFuelStationSuggestions(): List<String> = dao.getFuelStationSuggestions()

    suspend fun getFuelAdditiveSuggestions(): List<String> = dao.getFuelAdditiveSuggestions()

    suspend fun getDrivingModeSuggestions(): List<String> = dao.getDrivingModeSuggestions()

    suspend fun getFillUpTagSuggestions(): List<String> = normalizeSuggestions(
        dao.getAllFillUps().flatMap(FillUpRecordEntity::tags),
    )

    suspend fun getServiceCenterSuggestions(): List<String> = dao.getServiceCenterSuggestions()

    suspend fun getExpenseCenterSuggestions(): List<String> = dao.getExpenseCenterSuggestions()

    suspend fun getTripPurposeSuggestions(): List<String> = dao.getTripPurposeSuggestions()

    suspend fun getTripClientSuggestions(): List<String> = dao.getTripClientSuggestions()

    suspend fun getTripLocationSuggestions(): List<String> = dao.getTripLocationSuggestions()

    suspend fun getServiceTypes(): List<ServiceType> = dao.getServiceTypes().map(ServiceTypeEntity::toDomain)

    suspend fun getExpenseTypes(): List<ExpenseType> = dao.getExpenseTypes().map(ExpenseTypeEntity::toDomain)

    suspend fun getTripTypes(): List<TripType> = dao.getTripTypes().map(TripTypeEntity::toDomain)

    suspend fun getPreferenceSnapshot(): AppPreferenceSnapshot = preferencesRepository.currentSnapshot()

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        preferencesRepository.update { current -> current.copy(notificationsEnabled = enabled) }
    }

    suspend fun setVisibleField(toggle: OptionalFieldToggle, visible: Boolean) {
        preferencesRepository.update { current ->
            val updated = current.visibleFields.toMutableSet().apply {
                if (visible) add(toggle) else remove(toggle)
            }
            current.copy(visibleFields = updated)
        }
    }

    suspend fun updatePreferences(transform: (AppPreferenceSnapshot) -> AppPreferenceSnapshot) {
        val current = preferencesRepository.currentSnapshot()
        val updated = transform(current)
        preferencesRepository.replace(updated)
        if (
            current.fuelEfficiencyAssignmentMethod != updated.fuelEfficiencyAssignmentMethod ||
            current.fuelEfficiencyUnit != updated.fuelEfficiencyUnit
        ) {
            dao.getVehicles().forEach { vehicle ->
                recalculateVehicleFillUps(vehicle.id, updated)
            }
        }
        onLedgerChanged()
    }

    suspend fun exportSectionedCsv(outputStream: OutputStream) {
        val snapshot = buildExportSnapshot()
        val servicesByRecord = snapshot.serviceRecordTypes.groupBy(
            keySelector = ServiceRecordTypeCrossRef::serviceRecordId,
            valueTransform = ServiceRecordTypeCrossRef::serviceTypeId,
        )
        val expensesByRecord = snapshot.expenseRecordTypes.groupBy(
            keySelector = ExpenseRecordTypeCrossRef::expenseRecordId,
            valueTransform = ExpenseRecordTypeCrossRef::expenseTypeId,
        )
        val csv = sectionedCsvExporter.export(
            preferences = snapshot.preferences,
            vehicles = snapshot.vehicles.map(VehicleEntity::toDomain),
            fillUps = snapshot.fillUpRecords.map(FillUpRecordEntity::toDomain),
            services = snapshot.serviceRecords.map { it.toDomain(servicesByRecord[it.id].orEmpty()) },
            expenses = snapshot.expenseRecords.map { it.toDomain(expensesByRecord[it.id].orEmpty()) },
            trips = snapshot.tripRecords.map(TripRecordEntity::toDomain),
            serviceTypes = snapshot.serviceTypes.map(ServiceTypeEntity::toDomain),
            expenseTypes = snapshot.expenseTypes.map(ExpenseTypeEntity::toDomain),
            tripTypes = snapshot.tripTypes.map(TripTypeEntity::toDomain),
        )
        outputStream.writer(Charsets.UTF_8).use { writer -> writer.write(csv) }
    }

    suspend fun exportOpenJsonBackup(outputStream: OutputStream) {
        openJsonBackupExporter.export(
            snapshot = buildExportSnapshot(),
            outputStream = outputStream,
        )
    }

    fun observeStatisticsDashboard(
        vehicleId: Long? = null,
        timeframe: StatisticsTimeframe = StatisticsTimeframe.ALL_TIME,
    ): Flow<StatisticsDashboard> = combine(
        combine(
            dao.observeVehicles(),
            dao.observeAllFillUps(),
            dao.observeAllServices(),
            dao.observeAllExpenses(),
            dao.observeAllTrips(),
        ) { vehicles, fillUps, services, expenses, trips ->
            StatisticsSource(
                vehicles = vehicles.map(VehicleEntity::toDomain),
                fillUps = fillUps.map(FillUpRecordEntity::toDomain),
                services = services.map { it.toDomain(emptyList()) },
                expenses = expenses.map { it.toDomain(emptyList()) },
                trips = trips.map(TripRecordEntity::toDomain),
            )
        },
        preferencesRepository.preferences,
    ) { source, preferences ->
        statisticsReportBuilder.build(
            source = source.copy(preferences = preferences),
            filter = StatisticsFilter(vehicleId = vehicleId, timeframe = timeframe),
        )
    }

    suspend fun exportStatisticsCsv(
        outputStream: OutputStream,
        filter: StatisticsFilter = StatisticsFilter(),
    ) {
        val dashboard = statisticsReportBuilder.build(
            source = buildStatisticsSource(buildExportSnapshot()),
            filter = filter,
        )
        val csv = statisticsCsvExporter.export(dashboard)
        outputStream.writer(Charsets.UTF_8).use { writer -> writer.write(csv) }
    }

    suspend fun exportStatisticsHtml(
        outputStream: OutputStream,
        filter: StatisticsFilter = StatisticsFilter(),
    ) {
        val dashboard = statisticsReportBuilder.build(
            source = buildStatisticsSource(buildExportSnapshot()),
            filter = filter,
        )
        val html = statisticsHtmlExporter.export(dashboard)
        outputStream.writer(Charsets.UTF_8).use { writer -> writer.write(html) }
    }

    suspend fun getDueReminderAlerts(now: LocalDateTime = LocalDateTime.now()): List<ReminderAlert> {
        val preferences = preferencesRepository.currentSnapshot()
        val reminders = dao.getAllServiceReminders()
        if (reminders.isEmpty()) return emptyList()
        val vehiclesById = dao.getVehicles().associateBy(VehicleEntity::id)
        val serviceTypesById = dao.getServiceTypes().associateBy(ServiceTypeEntity::id)
        val odometerByVehicle = reminders.map(ServiceReminderEntity::vehicleId).distinct().associateWith { vehicleId ->
            dao.getLatestOdometer(vehicleId)
        }
        return reminders.mapNotNull { entity ->
            val trigger = ReminderAlertEvaluator.evaluate(
                reminder = entity.toDomain(),
                now = now,
                currentOdometer = odometerByVehicle[entity.vehicleId],
                timeThresholdPercent = preferences.reminderTimeAlertPercent,
                distanceThresholdPercent = preferences.reminderDistanceAlertPercent,
            ) ?: return@mapNotNull null
            ReminderAlert(
                reminderId = entity.id,
                vehicleId = entity.vehicleId,
                vehicleName = vehiclesById[entity.vehicleId]?.name.orEmpty(),
                serviceTypeId = entity.serviceTypeId,
                serviceTypeName = serviceTypesById[entity.serviceTypeId]?.name ?: "Service",
                dueDate = entity.dueDate,
                dueDistance = entity.dueDistance,
                currentOdometer = odometerByVehicle[entity.vehicleId],
                triggeredByTime = trigger.byTime,
                triggeredByDistance = trigger.byDistance,
            )
        }.sortedWith(
            compareBy<ReminderAlert> { it.dueDate ?: LocalDate.MAX }
                .thenBy { it.dueDistance ?: Double.MAX_VALUE },
        )
    }

    suspend fun getUpcomingReminderWidgets(limit: Int = 3): List<ReminderWidgetItem> {
        val reminders = dao.getAllServiceReminders()
        if (reminders.isEmpty()) return emptyList()
        val vehiclesById = dao.getVehicles().associateBy(VehicleEntity::id)
        val serviceTypesById = dao.getServiceTypes().associateBy(ServiceTypeEntity::id)
        return reminders
            .sortedWith(compareBy<ServiceReminderEntity> { it.dueDate ?: LocalDate.MAX }.thenBy { it.dueDistance ?: Double.MAX_VALUE })
            .take(limit.coerceAtLeast(1))
            .map { reminder ->
                ReminderWidgetItem(
                    reminderId = reminder.id,
                    vehicleName = vehiclesById[reminder.vehicleId]?.name.orEmpty(),
                    serviceTypeName = serviceTypesById[reminder.serviceTypeId]?.name ?: "Service",
                    dueDate = reminder.dueDate,
                    dueDistance = reminder.dueDistance,
                )
            }
    }

    suspend fun getFuelWidgetItems(
        metric: FuelWidgetMetric,
        limit: Int = 3,
    ): List<FuelWidgetItem> {
        val preferences = preferencesRepository.currentSnapshot()
        val vehicles = dao.getVehicles().map(VehicleEntity::toDomain)
        val fillUpsByVehicle = dao.getAllFillUps()
            .map(FillUpRecordEntity::toDomain)
            .groupBy(FillUpRecord::vehicleId)

        val candidateVehicles = vehicles
            .filter { it.lifecycle == com.garageledger.domain.model.VehicleLifecycle.ACTIVE && !fillUpsByVehicle[it.id].isNullOrEmpty() }
            .ifEmpty { vehicles.filter { !fillUpsByVehicle[it.id].isNullOrEmpty() } }

        return candidateVehicles
            .mapNotNull { vehicle ->
                val fillUps = fillUpsByVehicle[vehicle.id].orEmpty().sortedBy(FillUpRecord::dateTime)
                if (fillUps.isEmpty()) return@mapNotNull null
                val latest = fillUps.last()
                when (metric) {
                    FuelWidgetMetric.FUEL_EFFICIENCY -> {
                        val efficiencyValues = fillUps.mapNotNull { it.fuelEfficiency ?: it.importedFuelEfficiency }
                        if (efficiencyValues.isEmpty()) return@mapNotNull null
                        FuelWidgetItem(
                            vehicleId = vehicle.id,
                            vehicleName = vehicle.name,
                            metric = metric,
                            latestValue = latest.fuelEfficiency ?: latest.importedFuelEfficiency,
                            averageValue = efficiencyValues.average(),
                            unitLabel = latest.fuelEfficiencyUnit?.storageValue
                                ?: vehicle.fuelEfficiencyUnitOverride?.storageValue
                                ?: preferences.fuelEfficiencyUnit.storageValue,
                        )
                    }

                    FuelWidgetMetric.FUEL_PRICE -> FuelWidgetItem(
                        vehicleId = vehicle.id,
                        vehicleName = vehicle.name,
                        metric = metric,
                        latestValue = latest.pricePerUnit,
                        averageValue = fillUps.map(FillUpRecord::pricePerUnit).average(),
                        unitLabel = "${preferences.currencySymbol}/${latest.volumeUnit.storageValue}",
                    )
                }
            }
            .sortedByDescending { item ->
                fillUpsByVehicle[item.vehicleId]
                    ?.maxOfOrNull(FillUpRecord::dateTime)
                    ?: LocalDateTime.MIN
            }
            .take(limit.coerceAtLeast(1))
    }

    suspend fun markReminderAlertsDelivered(
        alerts: List<ReminderAlert>,
        deliveredAt: LocalDateTime = LocalDateTime.now(),
    ) {
        if (alerts.isEmpty()) return
        val remindersById = dao.getServiceReminders(alerts.map(ReminderAlert::reminderId)).associateBy(ServiceReminderEntity::id)
        val updated = alerts.mapNotNull { alert ->
            remindersById[alert.reminderId]?.copy(
                lastTimeAlert = if (alert.triggeredByTime) deliveredAt else remindersById[alert.reminderId]?.lastTimeAlert,
                lastDistanceAlert = if (alert.triggeredByDistance) deliveredAt else remindersById[alert.reminderId]?.lastDistanceAlert,
            )
        }
        if (updated.isNotEmpty()) {
            dao.updateReminders(updated)
            onLedgerChanged()
        }
    }

    fun observeBrowseRecords(): Flow<List<BrowseRecordItem>> = combine(
        combine(
            dao.observeVehicles(),
            dao.observeAllFillUps(),
            dao.observeAllServices(),
            dao.observeAllExpenses(),
            dao.observeAllTrips(),
        ) { vehicles, fillUps, services, expenses, trips ->
            BrowsePrimarySnapshot(
                vehicles = vehicles,
                fillUps = fillUps,
                services = services,
                expenses = expenses,
                trips = trips,
            )
        },
        combine(
            dao.observeServiceTypes(),
            dao.observeExpenseTypes(),
            dao.observeTripTypes(),
            dao.observeServiceRecordCrossRefs(),
            dao.observeExpenseRecordCrossRefs(),
        ) { serviceTypes, expenseTypes, tripTypes, serviceCrossRefs, expenseCrossRefs ->
            BrowseSecondarySnapshot(
                serviceTypes = serviceTypes,
                expenseTypes = expenseTypes,
                tripTypes = tripTypes,
                serviceCrossRefs = serviceCrossRefs,
                expenseCrossRefs = expenseCrossRefs,
            )
        },
    ) { primary, secondary ->
        buildBrowseRecords(primary, secondary)
    }

    suspend fun importAcarCsv(inputStream: InputStream): ImportReport = persistImportedData(
        sourceLabel = "aCar CSV",
        imported = acarCsvImporter.import(inputStream),
        replaceExisting = false,
    )

    suspend fun importAcarAbp(inputStream: InputStream): ImportReport = persistImportedData(
        sourceLabel = "aCar ABP",
        imported = acarAbpImporter.import(inputStream),
        replaceExisting = true,
    )

    suspend fun previewFuellyCsv(inputStream: InputStream): FuellyCsvPreview = fuellyCsvImporter.preview(inputStream)

    suspend fun importFuellyCsv(
        inputStream: InputStream,
        config: FuellyCsvImportConfig,
    ): ImportReport {
        val imported = fuellyCsvImporter.importFillUps(inputStream, config)
        return persistImportedData(
            sourceLabel = "Fuelly CSV",
            imported = ImportedGarageData(
                fillUpRecords = imported.fillUpRecords,
                issues = imported.issues,
            ),
            replaceExisting = false,
        )
    }

    suspend fun saveFillUp(record: FillUpRecord): Long {
        validateChronology(
            vehicleId = record.vehicleId,
            excludedRecordId = record.id,
            candidates = listOf(ChronoOdometerRecord(record.id, record.dateTime, record.odometerReading)),
        )
        val savedId = if (record.id == 0L) {
            dao.insertFillUp(record.toEntity())
        } else {
            dao.updateFillUp(record.toEntity())
            record.id
        }
        recalculateVehicleFillUps(record.vehicleId)
        onLedgerChanged()
        return savedId
    }

    suspend fun saveService(record: ServiceRecord): Long {
        validateChronology(
            vehicleId = record.vehicleId,
            excludedRecordId = record.id,
            candidates = listOf(ChronoOdometerRecord(record.id, record.dateTime, record.odometerReading)),
        )
        val savedId = database.withTransaction {
            val recordId = if (record.id == 0L) {
                dao.insertService(record.toEntity())
            } else {
                dao.updateService(record.toEntity())
                dao.deleteServiceCrossRefsForRecord(record.id)
                record.id
            }
            if (record.serviceTypeIds.isNotEmpty()) {
                dao.insertServiceCrossRefs(
                    record.serviceTypeIds.distinct().map { typeId ->
                        ServiceRecordTypeCrossRef(recordId, typeId)
                    },
                )
            }
            recordId
        }
        recalculateVehicleReminders(record.vehicleId)
        onLedgerChanged()
        return savedId
    }

    suspend fun saveExpense(record: ExpenseRecord): Long {
        validateChronology(
            vehicleId = record.vehicleId,
            excludedRecordId = record.id,
            candidates = listOf(ChronoOdometerRecord(record.id, record.dateTime, record.odometerReading)),
        )
        val savedId = database.withTransaction {
            val recordId = if (record.id == 0L) {
                dao.insertExpense(record.toEntity())
            } else {
                dao.updateExpense(record.toEntity())
                dao.deleteExpenseCrossRefsForRecord(record.id)
                record.id
            }
            if (record.expenseTypeIds.isNotEmpty()) {
                dao.insertExpenseCrossRefs(
                    record.expenseTypeIds.distinct().map { typeId ->
                        ExpenseRecordTypeCrossRef(recordId, typeId)
                    },
                )
            }
            recordId
        }
        onLedgerChanged()
        return savedId
    }

    suspend fun saveTrip(record: TripRecord): Long {
        val normalized = normalizeTrip(record)
        val candidates = buildList {
            add(ChronoOdometerRecord(normalized.id, normalized.startDateTime, normalized.startOdometerReading))
            if (normalized.endDateTime != null && normalized.endOdometerReading != null) {
                add(ChronoOdometerRecord(normalized.id, normalized.endDateTime, normalized.endOdometerReading))
            }
        }
        validateChronology(
            vehicleId = normalized.vehicleId,
            excludedRecordId = normalized.id,
            candidates = candidates,
        )
        val savedId = if (normalized.id == 0L) {
            dao.insertTrip(normalized.toEntity())
        } else {
            dao.updateTrip(normalized.toEntity())
            normalized.id
        }
        onLedgerChanged()
        return savedId
    }

    suspend fun deleteFillUp(recordId: Long) {
        val record = dao.getFillUp(recordId)?.toDomain() ?: return
        database.withTransaction {
            dao.deleteRecordAttachmentsForRecord(RecordFamily.FILL_UP, recordId)
            dao.deleteFillUp(recordId)
            recalculateVehicleFillUps(record.vehicleId)
        }
        onLedgerChanged()
    }

    suspend fun deleteService(recordId: Long) {
        val record = getService(recordId) ?: return
        database.withTransaction {
            dao.deleteRecordAttachmentsForRecord(RecordFamily.SERVICE, recordId)
            dao.deleteServiceCrossRefsForRecord(recordId)
            dao.deleteService(recordId)
            recalculateVehicleReminders(record.vehicleId)
        }
        onLedgerChanged()
    }

    suspend fun deleteExpense(recordId: Long) {
        val record = getExpense(recordId) ?: return
        database.withTransaction {
            dao.deleteRecordAttachmentsForRecord(RecordFamily.EXPENSE, recordId)
            dao.deleteExpenseCrossRefsForRecord(recordId)
            dao.deleteExpense(recordId)
        }
        onLedgerChanged()
    }

    suspend fun deleteTrip(recordId: Long) {
        val record = dao.getTrip(recordId)?.toDomain() ?: return
        database.withTransaction {
            dao.deleteRecordAttachmentsForRecord(RecordFamily.TRIP, recordId)
            dao.deleteTrip(recordId)
        }
        onLedgerChanged()
    }

    suspend fun saveFuelType(type: FuelType): Long = database.withTransaction {
        if (type.id == 0L) {
            dao.insertFuelTypes(listOf(type.toEntity(idOverride = 0L))).single()
        } else {
            dao.updateFuelType(type.toEntity())
            type.id
        }
    }.also { onLedgerChanged() }

    suspend fun saveServiceType(type: ServiceType): Long = database.withTransaction {
        if (type.id == 0L) {
            dao.insertServiceTypes(listOf(type.toEntity(idOverride = 0L))).single()
        } else {
            dao.updateServiceType(type.toEntity())
            type.id
        }
    }.also { onLedgerChanged() }

    suspend fun saveExpenseType(type: ExpenseType): Long = database.withTransaction {
        if (type.id == 0L) {
            dao.insertExpenseTypes(listOf(type.toEntity(idOverride = 0L))).single()
        } else {
            dao.updateExpenseType(type.toEntity())
            type.id
        }
    }.also { onLedgerChanged() }

    suspend fun saveTripType(type: TripType): Long = database.withTransaction {
        if (type.id == 0L) {
            dao.insertTripTypes(listOf(type.toEntity(idOverride = 0L))).single()
        } else {
            dao.updateTripType(type.toEntity())
            type.id
        }
    }.also { onLedgerChanged() }

    suspend fun deleteFuelType(typeId: Long) {
        val inUse = dao.countFuelTypeUsage(typeId)
        if (inUse > 0) {
            throw IllegalArgumentException("This fuel type is already used by $inUse fill-up record(s).")
        }
        database.withTransaction { dao.deleteFuelType(typeId) }
        onLedgerChanged()
    }

    suspend fun deleteServiceType(typeId: Long) {
        val recordUsage = dao.countServiceTypeRecordUsage(typeId)
        val reminderUsage = dao.countServiceTypeReminderUsage(typeId)
        val totalUsage = recordUsage + reminderUsage
        if (totalUsage > 0) {
            throw IllegalArgumentException("This service type is already used by records or reminders.")
        }
        database.withTransaction { dao.deleteServiceType(typeId) }
        onLedgerChanged()
    }

    suspend fun deleteExpenseType(typeId: Long) {
        val inUse = dao.countExpenseTypeUsage(typeId)
        if (inUse > 0) {
            throw IllegalArgumentException("This expense type is already used by $inUse expense record(s).")
        }
        database.withTransaction { dao.deleteExpenseType(typeId) }
        onLedgerChanged()
    }

    suspend fun deleteTripType(typeId: Long) {
        val inUse = dao.countTripTypeUsage(typeId)
        if (inUse > 0) {
            throw IllegalArgumentException("This trip type is already used by $inUse trip record(s).")
        }
        database.withTransaction { dao.deleteTripType(typeId) }
        onLedgerChanged()
    }

    suspend fun replaceRecordAttachments(
        vehicleId: Long,
        recordFamily: RecordFamily,
        recordId: Long,
        attachments: List<RecordAttachment>,
    ) {
        database.withTransaction {
            dao.deleteRecordAttachmentsForRecord(recordFamily, recordId)
            if (attachments.isNotEmpty()) {
                dao.insertRecordAttachments(
                    attachments.map { attachment ->
                        attachment.copy(
                            id = 0L,
                            vehicleId = vehicleId,
                            recordFamily = recordFamily,
                            recordId = recordId,
                        ).toEntity()
                    },
                )
            }
        }
        onLedgerChanged()
    }

    private suspend fun persistImportedData(
        sourceLabel: String,
        imported: ImportedGarageData,
        replaceExisting: Boolean,
    ): ImportReport {
        val affectedVehicleIds = mutableSetOf<Long>()
        database.withTransaction {
            if (replaceExisting) {
                dao.clearServiceCrossRefs()
                dao.clearExpenseCrossRefs()
                dao.clearAttachments()
                dao.clearServiceReminders()
                dao.clearFillUps()
                dao.clearServices()
                dao.clearExpenses()
                dao.clearTrips()
                dao.clearVehicleParts()
                dao.clearFuelTypes()
                dao.clearServiceTypes()
                dao.clearExpenseTypes()
                dao.clearTripTypes()
                dao.clearVehicles()
            }

            val vehicleIdMap = upsertVehicles(imported.vehicles, replaceExisting)
            affectedVehicleIds += vehicleIdMap.values

            val serviceTypeIdMap = upsertServiceTypes(imported.serviceTypes, replaceExisting)
            val expenseTypeIdMap = upsertExpenseTypes(imported.expenseTypes, replaceExisting)
            val tripTypeIdMap = upsertTripTypes(imported.tripTypes, replaceExisting)
            val fuelTypeIdMap = upsertFuelTypes(imported.fuelTypes, replaceExisting)

            if (imported.vehicleParts.isNotEmpty()) {
                dao.insertVehicleParts(
                    imported.vehicleParts.mapNotNull { part ->
                        val mappedVehicleId = vehicleIdMap[part.vehicleId] ?: return@mapNotNull null
                        part.toEntity(vehicleIdOverride = mappedVehicleId).copy(id = 0L)
                    },
                )
            }

            if (imported.fillUpRecords.isNotEmpty()) {
                dao.insertFillUps(
                    imported.fillUpRecords.mapNotNull { fillUp ->
                        val mappedVehicleId = vehicleIdMap[fillUp.vehicleId] ?: return@mapNotNull null
                        fillUp.toEntity(vehicleIdOverride = mappedVehicleId).copy(
                            id = 0L,
                            fuelTypeId = fillUp.fuelTypeId?.let(fuelTypeIdMap::get),
                        )
                    },
                )
            }

            if (imported.serviceRecords.isNotEmpty()) {
                val serviceEntities = imported.serviceRecords.mapNotNull { record ->
                    val mappedVehicleId = vehicleIdMap[record.vehicleId] ?: return@mapNotNull null
                    record.toEntity(vehicleIdOverride = mappedVehicleId).copy(id = 0L)
                }
                val serviceIds = dao.insertServices(serviceEntities)
                val crossRefs = imported.serviceRecords.zip(serviceIds).flatMap { (record, insertedId) ->
                    record.serviceTypeIds.mapNotNull { legacyTypeId ->
                        serviceTypeIdMap[legacyTypeId]?.let { ServiceRecordTypeCrossRef(insertedId, it) }
                    }
                }
                dao.insertServiceCrossRefs(crossRefs)
            }

            if (imported.expenseRecords.isNotEmpty()) {
                val expenseEntities = imported.expenseRecords.mapNotNull { record ->
                    val mappedVehicleId = vehicleIdMap[record.vehicleId] ?: return@mapNotNull null
                    record.toEntity(vehicleIdOverride = mappedVehicleId).copy(id = 0L)
                }
                val expenseIds = dao.insertExpenses(expenseEntities)
                val crossRefs = imported.expenseRecords.zip(expenseIds).flatMap { (record, insertedId) ->
                    record.expenseTypeIds.mapNotNull { legacyTypeId ->
                        expenseTypeIdMap[legacyTypeId]?.let { ExpenseRecordTypeCrossRef(insertedId, it) }
                    }
                }
                dao.insertExpenseCrossRefs(crossRefs)
            }

            if (imported.tripRecords.isNotEmpty()) {
                dao.insertTrips(
                    imported.tripRecords.mapNotNull { trip ->
                        val mappedVehicleId = vehicleIdMap[trip.vehicleId] ?: return@mapNotNull null
                        trip.toEntity(vehicleIdOverride = mappedVehicleId).copy(
                            id = 0L,
                            tripTypeId = trip.tripTypeId?.let(tripTypeIdMap::get),
                        )
                    },
                )
            }

            if (imported.serviceReminders.isNotEmpty()) {
                dao.insertServiceReminders(
                    imported.serviceReminders.mapNotNull { reminder ->
                        val mappedVehicleId = vehicleIdMap[reminder.vehicleId] ?: return@mapNotNull null
                        val mappedServiceTypeId = serviceTypeIdMap[reminder.serviceTypeId] ?: return@mapNotNull null
                        reminder.toEntity(
                            vehicleIdOverride = mappedVehicleId,
                            serviceTypeIdOverride = mappedServiceTypeId,
                        ).copy(id = 0L)
                    },
                )
            }
        }

        if (imported.preferences != null) {
            preferencesRepository.replace(imported.preferences)
        }
        affectedVehicleIds.forEach { vehicleId ->
            recalculateVehicleFillUps(vehicleId)
            recalculateVehicleReminders(vehicleId)
        }
        onLedgerChanged()

        return ImportReport(
            sourceLabel = sourceLabel,
            vehiclesImported = imported.vehicles.size,
            fillUpsImported = imported.fillUpRecords.size,
            serviceRecordsImported = imported.serviceRecords.size,
            expenseRecordsImported = imported.expenseRecords.size,
            tripRecordsImported = imported.tripRecords.size,
            vehiclePartsImported = imported.vehicleParts.size,
            serviceTypesImported = imported.serviceTypes.size,
            expenseTypesImported = imported.expenseTypes.size,
            tripTypesImported = imported.tripTypes.size,
            fuelTypesImported = imported.fuelTypes.size,
            skippedRows = imported.issues.count { it.severity == ImportIssue.Severity.ERROR },
            issues = imported.issues,
        )
    }

    private suspend fun upsertVehicles(items: List<Vehicle>, replaceExisting: Boolean): Map<Long, Long> {
        if (items.isEmpty()) return emptyMap()
        if (replaceExisting) {
            val inserted = dao.insertVehicles(items.map { it.toEntity(idOverride = 0L) })
            return items.zip(inserted).associate { (item, id) -> (item.legacySourceId ?: id) to id }
        }
        val existingByName = dao.getVehicles().associateBy { it.name.lowercase() }
        val result = mutableMapOf<Long, Long>()
        val toInsert = mutableListOf<Vehicle>()
        items.forEach { vehicle ->
            val existing = existingByName[vehicle.name.lowercase()]
            if (existing != null) {
                result[vehicle.legacySourceId ?: existing.id] = existing.id
            } else {
                toInsert += vehicle
            }
        }
        val inserted = if (toInsert.isNotEmpty()) dao.insertVehicles(toInsert.map { it.toEntity(idOverride = 0L) }) else emptyList()
        toInsert.zip(inserted).forEach { (vehicle, id) -> result[vehicle.legacySourceId ?: id] = id }
        return result
    }

    private suspend fun upsertServiceTypes(
        items: List<com.garageledger.domain.model.ServiceType>,
        replaceExisting: Boolean,
    ): Map<Long, Long> {
        if (items.isEmpty()) return emptyMap()
        if (replaceExisting) {
            val inserted = dao.insertServiceTypes(items.map { it.toEntity(idOverride = 0L) })
            return items.zip(inserted).associate { (item, id) -> (item.legacySourceId ?: id) to id }
        }
        val existing = dao.getServiceTypes().associateBy { it.name.lowercase() }
        val result = mutableMapOf<Long, Long>()
        val toInsert = mutableListOf<com.garageledger.domain.model.ServiceType>()
        items.forEach { item ->
            val match = existing[item.name.lowercase()]
            if (match != null) {
                result[item.legacySourceId ?: match.id] = match.id
            } else {
                toInsert += item
            }
        }
        val inserted = if (toInsert.isNotEmpty()) dao.insertServiceTypes(toInsert.map { it.toEntity(idOverride = 0L) }) else emptyList()
        toInsert.zip(inserted).forEach { (item, id) -> result[item.legacySourceId ?: id] = id }
        return result
    }

    private suspend fun upsertExpenseTypes(
        items: List<com.garageledger.domain.model.ExpenseType>,
        replaceExisting: Boolean,
    ): Map<Long, Long> {
        if (items.isEmpty()) return emptyMap()
        if (replaceExisting) {
            val inserted = dao.insertExpenseTypes(items.map { it.toEntity(idOverride = 0L) })
            return items.zip(inserted).associate { (item, id) -> (item.legacySourceId ?: id) to id }
        }
        val existing = dao.getExpenseTypes().associateBy { it.name.lowercase() }
        val result = mutableMapOf<Long, Long>()
        val toInsert = mutableListOf<com.garageledger.domain.model.ExpenseType>()
        items.forEach { item ->
            val match = existing[item.name.lowercase()]
            if (match != null) {
                result[item.legacySourceId ?: match.id] = match.id
            } else {
                toInsert += item
            }
        }
        val inserted = if (toInsert.isNotEmpty()) dao.insertExpenseTypes(toInsert.map { it.toEntity(idOverride = 0L) }) else emptyList()
        toInsert.zip(inserted).forEach { (item, id) -> result[item.legacySourceId ?: id] = id }
        return result
    }

    private suspend fun upsertTripTypes(
        items: List<com.garageledger.domain.model.TripType>,
        replaceExisting: Boolean,
    ): Map<Long, Long> {
        if (items.isEmpty()) return emptyMap()
        if (replaceExisting) {
            val inserted = dao.insertTripTypes(items.map { it.toEntity(idOverride = 0L) })
            return items.zip(inserted).associate { (item, id) -> (item.legacySourceId ?: id) to id }
        }
        val existing = dao.getTripTypes().associateBy { it.name.lowercase() }
        val result = mutableMapOf<Long, Long>()
        val toInsert = mutableListOf<com.garageledger.domain.model.TripType>()
        items.forEach { item ->
            val match = existing[item.name.lowercase()]
            if (match != null) {
                result[item.legacySourceId ?: match.id] = match.id
            } else {
                toInsert += item
            }
        }
        val inserted = if (toInsert.isNotEmpty()) dao.insertTripTypes(toInsert.map { it.toEntity(idOverride = 0L) }) else emptyList()
        toInsert.zip(inserted).forEach { (item, id) -> result[item.legacySourceId ?: id] = id }
        return result
    }

    private suspend fun upsertFuelTypes(items: List<com.garageledger.domain.model.FuelType>, replaceExisting: Boolean): Map<Long, Long> {
        if (items.isEmpty()) return emptyMap()
        if (replaceExisting) {
            val inserted = dao.insertFuelTypes(items.map { it.toEntity(idOverride = 0L) })
            return items.zip(inserted).associate { (item, id) -> (item.legacySourceId ?: id) to id }
        }
        val existing = dao.getFuelTypes().associateBy { "${it.category}|${it.grade}|${it.octane}|${it.cetane}".lowercase() }
        val result = mutableMapOf<Long, Long>()
        val toInsert = mutableListOf<com.garageledger.domain.model.FuelType>()
        items.forEach { fuelType ->
            val key = "${fuelType.category}|${fuelType.grade}|${fuelType.octane}|${fuelType.cetane}".lowercase()
            val match = existing[key]
            if (match != null) {
                result[fuelType.legacySourceId ?: match.id] = match.id
            } else {
                toInsert += fuelType
            }
        }
        val inserted = if (toInsert.isNotEmpty()) dao.insertFuelTypes(toInsert.map { it.toEntity(idOverride = 0L) }) else emptyList()
        toInsert.zip(inserted).forEach { (item, id) -> result[item.legacySourceId ?: id] = id }
        return result
    }

    private suspend fun validateChronology(
        vehicleId: Long,
        excludedRecordId: Long,
        candidates: List<ChronoOdometerRecord>,
    ) {
        val fillUps = dao.getVehicleFillUpsAscending(vehicleId)
            .filterNot { it.id == excludedRecordId }
            .map { ChronoOdometerRecord(it.id, it.dateTime, it.odometerReading) }
        val services = dao.getVehicleServicesAscending(vehicleId)
            .filterNot { it.id == excludedRecordId }
            .map { ChronoOdometerRecord(it.id, it.dateTime, it.odometerReading) }
        val expenses = dao.getVehicleExpensesAscending(vehicleId)
            .filterNot { it.id == excludedRecordId }
            .map { ChronoOdometerRecord(it.id, it.dateTime, it.odometerReading) }
        val trips = dao.getVehicleTripsAscending(vehicleId)
            .filterNot { it.id == excludedRecordId }
            .flatMap { trip ->
            buildList {
                add(ChronoOdometerRecord(trip.id, trip.startDateTime, trip.startOdometerReading))
                if (trip.endDateTime != null && trip.endOdometerReading != null) {
                    add(ChronoOdometerRecord(trip.id, trip.endDateTime, trip.endOdometerReading))
                }
            }
        }

        val result = RecordConsistencyValidator.validateTimeline(fillUps + services + expenses + trips + candidates)
        if (result is RecordConsistencyValidator.ValidationResult.Invalid) {
            throw IllegalArgumentException(result.message)
        }
    }

    private suspend fun normalizeTrip(record: TripRecord): TripRecord {
        if (record.endDateTime != null && record.endDateTime.isBefore(record.startDateTime)) {
            throw IllegalArgumentException("Trip end time must be on or after the start time.")
        }
        if (record.endOdometerReading != null && record.endOdometerReading < record.startOdometerReading) {
            throw IllegalArgumentException("Trip end odometer must be on or after the start odometer.")
        }

        val resolvedDistance = record.endOdometerReading?.let { end ->
            (end - record.startOdometerReading).takeIf { it >= 0.0 }
        } ?: record.distance
        val resolvedDuration = record.endDateTime?.let { end ->
            Duration.between(record.startDateTime, end).toMillis().takeIf { it >= 0L }
        } ?: record.durationMillis
        val taxRate = record.taxDeductionRate ?: record.tripTypeId?.let { tripTypeId ->
            dao.getTripTypes().firstOrNull { it.id == tripTypeId }?.defaultTaxDeductionRate
        }
        val taxAmount = if (taxRate != null && resolvedDistance != null) taxRate * resolvedDistance else record.taxDeductionAmount
        val reimbursementAmount = if (record.reimbursementRate != null && resolvedDistance != null) {
            record.reimbursementRate * resolvedDistance
        } else {
            record.reimbursementAmount
        }

        return record.copy(
            distance = resolvedDistance,
            durationMillis = resolvedDuration,
            taxDeductionRate = taxRate,
            taxDeductionAmount = taxAmount,
            reimbursementAmount = reimbursementAmount,
        )
    }

    private suspend fun recalculateVehicleFillUps(
        vehicleId: Long,
        preferencesSnapshot: AppPreferenceSnapshot? = null,
    ) {
        val fillUps = dao.getVehicleFillUpsAscending(vehicleId).map(FillUpRecordEntity::toDomain)
        if (fillUps.isEmpty()) return
        val resolvedPreferences = preferencesSnapshot ?: preferencesRepository.currentSnapshot()
        val fuelEfficiencyUnit = resolveFuelEfficiencyUnit(vehicleId, resolvedPreferences)
        val recalculated = FuelEfficiencyCalculator.recalculate(
            records = fillUps,
            assignmentMethod = resolvedPreferences.fuelEfficiencyAssignmentMethod,
            fuelEfficiencyUnit = fuelEfficiencyUnit,
        )
        dao.updateFillUps(recalculated.map(FillUpRecord::toEntity))
    }

    private suspend fun resolveFuelEfficiencyUnit(
        vehicleId: Long,
        preferencesSnapshot: AppPreferenceSnapshot,
    ): FuelEfficiencyUnit = dao.getVehicleEntity(vehicleId)
        ?.toDomain()
        ?.fuelEfficiencyUnitOverride
        ?: preferencesSnapshot.fuelEfficiencyUnit

    private suspend fun recalculateVehicleReminders(vehicleId: Long) {
        val reminders = dao.observeVehicleReminders(vehicleId).first()
        if (reminders.isEmpty()) return
        val services = dao.getVehicleServicesAscending(vehicleId)
        val crossRefs = dao.getServiceRecordCrossRefs(services.map(ServiceRecordEntity::id))
        val serviceTypeIdsByRecord = crossRefs.groupBy(ServiceRecordTypeCrossRef::serviceRecordId, ServiceRecordTypeCrossRef::serviceTypeId)
        val serviceHistory = services.map { entity -> entity.toDomain(serviceTypeIdsByRecord[entity.id].orEmpty()) }
        val currentOdometer = dao.getLatestOdometer(vehicleId)
        val updated = reminders.map { entity ->
            ReminderScheduler.schedule(
                reminder = entity.toDomain(),
                serviceHistory = serviceHistory,
                currentDate = LocalDate.now(),
                currentOdometer = currentOdometer,
            ).toEntity()
        }
        dao.updateReminders(updated)
    }

    private fun buildStats(
        vehicleId: Long,
        fillUps: List<FillUpRecord>,
        services: List<ServiceRecord>,
        expenses: List<ExpenseRecord> = emptyList(),
        trips: List<TripRecord> = emptyList(),
    ): VehicleStatistics {
        val usableEfficiencies = fillUps.mapNotNull { it.fuelEfficiency }
        val averageFuelEfficiency = if (usableEfficiencies.isEmpty()) null else usableEfficiencies.average()
        val pricePoints = fillUps.map { it.pricePerUnit }
        return VehicleStatistics(
            vehicleId = vehicleId,
            fillUpCount = fillUps.size,
            totalFuelVolume = fillUps.sumOf { it.volume },
            totalFuelCost = fillUps.sumOf { it.totalCost },
            totalDistance = fillUps.sumOf { it.distanceSincePrevious ?: 0.0 },
            averageFuelEfficiency = averageFuelEfficiency,
            lastFuelEfficiency = fillUps.sortedByDescending { it.dateTime }.firstNotNullOfOrNull { it.fuelEfficiency },
            averagePricePerUnit = if (pricePoints.isEmpty()) null else pricePoints.average(),
            serviceCostTotal = services.sumOf { it.totalCost },
            expenseCostTotal = expenses.sumOf { it.totalCost },
            tripDistanceTotal = trips.sumOf { it.distance ?: 0.0 },
        )
    }

    private fun buildBrowseRecords(
        primary: BrowsePrimarySnapshot,
        secondary: BrowseSecondarySnapshot,
    ): List<BrowseRecordItem> {
        val vehicleNamesById = primary.vehicles.associate { it.id to it.name }
        val serviceTypeNames = secondary.serviceTypes.associate { it.id to it.name }
        val expenseTypeNames = secondary.expenseTypes.associate { it.id to it.name }
        val tripTypeNames = secondary.tripTypes.associate { it.id to it.name }
        val serviceTypeNamesByRecord = secondary.serviceCrossRefs
            .groupBy(ServiceRecordTypeCrossRef::serviceRecordId, ServiceRecordTypeCrossRef::serviceTypeId)
            .mapValues { (_, ids) -> ids.mapNotNull(serviceTypeNames::get).distinct().sorted() }
        val expenseTypeNamesByRecord = secondary.expenseCrossRefs
            .groupBy(ExpenseRecordTypeCrossRef::expenseRecordId, ExpenseRecordTypeCrossRef::expenseTypeId)
            .mapValues { (_, ids) -> ids.mapNotNull(expenseTypeNames::get).distinct().sorted() }

        val fuelUps = primary.fillUps.map { fillUp ->
            BrowseRecordItem(
                recordId = fillUp.id,
                vehicleId = fillUp.vehicleId,
                vehicleName = vehicleNamesById[fillUp.vehicleId].orEmpty(),
                family = RecordFamily.FILL_UP,
                occurredAt = fillUp.dateTime,
                title = "Fuel-Up ${fillUp.volume.toPrettyNumber()} ${fillUp.volumeUnit}",
                subtitle = listOfNotNull(fillUp.fuelBrand.takeIf { it.isNotBlank() }, fillUp.stationAddress.takeIf { it.isNotBlank() }).joinToString(" | "),
                amount = fillUp.totalCost,
                odometerReading = fillUp.odometerReading,
                tags = fillUp.tags,
                searchText = buildSearchText(
                    vehicleNamesById[fillUp.vehicleId],
                    fillUp.paymentType,
                    fillUp.fuelBrand,
                    fillUp.stationAddress,
                    fillUp.notes,
                    fillUp.importedFuelTypeText,
                    fillUp.tags,
                ),
            )
        }
        val services = primary.services.map { service ->
            val typeNames = serviceTypeNamesByRecord[service.id].orEmpty()
            BrowseRecordItem(
                recordId = service.id,
                vehicleId = service.vehicleId,
                vehicleName = vehicleNamesById[service.vehicleId].orEmpty(),
                family = RecordFamily.SERVICE,
                occurredAt = service.dateTime,
                title = typeNames.takeIf { it.isNotEmpty() }?.joinToString(", ") ?: "Service",
                subtitle = listOfNotNull(service.serviceCenterName.takeIf { it.isNotBlank() }, service.serviceCenterAddress.takeIf { it.isNotBlank() }).joinToString(" | "),
                amount = service.totalCost,
                odometerReading = service.odometerReading,
                tags = service.tags,
                searchText = buildSearchText(
                    vehicleNamesById[service.vehicleId],
                    service.paymentType,
                    service.serviceCenterName,
                    service.serviceCenterAddress,
                    service.notes,
                    typeNames,
                    service.tags,
                ),
            )
        }
        val expenses = primary.expenses.map { expense ->
            val typeNames = expenseTypeNamesByRecord[expense.id].orEmpty()
            BrowseRecordItem(
                recordId = expense.id,
                vehicleId = expense.vehicleId,
                vehicleName = vehicleNamesById[expense.vehicleId].orEmpty(),
                family = RecordFamily.EXPENSE,
                occurredAt = expense.dateTime,
                title = typeNames.takeIf { it.isNotEmpty() }?.joinToString(", ") ?: "Expense",
                subtitle = listOfNotNull(expense.expenseCenterName.takeIf { it.isNotBlank() }, expense.expenseCenterAddress.takeIf { it.isNotBlank() }).joinToString(" | "),
                amount = expense.totalCost,
                odometerReading = expense.odometerReading,
                tags = expense.tags,
                searchText = buildSearchText(
                    vehicleNamesById[expense.vehicleId],
                    expense.paymentType,
                    expense.expenseCenterName,
                    expense.expenseCenterAddress,
                    expense.notes,
                    typeNames,
                    expense.tags,
                ),
            )
        }
        val trips = primary.trips.map { trip ->
            val typeName = trip.tripTypeId?.let(tripTypeNames::get)
            BrowseRecordItem(
                recordId = trip.id,
                vehicleId = trip.vehicleId,
                vehicleName = vehicleNamesById[trip.vehicleId].orEmpty(),
                family = RecordFamily.TRIP,
                occurredAt = trip.startDateTime,
                title = typeName ?: "Trip",
                subtitle = listOfNotNull(
                    trip.startLocation.takeIf { it.isNotBlank() },
                    trip.endLocation.takeIf { it.isNotBlank() },
                ).joinToString(" -> "),
                amount = trip.reimbursementAmount ?: trip.taxDeductionAmount,
                odometerReading = trip.startOdometerReading,
                tags = trip.tags,
                searchText = buildSearchText(
                    vehicleNamesById[trip.vehicleId],
                    trip.purpose,
                    trip.client,
                    trip.startLocation,
                    trip.endLocation,
                    typeName,
                    trip.notes,
                    trip.tags,
                ),
            )
        }

        return (fuelUps + services + expenses + trips).sortedByDescending(BrowseRecordItem::occurredAt)
    }

    private fun buildSearchText(vararg values: Any?): String = values.flatMap { value ->
        when (value) {
            null -> emptyList()
            is String -> listOf(value)
            is Iterable<*> -> value.filterIsInstance<String>()
            else -> listOf(value.toString())
        }
    }.joinToString(" ").lowercase()

    private fun normalizeSuggestions(values: List<String>): List<String> {
        val distinct = linkedMapOf<String, String>()
        values.forEach { value ->
            val normalized = value.trim()
            if (normalized.isNotBlank()) {
                distinct.putIfAbsent(normalized.lowercase(), normalized)
            }
        }
        return distinct.values.sortedBy(String::lowercase)
    }

    private fun Double.toPrettyNumber(): String = if (this % 1.0 == 0.0) {
        this.toInt().toString()
    } else {
        "%,.2f".format(this).replace(",", "")
    }

    private suspend fun buildExportSnapshot(): ExportSnapshot = ExportSnapshot(
        preferences = preferencesRepository.currentSnapshot(),
        vehicles = dao.getVehicles(),
        vehicleParts = dao.getAllVehicleParts(),
        fuelTypes = dao.getFuelTypes(),
        serviceTypes = dao.getServiceTypes(),
        expenseTypes = dao.getExpenseTypes(),
        tripTypes = dao.getTripTypes(),
        serviceReminders = dao.getAllServiceReminders(),
        fillUpRecords = dao.getAllFillUps(),
        serviceRecords = dao.getAllServices(),
        serviceRecordTypes = dao.getAllServiceRecordCrossRefs(),
        expenseRecords = dao.getAllExpenses(),
        expenseRecordTypes = dao.getAllExpenseRecordCrossRefs(),
        tripRecords = dao.getAllTrips(),
        attachments = dao.getAllAttachments(),
    )

    private fun buildStatisticsSource(snapshot: ExportSnapshot): StatisticsSource = StatisticsSource(
        preferences = snapshot.preferences,
        vehicles = snapshot.vehicles.map(VehicleEntity::toDomain),
        fillUps = snapshot.fillUpRecords.map(FillUpRecordEntity::toDomain),
        services = snapshot.serviceRecords.map { it.toDomain(emptyList()) },
        expenses = snapshot.expenseRecords.map { it.toDomain(emptyList()) },
        trips = snapshot.tripRecords.map(TripRecordEntity::toDomain),
    )

    private fun buildReminderDisplays(
        reminders: List<ServiceReminderEntity>,
        serviceTypes: List<ServiceTypeEntity>,
    ): List<ReminderDisplayItem> {
        val serviceTypeNames = serviceTypes.associate { it.id to it.name }
        return reminders
            .map(ServiceReminderEntity::toDomain)
            .sortedWith(compareBy({ it.dueDate ?: LocalDate.MAX }, { it.dueDistance ?: Double.MAX_VALUE }))
            .map { reminder ->
                ReminderDisplayItem(
                    reminder = reminder,
                    serviceTypeName = serviceTypeNames[reminder.serviceTypeId] ?: "Service",
                )
            }
    }

    private data class VehicleDetailPrimarySnapshot(
        val vehicle: VehicleEntity?,
        val parts: List<VehiclePartEntity>,
        val reminders: List<ServiceReminderEntity>,
        val fillUps: List<FillUpRecordEntity>,
        val services: List<ServiceRecordEntity>,
        val serviceTypes: List<ServiceTypeEntity>,
    )

    private data class VehicleDetailSecondarySnapshot(
        val expenses: List<ExpenseRecordEntity>,
        val trips: List<TripRecordEntity>,
    )

    private data class BrowsePrimarySnapshot(
        val vehicles: List<VehicleEntity>,
        val fillUps: List<FillUpRecordEntity>,
        val services: List<ServiceRecordEntity>,
        val expenses: List<ExpenseRecordEntity>,
        val trips: List<TripRecordEntity>,
    )

    private data class BrowseSecondarySnapshot(
        val serviceTypes: List<ServiceTypeEntity>,
        val expenseTypes: List<ExpenseTypeEntity>,
        val tripTypes: List<TripTypeEntity>,
        val serviceCrossRefs: List<ServiceRecordTypeCrossRef>,
        val expenseCrossRefs: List<ExpenseRecordTypeCrossRef>,
    )
}
