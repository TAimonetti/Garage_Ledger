package com.garageledger.data.export

import com.garageledger.domain.model.AppPreferenceSnapshot
import com.garageledger.domain.model.ExpenseRecord
import com.garageledger.domain.model.ExpenseType
import com.garageledger.domain.model.FillUpRecord
import com.garageledger.domain.model.ServiceRecord
import com.garageledger.domain.model.ServiceType
import com.garageledger.domain.model.TripRecord
import com.garageledger.domain.model.TripType
import com.garageledger.domain.model.Vehicle
import java.io.StringWriter
import java.time.format.DateTimeFormatter
import java.util.Locale
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVPrinter
import org.apache.commons.csv.QuoteMode

class SectionedCsvExporter {
    fun export(
        preferences: AppPreferenceSnapshot,
        vehicles: List<Vehicle>,
        fillUps: List<FillUpRecord>,
        services: List<ServiceRecord>,
        expenses: List<ExpenseRecord>,
        trips: List<TripRecord>,
        serviceTypes: List<ServiceType> = emptyList(),
        expenseTypes: List<ExpenseType> = emptyList(),
        tripTypes: List<TripType> = emptyList(),
    ): String {
        val vehicleNames = vehicles.associateBy(Vehicle::id, Vehicle::name)
        val serviceNames = serviceTypes.associateBy(ServiceType::id, ServiceType::name)
        val expenseNames = expenseTypes.associateBy(ExpenseType::id, ExpenseType::name)
        val tripNames = tripTypes.associateBy(TripType::id, TripType::name)
        val writer = StringWriter()

        fun section(title: String, headers: List<String>, rows: List<List<String>>) {
            writer.appendLine(title)
            CSVPrinter(writer, CsvFormat).use { printer ->
                printer.printRecord(headers)
                rows.forEach(printer::printRecord)
            }
            writer.appendLine()
        }

        section(
            title = "Metadata",
            headers = listOf(
                "Android Version",
                "aCar Version",
                "aCar Build Date",
                "aCar Database Version",
                "Export Date/Time",
                "Export Version",
                "Minimum Supported aCar Version",
                "Date Format",
                "Time Format",
            ),
            rows = listOf(
                listOf(
                    "Garage Ledger",
                    "0.1.0",
                    "",
                    "1",
                    "",
                    "1",
                    "1",
                    "MM/dd/yyyy",
                    "hh:mm a",
                ),
            ),
        )

        section(
            title = "Vehicles",
            headers = listOf(
                "Name", "Make", "Model", "Active", "Year", "License Plate", "VIN", "Insurance Policy",
                "Body Style", "Color", "Engine Displacement", "Fuel Tank Capacity", "Purchase Price",
                "Purchase Odometer Reading", "Purchase Date", "Selling Price", "Selling Odometer Reading",
                "Selling Date", "Notes",
            ),
            rows = vehicles.map { vehicle ->
                listOf(
                    vehicle.name,
                    vehicle.make,
                    vehicle.model,
                    if (vehicle.lifecycle.name == "ACTIVE") "Yes" else "No",
                    vehicle.year?.toString().orEmpty(),
                    vehicle.licensePlate,
                    vehicle.vin,
                    vehicle.insurancePolicy,
                    vehicle.bodyStyle,
                    vehicle.color,
                    vehicle.engineDisplacement,
                    vehicle.fuelTankCapacity.formatNumber(),
                    vehicle.purchasePrice.formatPlain(),
                    vehicle.purchaseOdometer.formatNumber(),
                    vehicle.purchaseDate?.format(DateTimeFormatter.ofPattern("MM/dd/yyyy")).orEmpty(),
                    vehicle.sellingPrice.formatPlain(),
                    vehicle.sellingOdometer.formatNumber(),
                    vehicle.sellingDate?.format(DateTimeFormatter.ofPattern("MM/dd/yyyy")).orEmpty(),
                    vehicle.notes,
                )
            },
        )

        section(
            title = "Fill-Up Records",
            headers = listOf(
                "Vehicle", "Date", "Time", "Odometer Reading", "Distance Unit", "Volume", "Volume Unit",
                "Price per Unit", "Total Cost", "Payment", "Partial Fill-Up?", "Previously Missed Fill-Ups?",
                "Fuel Efficiency", "Fuel Efficiency Unit", "Fuel Type", "Has Fuel Additive?", "Fuel Additive Name",
                "Fuel Brand", "Fueling Station Address", "Latitude", "Longitude", "Driving Mode",
                "City Driving Percentage", "Highway Driving Percentage", "Average Speed", "Tags", "Notes",
            ),
            rows = fillUps.sortedByDescending { it.dateTime }.map { record ->
                listOf(
                    vehicleNames[record.vehicleId].orEmpty(),
                    record.dateTime.format(DateFormatter),
                    record.dateTime.format(TimeFormatter),
                    record.odometerReading.formatNumber(),
                    record.distanceUnit.storageValue,
                    record.volume.formatPlain(),
                    record.volumeUnit.storageValue,
                    record.pricePerUnit.asCurrency(preferences.currencySymbol),
                    record.totalCost.asCurrency(preferences.currencySymbol),
                    record.paymentType,
                    if (record.partial) "Yes" else "No",
                    if (record.previousMissedFillups) "Yes" else "No",
                    (record.fuelEfficiency ?: record.importedFuelEfficiency).formatPlain(),
                    record.fuelEfficiencyUnit?.storageValue.orEmpty(),
                    record.importedFuelTypeText.orEmpty(),
                    if (record.hasFuelAdditive) "Yes" else "No",
                    record.fuelAdditiveName,
                    record.fuelBrand,
                    record.stationAddress,
                    record.latitude.formatPlain(),
                    record.longitude.formatPlain(),
                    record.drivingMode.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.US) else it.toString() },
                    record.cityDrivingPercentage?.toString().orEmpty(),
                    record.highwayDrivingPercentage?.toString().orEmpty(),
                    record.averageSpeed.formatPlain(),
                    record.tags.joinToString(", "),
                    record.notes,
                )
            },
        )

        section(
            title = "Service Records",
            headers = listOf(
                "Vehicle", "Date", "Time", "Odometer Reading", "Distance Unit", "Services", "Total Cost",
                "Payment", "Service Center Name", "Service Center Address", "Latitude", "Longitude", "Tags", "Notes",
            ),
            rows = services.sortedByDescending { it.dateTime }.map { record ->
                listOf(
                    vehicleNames[record.vehicleId].orEmpty(),
                    record.dateTime.format(DateFormatter),
                    record.dateTime.format(TimeFormatter),
                    record.odometerReading.formatNumber(),
                    record.distanceUnit.storageValue,
                    record.serviceTypeIds.mapNotNull(serviceNames::get).joinToString(", "),
                    record.totalCost.asCurrency(preferences.currencySymbol),
                    record.paymentType,
                    record.serviceCenterName,
                    record.serviceCenterAddress,
                    record.latitude.formatPlain(),
                    record.longitude.formatPlain(),
                    record.tags.joinToString(", "),
                    record.notes,
                )
            },
        )

        section(
            title = "Expense Records",
            headers = listOf(
                "Vehicle", "Date", "Time", "Odometer Reading", "Distance Unit", "Expenses", "Total Cost",
                "Payment", "Expense Center Name", "Expense Center Address", "Latitude", "Longitude", "Tags", "Notes",
            ),
            rows = expenses.sortedByDescending { it.dateTime }.map { record ->
                listOf(
                    vehicleNames[record.vehicleId].orEmpty(),
                    record.dateTime.format(DateFormatter),
                    record.dateTime.format(TimeFormatter),
                    record.odometerReading.formatNumber(),
                    record.distanceUnit.storageValue,
                    record.expenseTypeIds.mapNotNull(expenseNames::get).joinToString(", "),
                    record.totalCost.asCurrency(preferences.currencySymbol),
                    record.paymentType,
                    record.expenseCenterName,
                    record.expenseCenterAddress,
                    record.latitude.formatPlain(),
                    record.longitude.formatPlain(),
                    record.tags.joinToString(", "),
                    record.notes,
                )
            },
        )

        section(
            title = "Trip Records",
            headers = listOf(
                "Vehicle", "Start Date", "Start Time", "Start Odometer Reading", "Start Location", "End Date",
                "End Time", "End Odometer Reading", "End Location", "Start Latitude", "Start Longitude",
                "End Latitude", "End Longitude", "Distance Unit", "Distance", "Duration", "Type", "Purpose",
                "Client", "Tax Deduction Rate", "Tax Deduction Amount", "Reimbursement Rate", "Reimbursement Amount",
                "Paid", "Tags", "Notes",
            ),
            rows = trips.sortedByDescending { it.startDateTime }.map { record ->
                listOf(
                    vehicleNames[record.vehicleId].orEmpty(),
                    record.startDateTime.format(DateFormatter),
                    record.startDateTime.format(TimeFormatter),
                    record.startOdometerReading.formatNumber(),
                    record.startLocation,
                    record.endDateTime?.format(DateFormatter).orEmpty(),
                    record.endDateTime?.format(TimeFormatter).orEmpty(),
                    record.endOdometerReading.formatNumber(),
                    record.endLocation,
                    record.startLatitude.formatPlain(),
                    record.startLongitude.formatPlain(),
                    record.endLatitude.formatPlain(),
                    record.endLongitude.formatPlain(),
                    record.distanceUnit.storageValue,
                    record.distance.formatPlain(),
                    record.durationMillis?.toString().orEmpty(),
                    record.tripTypeId?.let(tripNames::get).orEmpty(),
                    record.purpose,
                    record.client,
                    record.taxDeductionRate.formatPlain(),
                    record.taxDeductionAmount.formatPlain(),
                    record.reimbursementRate.formatPlain(),
                    record.reimbursementAmount.formatPlain(),
                    if (record.paid) "Yes" else "No",
                    record.tags.joinToString(", "),
                    record.notes,
                )
            },
        )

        return writer.toString()
    }

    private companion object {
        val CsvFormat: CSVFormat = CSVFormat.DEFAULT.builder()
            .setQuoteMode(QuoteMode.ALL)
            .build()
        val DateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("MM/dd/yyyy")
        val TimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("hh:mm a")
    }
}

private fun Double?.formatPlain(): String = this?.let { "%,.3f".format(it).replace(",", "") }.orEmpty().trimEnd('0').trimEnd('.')

private fun Double?.formatNumber(): String = this?.let { "%,.0f".format(it) }.orEmpty()

private fun Double?.asCurrency(symbol: String): String = this?.let { "$symbol${"%,.2f".format(it)}" }.orEmpty()
