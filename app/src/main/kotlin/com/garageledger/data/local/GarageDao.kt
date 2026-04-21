package com.garageledger.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface GarageDao {
    @Query("SELECT * FROM vehicles ORDER BY CASE WHEN lifecycle = 'ACTIVE' THEN 0 ELSE 1 END, name COLLATE NOCASE")
    fun observeVehicles(): Flow<List<VehicleEntity>>

    @Query("SELECT * FROM vehicles ORDER BY CASE WHEN lifecycle = 'ACTIVE' THEN 0 ELSE 1 END, name COLLATE NOCASE")
    suspend fun getVehicles(): List<VehicleEntity>

    @Query("SELECT * FROM vehicles WHERE id = :vehicleId")
    fun observeVehicle(vehicleId: Long): Flow<VehicleEntity?>

    @Query("SELECT * FROM vehicle_parts WHERE vehicleId = :vehicleId ORDER BY name COLLATE NOCASE")
    fun observeVehicleParts(vehicleId: Long): Flow<List<VehiclePartEntity>>

    @Query("SELECT * FROM service_reminders WHERE vehicleId = :vehicleId ORDER BY dueDate ASC, dueDistance ASC")
    fun observeVehicleReminders(vehicleId: Long): Flow<List<ServiceReminderEntity>>

    @Query("SELECT * FROM service_reminders ORDER BY vehicleId, dueDate ASC, dueDistance ASC")
    suspend fun getAllServiceReminders(): List<ServiceReminderEntity>

    @Query("SELECT * FROM service_reminders WHERE id IN (:reminderIds)")
    suspend fun getServiceReminders(reminderIds: List<Long>): List<ServiceReminderEntity>

    @Query("SELECT * FROM fillup_records WHERE vehicleId = :vehicleId ORDER BY dateTime DESC")
    fun observeVehicleFillUps(vehicleId: Long): Flow<List<FillUpRecordEntity>>

    @Query("SELECT * FROM fillup_records WHERE vehicleId = :vehicleId ORDER BY dateTime ASC")
    suspend fun getVehicleFillUpsAscending(vehicleId: Long): List<FillUpRecordEntity>

    @Query("SELECT * FROM fillup_records ORDER BY vehicleId, dateTime ASC")
    suspend fun getAllFillUps(): List<FillUpRecordEntity>

    @Query("SELECT * FROM service_records WHERE vehicleId = :vehicleId ORDER BY dateTime DESC")
    fun observeVehicleServices(vehicleId: Long): Flow<List<ServiceRecordEntity>>

    @Query("SELECT * FROM service_records WHERE vehicleId = :vehicleId ORDER BY dateTime ASC")
    suspend fun getVehicleServicesAscending(vehicleId: Long): List<ServiceRecordEntity>

    @Query("SELECT * FROM service_records ORDER BY vehicleId, dateTime ASC")
    suspend fun getAllServices(): List<ServiceRecordEntity>

    @Query("SELECT * FROM expense_records WHERE vehicleId = :vehicleId ORDER BY dateTime DESC")
    fun observeVehicleExpenses(vehicleId: Long): Flow<List<ExpenseRecordEntity>>

    @Query("SELECT * FROM expense_records WHERE vehicleId = :vehicleId ORDER BY dateTime ASC")
    suspend fun getVehicleExpensesAscending(vehicleId: Long): List<ExpenseRecordEntity>

    @Query("SELECT * FROM expense_records ORDER BY vehicleId, dateTime ASC")
    suspend fun getAllExpenses(): List<ExpenseRecordEntity>

    @Query("SELECT * FROM trip_records WHERE vehicleId = :vehicleId ORDER BY startDateTime DESC")
    fun observeVehicleTrips(vehicleId: Long): Flow<List<TripRecordEntity>>

    @Query("SELECT * FROM trip_records WHERE vehicleId = :vehicleId ORDER BY startDateTime ASC")
    suspend fun getVehicleTripsAscending(vehicleId: Long): List<TripRecordEntity>

    @Query("SELECT * FROM trip_records ORDER BY vehicleId, startDateTime ASC")
    suspend fun getAllTrips(): List<TripRecordEntity>

    @Query("SELECT * FROM fillup_records ORDER BY dateTime DESC")
    fun observeAllFillUps(): Flow<List<FillUpRecordEntity>>

    @Query("SELECT * FROM service_records ORDER BY dateTime DESC")
    fun observeAllServices(): Flow<List<ServiceRecordEntity>>

    @Query("SELECT * FROM expense_records ORDER BY dateTime DESC")
    fun observeAllExpenses(): Flow<List<ExpenseRecordEntity>>

    @Query("SELECT * FROM trip_records ORDER BY startDateTime DESC")
    fun observeAllTrips(): Flow<List<TripRecordEntity>>

    @Query("SELECT * FROM fillup_records WHERE id = :recordId")
    suspend fun getFillUp(recordId: Long): FillUpRecordEntity?

    @Query("SELECT * FROM service_records WHERE id = :recordId")
    suspend fun getService(recordId: Long): ServiceRecordEntity?

    @Query("SELECT * FROM expense_records WHERE id = :recordId")
    suspend fun getExpense(recordId: Long): ExpenseRecordEntity?

    @Query("SELECT * FROM trip_records WHERE id = :recordId")
    suspend fun getTrip(recordId: Long): TripRecordEntity?

    @Query("SELECT * FROM service_types ORDER BY name COLLATE NOCASE")
    fun observeServiceTypes(): Flow<List<ServiceTypeEntity>>

    @Query("SELECT * FROM service_types ORDER BY name COLLATE NOCASE")
    suspend fun getServiceTypes(): List<ServiceTypeEntity>

    @Query("SELECT * FROM expense_types ORDER BY name COLLATE NOCASE")
    fun observeExpenseTypes(): Flow<List<ExpenseTypeEntity>>

    @Query("SELECT * FROM expense_types ORDER BY name COLLATE NOCASE")
    suspend fun getExpenseTypes(): List<ExpenseTypeEntity>

    @Query("SELECT * FROM trip_types ORDER BY name COLLATE NOCASE")
    fun observeTripTypes(): Flow<List<TripTypeEntity>>

    @Query("SELECT * FROM trip_types ORDER BY name COLLATE NOCASE")
    suspend fun getTripTypes(): List<TripTypeEntity>

    @Query("SELECT * FROM fuel_types ORDER BY category COLLATE NOCASE, grade COLLATE NOCASE")
    fun observeFuelTypes(): Flow<List<FuelTypeEntity>>

    @Query("SELECT * FROM fuel_types ORDER BY category COLLATE NOCASE, grade COLLATE NOCASE")
    suspend fun getFuelTypes(): List<FuelTypeEntity>

    @Query("SELECT * FROM vehicle_parts ORDER BY vehicleId, name COLLATE NOCASE")
    suspend fun getAllVehicleParts(): List<VehiclePartEntity>

    @Query("SELECT * FROM record_attachments ORDER BY createdAt ASC")
    suspend fun getAllAttachments(): List<RecordAttachmentEntity>

    @Query("SELECT * FROM record_attachments WHERE recordFamily = :recordFamily AND recordId = :recordId ORDER BY createdAt ASC")
    suspend fun getRecordAttachments(recordFamily: com.garageledger.domain.model.RecordFamily, recordId: Long): List<RecordAttachmentEntity>

    @Query("SELECT * FROM service_record_types")
    fun observeServiceRecordCrossRefs(): Flow<List<ServiceRecordTypeCrossRef>>

    @Query("SELECT * FROM service_record_types")
    suspend fun getAllServiceRecordCrossRefs(): List<ServiceRecordTypeCrossRef>

    @Query("SELECT * FROM service_record_types WHERE serviceRecordId IN (:recordIds)")
    suspend fun getServiceRecordCrossRefs(recordIds: List<Long>): List<ServiceRecordTypeCrossRef>

    @Query("SELECT * FROM expense_record_types")
    fun observeExpenseRecordCrossRefs(): Flow<List<ExpenseRecordTypeCrossRef>>

    @Query("SELECT * FROM expense_record_types")
    suspend fun getAllExpenseRecordCrossRefs(): List<ExpenseRecordTypeCrossRef>

    @Query("SELECT * FROM expense_record_types WHERE expenseRecordId IN (:recordIds)")
    suspend fun getExpenseRecordCrossRefs(recordIds: List<Long>): List<ExpenseRecordTypeCrossRef>

    @Query(
        """
        SELECT value FROM (
            SELECT paymentType AS value FROM fillup_records WHERE paymentType <> ''
            UNION
            SELECT paymentType AS value FROM service_records WHERE paymentType <> ''
            UNION
            SELECT paymentType AS value FROM expense_records WHERE paymentType <> ''
        )
        ORDER BY value COLLATE NOCASE
        """,
    )
    suspend fun getPaymentTypeSuggestions(): List<String>

    @Query("SELECT DISTINCT fuelBrand FROM fillup_records WHERE fuelBrand <> '' ORDER BY fuelBrand COLLATE NOCASE")
    suspend fun getFuelBrandSuggestions(): List<String>

    @Query("SELECT DISTINCT serviceCenterName FROM service_records WHERE serviceCenterName <> '' ORDER BY serviceCenterName COLLATE NOCASE")
    suspend fun getServiceCenterSuggestions(): List<String>

    @Query("SELECT DISTINCT expenseCenterName FROM expense_records WHERE expenseCenterName <> '' ORDER BY expenseCenterName COLLATE NOCASE")
    suspend fun getExpenseCenterSuggestions(): List<String>

    @Query("SELECT DISTINCT purpose FROM trip_records WHERE purpose <> '' ORDER BY purpose COLLATE NOCASE")
    suspend fun getTripPurposeSuggestions(): List<String>

    @Query("SELECT DISTINCT client FROM trip_records WHERE client <> '' ORDER BY client COLLATE NOCASE")
    suspend fun getTripClientSuggestions(): List<String>

    @Query(
        """
        SELECT value FROM (
            SELECT startLocation AS value FROM trip_records WHERE startLocation <> ''
            UNION
            SELECT endLocation AS value FROM trip_records WHERE endLocation <> ''
        )
        ORDER BY value COLLATE NOCASE
        """,
    )
    suspend fun getTripLocationSuggestions(): List<String>

    @Query(
        """
        SELECT MAX(value) FROM (
            SELECT MAX(odometerReading) AS value FROM fillup_records WHERE vehicleId = :vehicleId
            UNION ALL
            SELECT MAX(odometerReading) AS value FROM service_records WHERE vehicleId = :vehicleId
            UNION ALL
            SELECT MAX(odometerReading) AS value FROM expense_records WHERE vehicleId = :vehicleId
            UNION ALL
            SELECT MAX(COALESCE(endOdometerReading, startOdometerReading)) AS value FROM trip_records WHERE vehicleId = :vehicleId
        )
        """
    )
    suspend fun getLatestOdometer(vehicleId: Long): Double?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVehicles(items: List<VehicleEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVehicleParts(items: List<VehiclePartEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFuelTypes(items: List<FuelTypeEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertServiceTypes(items: List<ServiceTypeEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpenseTypes(items: List<ExpenseTypeEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTripTypes(items: List<TripTypeEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertServiceReminders(items: List<ServiceReminderEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFillUps(items: List<FillUpRecordEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertServices(items: List<ServiceRecordEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertServiceCrossRefs(items: List<ServiceRecordTypeCrossRef>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpenses(items: List<ExpenseRecordEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpenseCrossRefs(items: List<ExpenseRecordTypeCrossRef>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrips(items: List<TripRecordEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecordAttachments(items: List<RecordAttachmentEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFillUp(item: FillUpRecordEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertService(item: ServiceRecordEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(item: ExpenseRecordEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrip(item: TripRecordEntity): Long

    @Update
    suspend fun updateFuelType(item: FuelTypeEntity)

    @Update
    suspend fun updateServiceType(item: ServiceTypeEntity)

    @Update
    suspend fun updateExpenseType(item: ExpenseTypeEntity)

    @Update
    suspend fun updateTripType(item: TripTypeEntity)

    @Update
    suspend fun updateFillUp(item: FillUpRecordEntity)

    @Update
    suspend fun updateService(item: ServiceRecordEntity)

    @Update
    suspend fun updateExpense(item: ExpenseRecordEntity)

    @Update
    suspend fun updateTrip(item: TripRecordEntity)

    @Update
    suspend fun updateFillUps(items: List<FillUpRecordEntity>)

    @Update
    suspend fun updateReminders(items: List<ServiceReminderEntity>)

    @Query("DELETE FROM service_record_types WHERE serviceRecordId = :recordId")
    suspend fun deleteServiceCrossRefsForRecord(recordId: Long)

    @Query("DELETE FROM expense_record_types WHERE expenseRecordId = :recordId")
    suspend fun deleteExpenseCrossRefsForRecord(recordId: Long)

    @Query("DELETE FROM service_record_types")
    suspend fun clearServiceCrossRefs()

    @Query("DELETE FROM expense_record_types")
    suspend fun clearExpenseCrossRefs()

    @Query("DELETE FROM record_attachments")
    suspend fun clearAttachments()

    @Query("DELETE FROM record_attachments WHERE recordFamily = :recordFamily AND recordId = :recordId")
    suspend fun deleteRecordAttachmentsForRecord(recordFamily: com.garageledger.domain.model.RecordFamily, recordId: Long)

    @Query("DELETE FROM fillup_records WHERE id = :recordId")
    suspend fun deleteFillUp(recordId: Long)

    @Query("DELETE FROM service_records WHERE id = :recordId")
    suspend fun deleteService(recordId: Long)

    @Query("DELETE FROM expense_records WHERE id = :recordId")
    suspend fun deleteExpense(recordId: Long)

    @Query("DELETE FROM trip_records WHERE id = :recordId")
    suspend fun deleteTrip(recordId: Long)

    @Query("DELETE FROM fuel_types WHERE id = :typeId")
    suspend fun deleteFuelType(typeId: Long)

    @Query("DELETE FROM service_types WHERE id = :typeId")
    suspend fun deleteServiceType(typeId: Long)

    @Query("DELETE FROM expense_types WHERE id = :typeId")
    suspend fun deleteExpenseType(typeId: Long)

    @Query("DELETE FROM trip_types WHERE id = :typeId")
    suspend fun deleteTripType(typeId: Long)

    @Query("SELECT COUNT(*) FROM fillup_records WHERE fuelTypeId = :typeId")
    suspend fun countFuelTypeUsage(typeId: Long): Int

    @Query("SELECT COUNT(*) FROM service_record_types WHERE serviceTypeId = :typeId")
    suspend fun countServiceTypeRecordUsage(typeId: Long): Int

    @Query("SELECT COUNT(*) FROM service_reminders WHERE serviceTypeId = :typeId")
    suspend fun countServiceTypeReminderUsage(typeId: Long): Int

    @Query("SELECT COUNT(*) FROM expense_record_types WHERE expenseTypeId = :typeId")
    suspend fun countExpenseTypeUsage(typeId: Long): Int

    @Query("SELECT COUNT(*) FROM trip_records WHERE tripTypeId = :typeId")
    suspend fun countTripTypeUsage(typeId: Long): Int

    @Query("DELETE FROM service_reminders")
    suspend fun clearServiceReminders()

    @Query("DELETE FROM fillup_records")
    suspend fun clearFillUps()

    @Query("DELETE FROM service_records")
    suspend fun clearServices()

    @Query("DELETE FROM expense_records")
    suspend fun clearExpenses()

    @Query("DELETE FROM trip_records")
    suspend fun clearTrips()

    @Query("DELETE FROM vehicle_parts")
    suspend fun clearVehicleParts()

    @Query("DELETE FROM fuel_types")
    suspend fun clearFuelTypes()

    @Query("DELETE FROM service_types")
    suspend fun clearServiceTypes()

    @Query("DELETE FROM expense_types")
    suspend fun clearExpenseTypes()

    @Query("DELETE FROM trip_types")
    suspend fun clearTripTypes()

    @Query("DELETE FROM vehicles")
    suspend fun clearVehicles()
}
