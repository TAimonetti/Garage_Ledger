package com.garageledger.data.export

import com.garageledger.domain.model.StatisticsDashboard
import java.io.StringWriter
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVPrinter
import org.apache.commons.csv.QuoteMode

class StatisticsCsvExporter {
    fun export(dashboard: StatisticsDashboard): String {
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
            title = "Statistics Metadata",
            headers = listOf("Scope", "Timeframe", "Distance Unit", "Volume Unit", "Fuel Efficiency Unit", "Currency"),
            rows = listOf(
                listOf(
                    dashboard.scopeLabel,
                    dashboard.periodDescription,
                    dashboard.preferences.distanceUnit.storageValue,
                    dashboard.preferences.volumeUnit.storageValue,
                    dashboard.preferences.fuelEfficiencyUnit.storageValue,
                    dashboard.preferences.currencySymbol,
                ),
            ),
        )

        section(
            title = "Overall Summary",
            headers = listOf("Metric", "Value"),
            rows = listOf(
                listOf("Vehicles", dashboard.overall.vehicleCount.toString()),
                listOf("Total Records", dashboard.overall.totalRecordCount.toString()),
                listOf("Fill-Ups", dashboard.overall.fillUpCount.toString()),
                listOf("Services", dashboard.overall.serviceCount.toString()),
                listOf("Expenses", dashboard.overall.expenseCount.toString()),
                listOf("Trips", dashboard.overall.tripCount.toString()),
                listOf("Total Operating Cost", dashboard.overall.totalOperatingCost.toPlain()),
            ),
        )

        section(
            title = "Fill-Up Summary",
            headers = listOf("Metric", "Value"),
            rows = listOf(
                listOf("Count", dashboard.fillUps.count.toString()),
                listOf("Total Volume", dashboard.fillUps.totalVolume.toPlain()),
                listOf("Total Cost", dashboard.fillUps.totalCost.toPlain()),
                listOf("Total Distance", dashboard.fillUps.totalDistance.toPlain()),
                listOf("Average Fuel Efficiency", dashboard.fillUps.averageFuelEfficiency.toPlain()),
                listOf("Last Fuel Efficiency", dashboard.fillUps.lastFuelEfficiency.toPlain()),
                listOf("Average Price per Unit", dashboard.fillUps.averagePricePerUnit.toPlain()),
                listOf("Average Cost per Fill-Up", dashboard.fillUps.averageCostPerFillUp.toPlain()),
            ),
        )

        section(
            title = "Service Summary",
            headers = listOf("Metric", "Value"),
            rows = listOf(
                listOf("Count", dashboard.services.count.toString()),
                listOf("Total Cost", dashboard.services.totalCost.toPlain()),
                listOf("Average Cost", dashboard.services.averageCost.toPlain()),
            ),
        )

        section(
            title = "Expense Summary",
            headers = listOf("Metric", "Value"),
            rows = listOf(
                listOf("Count", dashboard.expenses.count.toString()),
                listOf("Total Cost", dashboard.expenses.totalCost.toPlain()),
                listOf("Average Cost", dashboard.expenses.averageCost.toPlain()),
            ),
        )

        section(
            title = "Trip Summary",
            headers = listOf("Metric", "Value"),
            rows = listOf(
                listOf("Count", dashboard.trips.count.toString()),
                listOf("Total Distance", dashboard.trips.totalDistance.toPlain()),
                listOf("Average Distance", dashboard.trips.averageDistance.toPlain()),
                listOf("Total Tax Deduction", dashboard.trips.totalTaxDeduction.toPlain()),
                listOf("Total Reimbursement", dashboard.trips.totalReimbursement.toPlain()),
                listOf("Paid Trips", dashboard.trips.paidCount.toString()),
                listOf("Open Trips", dashboard.trips.openCount.toString()),
            ),
        )

        dashboard.charts.forEach { chart ->
            section(
                title = "Chart - ${chart.title}",
                headers = listOf("Recorded At", "Vehicle", "Label", "Value", "Unit"),
                rows = chart.points.map { point ->
                    listOf(
                        point.recordedAt.toString(),
                        point.vehicleName,
                        point.label,
                        point.value.toPlain(),
                        chart.unitLabel,
                    )
                },
            )
        }

        return writer.toString()
    }

    private fun Double?.toPlain(): String = when (this) {
        null -> ""
        else -> if (this % 1.0 == 0.0) this.toInt().toString() else "%,.4f".format(this).replace(",", "")
    }

    private companion object {
        val CsvFormat: CSVFormat = CSVFormat.DEFAULT.builder()
            .setQuoteMode(QuoteMode.MINIMAL)
            .build()
    }
}
