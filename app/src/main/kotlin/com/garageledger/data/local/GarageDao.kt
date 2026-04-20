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

    @Query("SELECT * FROM fillup_records WHERE vehicleId = :vehicleId ORDER BY dateTime DESC")
    fun observeVehicleFillUps(vehicleId: Long): Flow<List<FillUpRecordEntity>>

    @Query("SELECT * FROM fillup_records WHERE vehicleId = :vehicleId ORDER BY dateTime ASC")
    suspend fun getVehicleFillUpsAscending(vehicleId: Long): List<FillUpRecordEntity>

    @Query("SELECT * FROM service_records WHERE vehicleId = :vehicleId ORDER BY dateTime DESC")
    fun observeVehicleServices(vehicleId: Long): Flow<List<ServiceRecordEntity>>

    @Query("SELECT * FROM service_records WHERE vehicleId = :vehicleId ORDER BY dateTime ASC")
    suspend fun getVehicleServicesAscending(vehicleId: Long): List<ServiceRecordEntity>

    @Query("SELECT * FROM expense_records WHERE vehicleId = :vehicleId ORDER BY dateTime ASC")
    suspend fun getVehicleExpensesAscending(vehicleId: Long): List<ExpenseRecordEntity>

    @Query("SELECT * FROM trip_records WHERE vehicleId = :vehicleId ORDER BY startDateTime ASC")
    suspend fun getVehicleTripsAscending(vehicleId: Long): List<TripRecordEntity>

    @Query("SELECT * FROM fillup_records WHERE id = :recordId")
    suspend fun getFillUp(recordId: Long): FillUpRecordEntity?

    @Query("SELECT * FROM service_types ORDER BY name COLLATE NOCASE")
    suspend fun getServiceTypes(): List<ServiceTypeEntity>

    @Query("SELECT * FROM expense_types ORDER BY name COLLATE NOCASE")
    suspend fun getExpenseTypes(): List<ExpenseTypeEntity>

    @Query("SELECT * FROM trip_types ORDER BY name COLLATE NOCASE")
    suspend fun getTripTypes(): List<TripTypeEntity>

    @Query("SELECT * FROM fuel_types ORDER BY category COLLATE NOCASE, grade COLLATE NOCASE")
    suspend fun getFuelTypes(): List<FuelTypeEntity>

    @Query("SELECT * FROM service_record_types WHERE serviceRecordId IN (:recordIds)")
    suspend fun getServiceRecordCrossRefs(recordIds: List<Long>): List<ServiceRecordTypeCrossRef>

    @Query("SELECT * FROM expense_record_types WHERE expenseRecordId IN (:recordIds)")
    suspend fun getExpenseRecordCrossRefs(recordIds: List<Long>): List<ExpenseRecordTypeCrossRef>

    @Query("SELECT DISTINCT paymentType FROM fillup_records WHERE paymentType <> '' ORDER BY paymentType COLLATE NOCASE")
    suspend fun getPaymentTypeSuggestions(): List<String>

    @Query("SELECT DISTINCT fuelBrand FROM fillup_records WHERE fuelBrand <> '' ORDER BY fuelBrand COLLATE NOCASE")
    suspend fun getFuelBrandSuggestions(): List<String>

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
    suspend fun insertFillUp(item: FillUpRecordEntity): Long

    @Update
    suspend fun updateFillUp(item: FillUpRecordEntity)

    @Update
    suspend fun updateFillUps(items: List<FillUpRecordEntity>)

    @Update
    suspend fun updateReminders(items: List<ServiceReminderEntity>)

    @Query("DELETE FROM service_record_types")
    suspend fun clearServiceCrossRefs()

    @Query("DELETE FROM expense_record_types")
    suspend fun clearExpenseCrossRefs()

    @Query("DELETE FROM record_attachments")
    suspend fun clearAttachments()

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
