package com.garageledger.domain.model

data class ImportedGarageData(
    val metadata: Map<String, String> = emptyMap(),
    val preferences: AppPreferenceSnapshot? = null,
    val vehicles: List<Vehicle> = emptyList(),
    val vehicleParts: List<VehiclePart> = emptyList(),
    val fuelTypes: List<FuelType> = emptyList(),
    val serviceTypes: List<ServiceType> = emptyList(),
    val expenseTypes: List<ExpenseType> = emptyList(),
    val tripTypes: List<TripType> = emptyList(),
    val serviceReminders: List<ServiceReminder> = emptyList(),
    val fillUpRecords: List<FillUpRecord> = emptyList(),
    val serviceRecords: List<ServiceRecord> = emptyList(),
    val expenseRecords: List<ExpenseRecord> = emptyList(),
    val tripRecords: List<TripRecord> = emptyList(),
    val issues: List<ImportIssue> = emptyList(),
)

data class ImportIssue(
    val severity: Severity,
    val message: String,
    val section: String = "",
    val rowNumber: Int? = null,
) {
    enum class Severity {
        INFO,
        WARNING,
        ERROR,
    }
}

data class ImportReport(
    val sourceLabel: String,
    val vehiclesImported: Int = 0,
    val fillUpsImported: Int = 0,
    val serviceRecordsImported: Int = 0,
    val expenseRecordsImported: Int = 0,
    val tripRecordsImported: Int = 0,
    val vehiclePartsImported: Int = 0,
    val serviceTypesImported: Int = 0,
    val expenseTypesImported: Int = 0,
    val tripTypesImported: Int = 0,
    val fuelTypesImported: Int = 0,
    val skippedRows: Int = 0,
    val issues: List<ImportIssue> = emptyList(),
)
