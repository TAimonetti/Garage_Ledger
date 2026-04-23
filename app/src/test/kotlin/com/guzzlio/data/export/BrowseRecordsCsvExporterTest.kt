package com.guzzlio.data.export

import com.guzzlio.domain.model.BrowseRecordItem
import com.guzzlio.domain.model.BrowseTripPaidStatus
import com.guzzlio.domain.model.RecordFamily
import com.google.common.truth.Truth.assertThat
import java.time.LocalDateTime
import org.junit.Test

class BrowseRecordsCsvExporterTest {
    @Test
    fun export_includesMetadataAndRecordRows() {
        val csv = BrowseRecordsCsvExporter().export(
            records = listOf(
                BrowseRecordItem(
                    recordId = 4L,
                    vehicleId = 1L,
                    vehicleName = "Corolla",
                    family = RecordFamily.TRIP,
                    occurredAt = LocalDateTime.parse("2026-04-01T08:15:00"),
                    title = "Business",
                    subtitle = "Phoenix -> Tempe",
                    amount = 23.5,
                    odometerReading = 120045.0,
                    paymentType = "Visa",
                    eventPlaceName = "Client Office",
                    tripPurpose = "Client Visit",
                    tripClient = "ACME",
                    tripLocations = listOf("Phoenix", "Tempe"),
                    tripPaidStatus = BrowseTripPaidStatus.UNPAID,
                    tripOpen = true,
                    tags = listOf("client", "tax"),
                    notes = "Bring paperwork",
                    searchText = "",
                ),
            ),
            sortDescending = true,
            exportedAt = LocalDateTime.parse("2026-04-20T12:00:00"),
        )

        assertThat(csv).contains("Browse Records Metadata")
        assertThat(csv).contains("Descending")
        assertThat(csv).contains("Browse Records")
        assertThat(csv).contains("Date/Time,Vehicle,Family,Title")
        assertThat(csv).contains("Corolla,TRIP,Business")
        assertThat(csv).contains("Phoenix | Tempe")
        assertThat(csv).contains("UNPAID")
        assertThat(csv).contains("Yes")
        assertThat(csv).contains("Bring paperwork")
    }
}
