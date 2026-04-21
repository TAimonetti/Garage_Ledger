package com.garageledger.ui

import com.garageledger.domain.model.BrowseRecordFilter
import com.garageledger.domain.model.BrowseRecordItem
import com.garageledger.domain.model.BrowseTripPaidStatus
import com.garageledger.domain.model.RecordFamily
import com.garageledger.domain.model.VehicleLifecycle
import com.google.common.truth.Truth.assertThat
import java.time.LocalDate
import java.time.LocalDateTime
import org.junit.Test

class BrowseSupportTest {
    @Test
    fun applyBrowseRecordFilter_matchesStructuredTripAndSubtypeFilters() {
        val trip = BrowseRecordItem(
            recordId = 1L,
            vehicleId = 10L,
            vehicleName = "Corolla",
            family = RecordFamily.TRIP,
            occurredAt = LocalDateTime.parse("2024-03-01T08:00:00"),
            title = "Business",
            subtypeNames = listOf("Business"),
            tripPurpose = "Client Visit",
            tripClient = "ACME",
            tripLocations = listOf("Phoenix", "Tempe"),
            tripPaidStatus = BrowseTripPaidStatus.UNPAID,
            searchText = "corolla client visit acme phoenix tempe business",
        )
        val service = BrowseRecordItem(
            recordId = 2L,
            vehicleId = 10L,
            vehicleName = "Corolla",
            family = RecordFamily.SERVICE,
            occurredAt = LocalDateTime.parse("2024-03-02T09:00:00"),
            title = "Oil Change",
            subtypeNames = listOf("Oil Change"),
            paymentType = "Visa",
            eventPlaceName = "Main Street Garage",
            searchText = "corolla oil change visa main street garage",
        )

        val filtered = applyBrowseRecordFilter(
            records = listOf(trip, service),
            filter = BrowseRecordFilter(
                family = RecordFamily.TRIP,
                subtype = "business",
                tripClient = "acme",
                tripLocation = "tempe",
                tripPaidStatus = BrowseTripPaidStatus.UNPAID,
            ),
        )

        assertThat(filtered).containsExactly(trip)
    }

    @Test
    fun applyBrowseRecordFilter_respectsDateAndFuelFacets() {
        val fillUp = BrowseRecordItem(
            recordId = 3L,
            vehicleId = 11L,
            vehicleName = "Tundra",
            family = RecordFamily.FILL_UP,
            occurredAt = LocalDateTime.parse("2024-04-10T10:00:00"),
            title = "Fuel-Up",
            paymentType = "Cash",
            eventPlaceName = "Shell Station",
            fuelBrand = "Shell",
            fuelTypeLabel = "Gasoline - Regular",
            fuelAdditiveName = "Injector Cleaner",
            drivingMode = "Highway",
            searchText = "tundra shell gasoline regular injector cleaner highway",
        )

        val filtered = applyBrowseRecordFilter(
            records = listOf(fillUp),
            filter = BrowseRecordFilter(
                vehicleId = 11L,
                fromDate = LocalDate.parse("2024-04-01"),
                toDate = LocalDate.parse("2024-04-30"),
                paymentType = "cash",
                eventPlace = "shell",
                fuelBrand = "shell",
                fuelType = "regular",
                fuelAdditive = "injector",
                drivingMode = "highway",
            ),
        )

        assertThat(filtered).containsExactly(fillUp)
    }

    @Test
    fun buildBrowseFilterOptions_collectsDistinctSuggestions() {
        val records = listOf(
            BrowseRecordItem(
                recordId = 1L,
                vehicleId = 1L,
                vehicleName = "Corolla",
                family = RecordFamily.TRIP,
                occurredAt = LocalDateTime.parse("2024-01-01T10:00:00"),
                title = "Trip",
                tags = listOf("tax", "client"),
                subtypeNames = listOf("Business"),
                tripPurpose = "Client Visit",
                tripClient = "ACME",
                tripLocations = listOf("Phoenix", "Tempe"),
                tripPaidStatus = BrowseTripPaidStatus.PAID,
                searchText = "",
            ),
            BrowseRecordItem(
                recordId = 2L,
                vehicleId = 1L,
                vehicleName = "Corolla",
                family = RecordFamily.TRIP,
                occurredAt = LocalDateTime.parse("2024-01-02T10:00:00"),
                title = "Trip",
                tags = listOf("Tax"),
                subtypeNames = listOf("Business"),
                tripPurpose = "Client Visit",
                tripClient = "Beta",
                tripLocations = listOf("Phoenix"),
                tripPaidStatus = BrowseTripPaidStatus.UNPAID,
                searchText = "",
            ),
        )

        val options = buildBrowseFilterOptions(records, RecordFamily.TRIP)

        assertThat(options.tagSuggestions).containsExactly("client", "tax").inOrder()
        assertThat(options.subtypeSuggestions).containsExactly("Business")
        assertThat(options.tripPurposeSuggestions).containsExactly("Client Visit")
        assertThat(options.tripClientSuggestions).containsExactly("ACME", "Beta").inOrder()
        assertThat(options.tripLocationSuggestions).containsExactly("Phoenix", "Tempe").inOrder()
    }

    @Test
    fun buildBrowseActionItems_matchesTripStateAndRetiredVehicles() {
        val openTrip = BrowseRecordItem(
            recordId = 9L,
            vehicleId = 3L,
            vehicleName = "Altima",
            family = RecordFamily.TRIP,
            occurredAt = LocalDateTime.parse("2024-05-01T08:00:00"),
            title = "Trip",
            tripOpen = true,
            searchText = "",
        )
        val retiredTrip = openTrip.copy(
            recordId = 10L,
            tripOpen = false,
            vehicleLifecycle = VehicleLifecycle.RETIRED,
        )

        val openActions = buildBrowseActionItems(openTrip)
        val retiredActions = buildBrowseActionItems(retiredTrip)

        assertThat(openActions.map { it.action }).containsExactly(
            BrowseRecordAction.VIEW,
            BrowseRecordAction.EDIT,
            BrowseRecordAction.FINISH_TRIP,
            BrowseRecordAction.DELETE,
        ).inOrder()
        assertThat(openActions.first { it.action == BrowseRecordAction.FINISH_TRIP }.enabled).isTrue()
        assertThat(retiredActions.first { it.action == BrowseRecordAction.COPY_TRIP }.enabled).isFalse()
        assertThat(retiredActions.first { it.action == BrowseRecordAction.EDIT }.enabled).isFalse()
        assertThat(retiredActions.first { it.action == BrowseRecordAction.DELETE }.enabled).isFalse()
    }

    @Test
    fun sortBrowseRecords_ordersByOccurredAtAccordingToPreference() {
        val older = BrowseRecordItem(
            recordId = 1L,
            vehicleId = 2L,
            vehicleName = "Corolla",
            family = RecordFamily.FILL_UP,
            occurredAt = LocalDateTime.parse("2024-01-01T08:00:00"),
            title = "Older",
            searchText = "",
        )
        val newer = older.copy(
            recordId = 2L,
            occurredAt = LocalDateTime.parse("2024-02-01T08:00:00"),
            title = "Newer",
        )

        assertThat(sortBrowseRecords(listOf(older, newer), descending = true)).containsExactly(newer, older).inOrder()
        assertThat(sortBrowseRecords(listOf(older, newer), descending = false)).containsExactly(older, newer).inOrder()
        assertThat(browseSortLabel(descending = true)).isEqualTo("Newest First")
        assertThat(browseSortLabel(descending = false)).isEqualTo("Oldest First")
    }
}
