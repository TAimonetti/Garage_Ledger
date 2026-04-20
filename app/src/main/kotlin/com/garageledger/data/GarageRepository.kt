package com.garageledger.data

import androidx.room.withTransaction
import com.garageledger.core.model.FuelEfficiencyAssignmentMethod
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
import com.garageledger.domain.calc.ChronoOdometerRecord
import com.garageledger.domain.calc.FuelEfficiencyCalculator
import com.garageledger.domain.calc.RecordConsistencyValidator
import com.garageledger.domain.calc.ReminderScheduler
import com.garageledger.domain.model.ExpenseRecord
import com.garageledger.domain.model.FillUpRecord
import com.garageledger.domain.model.ImportIssue
import com.garageledger.domain.model.ImportReport
import com.garageledger.domain.model.ImportedGarageData
import com.garageledger.domain.model.ServiceRecord
import com.garageledger.domain.model.TripRecord
import com.garageledger.domain.model.Vehicle
import com.garageledger.domain.model.VehicleDetailBundle
import com.garageledger.domain.model.VehicleStatistics
import java.io.InputStream
import java.time.LocalDate
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
) {
    private val dao = database.garageDao()

    val preferences = preferencesRepository.preferences

    fun observeVehicles(): Flow<List<Vehicle>> = dao.observeVehicles().map { list -> list.map(VehicleEntity::toDomain) }

    fun observeVehicleDetail(vehicleId: Long): Flow<VehicleDetailBundle?> = combine(
        dao.observeVehicle(vehicleId),
        dao.observeVehicleParts(vehicleId),
        dao.observeVehicleReminders(vehicleId),
        dao.observeVehicleFillUps(vehicleId),
        dao.observeVehicleServices(vehicleId),
    ) { vehicle, parts, reminders, fillUps, services ->
        val currentVehicle = vehicle?.toDomain() ?: return@combine null
        val domainFillUps = fillUps.map(FillUpRecordEntity::toDomain)
        val domainServices = services.map { it.toDomain(emptyList()) }
        val stats = buildStats(vehicleId, domainFillUps, domainServices)
        VehicleDetailBundle(
            vehicle = currentVehicle,
            parts = parts.map(VehiclePartEntity::toDomain),
            reminders = reminders.map(ServiceReminderEntity::toDomain),
            recentFillUps = domainFillUps.sortedByDescending { it.dateTime }.take(15),
            recentServices = domainServices.sortedByDescending { it.dateTime }.take(10),
            stats = stats,
        )
    }

    suspend fun getFillUp(recordId: Long): FillUpRecord? = dao.getFillUp(recordId)?.toDomain()

    suspend fun getVehicleFillUps(vehicleId: Long): List<FillUpRecord> =
        dao.getVehicleFillUpsAscending(vehicleId).map(FillUpRecordEntity::toDomain)

    suspend fun getPaymentTypeSuggestions(): List<String> = dao.getPaymentTypeSuggestions()

    suspend fun getFuelBrandSuggestions(): List<String> = dao.getFuelBrandSuggestions()

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

    suspend fun importFuellyCsv(
        inputStream: InputStream,
        vehicleId: Long,
        fieldMapping: Map<String, String> = emptyMap(),
    ): ImportReport {
        val fillUps = fuellyCsvImporter.importFillUps(inputStream, vehicleId, fieldMapping)
        return persistImportedData(
            sourceLabel = "Fuelly CSV",
            imported = ImportedGarageData(fillUpRecords = fillUps),
            replaceExisting = false,
        )
    }

    suspend fun saveFillUp(record: FillUpRecord): Long {
        validateChronology(record)
        val savedId = if (record.id == 0L) {
            dao.insertFillUp(record.toEntity())
        } else {
            dao.updateFillUp(record.toEntity())
            record.id
        }
        recalculateVehicleFillUps(record.vehicleId)
        return savedId
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

    private suspend fun validateChronology(candidate: FillUpRecord) {
        val fillUps = dao.getVehicleFillUpsAscending(candidate.vehicleId)
            .filterNot { it.id == candidate.id }
            .map { ChronoOdometerRecord(it.id, it.dateTime, it.odometerReading) }
        val services = dao.getVehicleServicesAscending(candidate.vehicleId)
            .map { ChronoOdometerRecord(it.id, it.dateTime, it.odometerReading) }
        val expenses = dao.getVehicleExpensesAscending(candidate.vehicleId)
            .map { ChronoOdometerRecord(it.id, it.dateTime, it.odometerReading) }
        val trips = dao.getVehicleTripsAscending(candidate.vehicleId).flatMap { trip ->
            buildList {
                add(ChronoOdometerRecord(trip.id, trip.startDateTime, trip.startOdometerReading))
                if (trip.endDateTime != null && trip.endOdometerReading != null) {
                    add(ChronoOdometerRecord(trip.id, trip.endDateTime, trip.endOdometerReading))
                }
            }
        }

        val result = RecordConsistencyValidator.validateInsertion(
            siblings = fillUps + services + expenses + trips,
            candidate = ChronoOdometerRecord(candidate.id, candidate.dateTime, candidate.odometerReading),
        )
        if (result is RecordConsistencyValidator.ValidationResult.Invalid) {
            throw IllegalArgumentException(result.message)
        }
    }

    private suspend fun recalculateVehicleFillUps(vehicleId: Long) {
        val fillUps = dao.getVehicleFillUpsAscending(vehicleId).map(FillUpRecordEntity::toDomain)
        if (fillUps.isEmpty()) return
        val assignmentMethod = preferences.first().fuelEfficiencyAssignmentMethod
        val recalculated = FuelEfficiencyCalculator.recalculate(fillUps, assignmentMethod)
        dao.updateFillUps(recalculated.map(FillUpRecord::toEntity))
    }

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
}
