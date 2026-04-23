package com.guzzlio.data.export

import com.guzzlio.domain.model.BrowseRecordItem
import java.io.StringWriter
import java.time.LocalDateTime
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVPrinter
import org.apache.commons.csv.QuoteMode

class BrowseRecordsCsvExporter {
    fun export(
        records: List<BrowseRecordItem>,
        sortDescending: Boolean,
        exportedAt: LocalDateTime = LocalDateTime.now(),
    ): String {
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
            title = "Browse Records Metadata",
            headers = listOf("Exported At", "Sort Order", "Record Count"),
            rows = listOf(
                listOf(
                    exportedAt.toString(),
                    if (sortDescending) "Descending" else "Ascending",
                    records.size.toString(),
                ),
            ),
        )

        section(
            title = "Browse Records",
            headers = listOf(
                "Date/Time",
                "Vehicle",
                "Family",
                "Title",
                "Subtitle",
                "Odometer",
                "Amount",
                "Payment Type",
                "Event Place",
                "Fuel Brand",
                "Fuel Type",
                "Fuel Additive",
                "Driving Mode",
                "Trip Purpose",
                "Trip Client",
                "Trip Locations",
                "Trip Paid Status",
                "Open Trip",
                "Tags",
                "Notes",
            ),
            rows = records.map { record ->
                listOf(
                    record.occurredAt.toString(),
                    record.vehicleName,
                    record.family.name,
                    record.title,
                    record.subtitle,
                    record.odometerReading.toPlain(),
                    record.amount.toPlain(),
                    record.paymentType,
                    record.eventPlaceName,
                    record.fuelBrand,
                    record.fuelTypeLabel,
                    record.fuelAdditiveName,
                    record.drivingMode,
                    record.tripPurpose,
                    record.tripClient,
                    record.tripLocations.joinToString(" | "),
                    record.tripPaidStatus?.name.orEmpty(),
                    if (record.tripOpen) "Yes" else "No",
                    record.tags.joinToString(", "),
                    record.notes,
                )
            },
        )

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
