package com.garageledger.domain.model

import com.garageledger.core.model.DistanceUnit
import com.garageledger.core.model.FuelEfficiencyAssignmentMethod
import com.garageledger.core.model.FuelEfficiencyUnit
import com.garageledger.core.model.VolumeUnit
import java.time.LocalDate
import java.time.LocalDateTime
import kotlinx.serialization.Serializable

@Serializable
enum class VehicleLifecycle {
    ACTIVE,
    RETIRED,
}

@Serializable
enum class RecordFamily {
    FILL_UP,
    SERVICE,
    EXPENSE,
    TRIP,
}

@Serializable
enum class OptionalFieldToggle(val storageKey: String) {
    VEHICLE_LICENSE_PLATE("vehicle_license_plate"),
    VEHICLE_VIN("vehicle_vin"),
    VEHICLE_INSURANCE_POLICY("vehicle_insurance_policy"),
    VEHICLE_BODY_STYLE("vehicle_body_style"),
    VEHICLE_COLOR("vehicle_color"),
    VEHICLE_ENGINE_DISPLACEMENT("vehicle_engine_displacement"),
    VEHICLE_FUEL_TANK_CAPACITY("vehicle_fuel_tank_capacity"),
    VEHICLE_PURCHASE_INFO("vehicle_purchase_info"),
    VEHICLE_SELLING_INFO("vehicle_selling_info"),
    PAYMENT_TYPE("payment_type"),
    FUEL_TYPE("fuel_type"),
    FUEL_ADDITIVE("fuel_additive"),
    FUELING_STATION("fueling_station"),
    SERVICE_CENTER("service_center"),
    EXPENSE_CENTER("expense_center"),
    TAGS("tags"),
    NOTES("notes"),
    AVERAGE_SPEED("average_speed"),
    DRIVING_MODE("driving_mode"),
    DRIVING_CONDITION("driving_condition"),
    TRIP_PURPOSE("trip_purpose"),
    TRIP_CLIENT("trip_client"),
    TRIP_LOCATION("trip_location"),
    TRIP_TAX_DEDUCTION("trip_tax_deduction"),
    TRIP_REIMBURSEMENT("trip_reimbursement"),
}

@Serializable
data class AppPreferenceSnapshot(
    val distanceUnit: DistanceUnit = DistanceUnit.MILES,
    val volumeUnit: VolumeUnit = VolumeUnit.GALLONS_US,
    val fuelEfficiencyUnit: FuelEfficiencyUnit = FuelEfficiencyUnit.MPG_US,
    val currencySymbol: String = "$",
    val localeTag: String = "system",
    val fullDateFormat: String = "MMM dd, yyyy",
    val compactDateFormat: String = "MM/dd/yy",
    val browseSortDescending: Boolean = true,
    val fuelEfficiencyAssignmentMethod: FuelEfficiencyAssignmentMethod = FuelEfficiencyAssignmentMethod.PREVIOUS_RECORD,
    val reminderTimeAlertPercent: Int = 10,
    val reminderDistanceAlertPercent: Int = 10,
    val backupFrequencyHours: Int = 720,
    val backupHistoryCount: Int = 10,
    val useLocation: Boolean = true,
    val notificationsEnabled: Boolean = false,
    val notificationLedEnabled: Boolean = true,
    val visibleFields: Set<OptionalFieldToggle> = OptionalFieldToggle.entries.toSet(),
    val savedBrowseSearches: List<SavedBrowseSearch> = emptyList(),
)

data class Vehicle(
    val id: Long = 0L,
    val legacySourceId: Long? = null,
    val name: String,
    val type: String = "",
    val year: Int? = null,
    val make: String = "",
    val model: String = "",
    val submodel: String = "",
    val lifecycle: VehicleLifecycle = VehicleLifecycle.ACTIVE,
    val country: String = "",
    val licensePlate: String = "",
    val vin: String = "",
    val insurancePolicy: String = "",
    val bodyStyle: String = "",
    val color: String = "",
    val engineDisplacement: String = "",
    val fuelTankCapacity: Double? = null,
    val purchasePrice: Double? = null,
    val purchaseOdometer: Double? = null,
    val purchaseDate: LocalDate? = null,
    val sellingPrice: Double? = null,
    val sellingOdometer: Double? = null,
    val sellingDate: LocalDate? = null,
    val notes: String = "",
    val profilePhotoUri: String? = null,
    val distanceUnitOverride: DistanceUnit? = null,
    val volumeUnitOverride: VolumeUnit? = null,
    val fuelEfficiencyUnitOverride: FuelEfficiencyUnit? = null,
)

data class VehiclePart(
    val id: Long = 0L,
    val legacySourceId: Long? = null,
    val vehicleId: Long,
    val name: String,
    val partNumber: String = "",
    val type: String = "",
    val brand: String = "",
    val color: String = "",
    val size: String = "",
    val volume: Double? = null,
    val pressure: Double? = null,
    val quantity: Int? = null,
    val notes: String = "",
)

data class FuelType(
    val id: Long = 0L,
    val legacySourceId: Long? = null,
    val category: String,
    val grade: String,
    val octane: Int? = null,
    val cetane: Int? = null,
    val notes: String = "",
) {
    val categoryDisplayName: String
        get() = category.toFuelCategoryDisplayName()

    val hasStructuredChoiceData: Boolean
        get() = grade.isNotBlank() || (octane ?: 0) > 0 || (cetane ?: 0) > 0

    val displayName: String
        get() = buildString {
            val normalizedGrade = grade.trim()
            if (normalizedGrade.isNotBlank()) {
                append(normalizedGrade)
            } else {
                append(categoryDisplayName)
            }
            when {
                octane != null && octane > 0 -> append(" [$octane]")
                cetane != null && cetane > 0 -> append(" [$cetane]")
            }
        }
}

fun String.toFuelCategoryDisplayName(): String = when (trim().lowercase()) {
    "gasoline" -> "Gasoline"
    "diesel" -> "Diesel"
    "biodiesel" -> "Biodiesel"
    "bioalcohol" -> "Bioalcohol"
    "gas" -> "Gas"
    "other" -> "Other"
    else -> trim().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
}

data class ServiceType(
    val id: Long = 0L,
    val legacySourceId: Long? = null,
    val name: String,
    val notes: String = "",
    val defaultTimeReminderMonths: Int = 0,
    val defaultDistanceReminder: Double? = null,
)

data class ExpenseType(
    val id: Long = 0L,
    val legacySourceId: Long? = null,
    val name: String,
    val notes: String = "",
)

data class TripType(
    val id: Long = 0L,
    val legacySourceId: Long? = null,
    val name: String,
    val defaultTaxDeductionRate: Double? = null,
    val notes: String = "",
)

data class ServiceReminder(
    val id: Long = 0L,
    val legacySourceId: Long? = null,
    val vehicleId: Long,
    val serviceTypeId: Long,
    val intervalTimeMonths: Int = 0,
    val intervalDistance: Double? = null,
    val dueDate: LocalDate? = null,
    val dueDistance: Double? = null,
    val timeAlertSilent: Boolean = false,
    val distanceAlertSilent: Boolean = false,
    val lastTimeAlert: LocalDateTime? = null,
    val lastDistanceAlert: LocalDateTime? = null,
)

data class FillUpRecord(
    val id: Long = 0L,
    val legacySourceId: Long? = null,
    val vehicleId: Long,
    val dateTime: LocalDateTime,
    val odometerReading: Double,
    val distanceUnit: DistanceUnit,
    val volume: Double,
    val volumeUnit: VolumeUnit,
    val pricePerUnit: Double,
    val totalCost: Double,
    val paymentType: String = "",
    val partial: Boolean = false,
    val previousMissedFillups: Boolean = false,
    val fuelEfficiency: Double? = null,
    val fuelEfficiencyUnit: FuelEfficiencyUnit? = null,
    val importedFuelEfficiency: Double? = null,
    val fuelTypeId: Long? = null,
    val importedFuelTypeText: String? = null,
    val hasFuelAdditive: Boolean = false,
    val fuelAdditiveName: String = "",
    val fuelBrand: String = "",
    val stationAddress: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val drivingMode: String = "",
    val cityDrivingPercentage: Int? = null,
    val highwayDrivingPercentage: Int? = null,
    val averageSpeed: Double? = null,
    val tags: List<String> = emptyList(),
    val notes: String = "",
    val distanceSincePrevious: Double? = null,
    val distanceTillNextFillUp: Double? = null,
    val timeSincePreviousMillis: Long? = null,
    val timeTillNextFillUpMillis: Long? = null,
    val distanceForFuelEfficiency: Double? = null,
    val volumeForFuelEfficiency: Double? = null,
)

data class ServiceRecord(
    val id: Long = 0L,
    val legacySourceId: Long? = null,
    val vehicleId: Long,
    val dateTime: LocalDateTime,
    val odometerReading: Double,
    val distanceUnit: DistanceUnit,
    val totalCost: Double,
    val paymentType: String = "",
    val serviceCenterName: String = "",
    val serviceCenterAddress: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val tags: List<String> = emptyList(),
    val notes: String = "",
    val serviceTypeIds: List<Long> = emptyList(),
)

data class ExpenseRecord(
    val id: Long = 0L,
    val legacySourceId: Long? = null,
    val vehicleId: Long,
    val dateTime: LocalDateTime,
    val odometerReading: Double,
    val distanceUnit: DistanceUnit,
    val totalCost: Double,
    val paymentType: String = "",
    val expenseCenterName: String = "",
    val expenseCenterAddress: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val tags: List<String> = emptyList(),
    val notes: String = "",
    val expenseTypeIds: List<Long> = emptyList(),
)

data class TripRecord(
    val id: Long = 0L,
    val legacySourceId: Long? = null,
    val vehicleId: Long,
    val startDateTime: LocalDateTime,
    val startOdometerReading: Double,
    val startLocation: String = "",
    val endDateTime: LocalDateTime? = null,
    val endOdometerReading: Double? = null,
    val endLocation: String = "",
    val startLatitude: Double? = null,
    val startLongitude: Double? = null,
    val endLatitude: Double? = null,
    val endLongitude: Double? = null,
    val distanceUnit: DistanceUnit,
    val distance: Double? = null,
    val durationMillis: Long? = null,
    val tripTypeId: Long? = null,
    val purpose: String = "",
    val client: String = "",
    val taxDeductionRate: Double? = null,
    val taxDeductionAmount: Double? = null,
    val reimbursementRate: Double? = null,
    val reimbursementAmount: Double? = null,
    val paid: Boolean = false,
    val tags: List<String> = emptyList(),
    val notes: String = "",
)

data class RecordAttachment(
    val id: Long = 0L,
    val vehicleId: Long,
    val recordFamily: RecordFamily,
    val recordId: Long,
    val uri: String,
    val mimeType: String = "",
    val displayName: String = "",
    val createdAt: LocalDateTime,
)

data class VehicleDashboardSummary(
    val vehicle: Vehicle,
    val lastKnownOdometer: Double? = null,
    val fillUpCount: Int = 0,
    val serviceCount: Int = 0,
    val averageFuelEfficiency: Double? = null,
    val lastFuelPrice: Double? = null,
    val totalFuelCost: Double = 0.0,
)

data class VehicleDetailBundle(
    val vehicle: Vehicle,
    val parts: List<VehiclePart>,
    val reminders: List<ServiceReminder>,
    val upcomingReminders: List<ReminderDisplayItem> = emptyList(),
    val recentFillUps: List<FillUpRecord>,
    val recentServices: List<ServiceRecord>,
    val recentExpenses: List<ExpenseRecord>,
    val recentTrips: List<TripRecord>,
    val stats: VehicleStatistics,
)

data class VehicleStatistics(
    val vehicleId: Long,
    val fillUpCount: Int = 0,
    val totalFuelVolume: Double = 0.0,
    val totalFuelCost: Double = 0.0,
    val totalDistance: Double = 0.0,
    val averageFuelEfficiency: Double? = null,
    val lastFuelEfficiency: Double? = null,
    val averagePricePerUnit: Double? = null,
    val serviceCostTotal: Double = 0.0,
    val expenseCostTotal: Double = 0.0,
    val tripDistanceTotal: Double = 0.0,
)

data class BrowseRecordItem(
    val recordId: Long,
    val vehicleId: Long,
    val vehicleName: String,
    val family: RecordFamily,
    val occurredAt: LocalDateTime,
    val title: String,
    val subtitle: String = "",
    val amount: Double? = null,
    val odometerReading: Double? = null,
    val tags: List<String> = emptyList(),
    val subtypeNames: List<String> = emptyList(),
    val paymentType: String = "",
    val eventPlaceName: String = "",
    val fuelBrand: String = "",
    val fuelTypeLabel: String = "",
    val fuelAdditiveName: String = "",
    val drivingMode: String = "",
    val tripPurpose: String = "",
    val tripClient: String = "",
    val tripLocations: List<String> = emptyList(),
    val tripPaidStatus: BrowseTripPaidStatus? = null,
    val notes: String = "",
    val searchText: String = "",
    val tripOpen: Boolean = false,
    val vehicleLifecycle: VehicleLifecycle = VehicleLifecycle.ACTIVE,
)

@Serializable
enum class BrowseTripPaidStatus {
    PAID,
    UNPAID,
}

@Serializable
data class SavedBrowseSearch(
    val name: String,
    val vehicleId: Long? = null,
    val family: RecordFamily? = null,
    val query: String = "",
    val tag: String = "",
    val fromDateIso: String? = null,
    val toDateIso: String? = null,
    val subtype: String = "",
    val paymentType: String = "",
    val eventPlace: String = "",
    val fuelBrand: String = "",
    val fuelType: String = "",
    val fuelAdditive: String = "",
    val drivingMode: String = "",
    val tripPurpose: String = "",
    val tripClient: String = "",
    val tripLocation: String = "",
    val tripPaidStatus: BrowseTripPaidStatus? = null,
)

data class BrowseRecordFilter(
    val vehicleId: Long? = null,
    val family: RecordFamily? = null,
    val query: String = "",
    val tag: String = "",
    val fromDate: LocalDate? = null,
    val toDate: LocalDate? = null,
    val subtype: String = "",
    val paymentType: String = "",
    val eventPlace: String = "",
    val fuelBrand: String = "",
    val fuelType: String = "",
    val fuelAdditive: String = "",
    val drivingMode: String = "",
    val tripPurpose: String = "",
    val tripClient: String = "",
    val tripLocation: String = "",
    val tripPaidStatus: BrowseTripPaidStatus? = null,
)
