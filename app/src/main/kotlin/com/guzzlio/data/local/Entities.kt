package com.guzzlio.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.guzzlio.domain.model.RecordFamily
import java.time.LocalDate
import java.time.LocalDateTime

@Entity(tableName = "vehicles")
data class VehicleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val legacySourceId: Long? = null,
    val name: String,
    val type: String,
    val year: Int?,
    val make: String,
    val model: String,
    val submodel: String,
    val lifecycle: String,
    val country: String,
    val licensePlate: String,
    val vin: String,
    val insurancePolicy: String,
    val bodyStyle: String,
    val color: String,
    val engineDisplacement: String,
    val fuelTankCapacity: Double?,
    val purchasePrice: Double?,
    val purchaseOdometer: Double?,
    val purchaseDate: LocalDate?,
    val sellingPrice: Double?,
    val sellingOdometer: Double?,
    val sellingDate: LocalDate?,
    val notes: String,
    val profilePhotoUri: String?,
    val distanceUnitOverride: String?,
    val volumeUnitOverride: String?,
    val fuelEfficiencyUnitOverride: String?,
)

@Entity(tableName = "vehicle_parts")
data class VehiclePartEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val legacySourceId: Long? = null,
    val vehicleId: Long,
    val name: String,
    val partNumber: String,
    val type: String,
    val brand: String,
    val color: String,
    val size: String,
    val volume: Double?,
    val pressure: Double?,
    val quantity: Int?,
    val notes: String,
)

@Entity(tableName = "fuel_types")
data class FuelTypeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val legacySourceId: Long? = null,
    val category: String,
    val grade: String,
    val octane: Int?,
    val cetane: Int?,
    val notes: String,
)

@Entity(tableName = "service_types")
data class ServiceTypeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val legacySourceId: Long? = null,
    val name: String,
    val notes: String,
    val defaultTimeReminderMonths: Int,
    val defaultDistanceReminder: Double?,
)

@Entity(tableName = "expense_types")
data class ExpenseTypeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val legacySourceId: Long? = null,
    val name: String,
    val notes: String,
)

@Entity(tableName = "trip_types")
data class TripTypeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val legacySourceId: Long? = null,
    val name: String,
    val defaultTaxDeductionRate: Double?,
    val notes: String,
)

@Entity(tableName = "service_reminders")
data class ServiceReminderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val legacySourceId: Long? = null,
    val vehicleId: Long,
    val serviceTypeId: Long,
    val intervalTimeMonths: Int,
    val intervalDistance: Double?,
    val dueDate: LocalDate?,
    val dueDistance: Double?,
    val timeAlertSilent: Boolean,
    val distanceAlertSilent: Boolean,
    val lastTimeAlert: LocalDateTime?,
    val lastDistanceAlert: LocalDateTime?,
)

@Entity(tableName = "fillup_records")
data class FillUpRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val legacySourceId: Long? = null,
    val vehicleId: Long,
    val dateTime: LocalDateTime,
    val odometerReading: Double,
    val distanceUnit: String,
    val volume: Double,
    val volumeUnit: String,
    val pricePerUnit: Double,
    val totalCost: Double,
    val paymentType: String,
    val partial: Boolean,
    val previousMissedFillups: Boolean,
    val fuelEfficiency: Double?,
    val fuelEfficiencyUnit: String?,
    val importedFuelEfficiency: Double?,
    val fuelTypeId: Long?,
    val importedFuelTypeText: String?,
    val hasFuelAdditive: Boolean,
    val fuelAdditiveName: String,
    val fuelBrand: String,
    val stationAddress: String,
    val latitude: Double?,
    val longitude: Double?,
    val drivingMode: String,
    val cityDrivingPercentage: Int?,
    val highwayDrivingPercentage: Int?,
    val averageSpeed: Double?,
    val tags: List<String>,
    val notes: String,
    val distanceSincePrevious: Double?,
    val distanceTillNextFillUp: Double?,
    val timeSincePreviousMillis: Long?,
    val timeTillNextFillUpMillis: Long?,
    val distanceForFuelEfficiency: Double?,
    val volumeForFuelEfficiency: Double?,
)

@Entity(tableName = "service_records")
data class ServiceRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val legacySourceId: Long? = null,
    val vehicleId: Long,
    val dateTime: LocalDateTime,
    val odometerReading: Double,
    val distanceUnit: String,
    val totalCost: Double,
    val paymentType: String,
    val serviceCenterName: String,
    val serviceCenterAddress: String,
    val latitude: Double?,
    val longitude: Double?,
    val tags: List<String>,
    val notes: String,
)

@Entity(primaryKeys = ["serviceRecordId", "serviceTypeId"], tableName = "service_record_types")
data class ServiceRecordTypeCrossRef(
    val serviceRecordId: Long,
    val serviceTypeId: Long,
)

@Entity(tableName = "expense_records")
data class ExpenseRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val legacySourceId: Long? = null,
    val vehicleId: Long,
    val dateTime: LocalDateTime,
    val odometerReading: Double,
    val distanceUnit: String,
    val totalCost: Double,
    val paymentType: String,
    val expenseCenterName: String,
    val expenseCenterAddress: String,
    val latitude: Double?,
    val longitude: Double?,
    val tags: List<String>,
    val notes: String,
)

@Entity(primaryKeys = ["expenseRecordId", "expenseTypeId"], tableName = "expense_record_types")
data class ExpenseRecordTypeCrossRef(
    val expenseRecordId: Long,
    val expenseTypeId: Long,
)

@Entity(tableName = "trip_records")
data class TripRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val legacySourceId: Long? = null,
    val vehicleId: Long,
    val startDateTime: LocalDateTime,
    val startOdometerReading: Double,
    val startLocation: String,
    val endDateTime: LocalDateTime?,
    val endOdometerReading: Double?,
    val endLocation: String,
    val startLatitude: Double?,
    val startLongitude: Double?,
    val endLatitude: Double?,
    val endLongitude: Double?,
    val distanceUnit: String,
    val distance: Double?,
    val durationMillis: Long?,
    val tripTypeId: Long?,
    val purpose: String,
    val client: String,
    val taxDeductionRate: Double?,
    val taxDeductionAmount: Double?,
    val reimbursementRate: Double?,
    val reimbursementAmount: Double?,
    val paid: Boolean,
    val tags: List<String>,
    val notes: String,
)

@Entity(tableName = "record_attachments")
data class RecordAttachmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val vehicleId: Long,
    val recordFamily: RecordFamily,
    val recordId: Long,
    val uri: String,
    val mimeType: String,
    val displayName: String,
    val createdAt: LocalDateTime,
)
