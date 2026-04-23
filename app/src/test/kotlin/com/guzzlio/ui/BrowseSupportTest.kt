package com.guzzlio.ui

import com.guzzlio.domain.model.BrowseRecordFilter
import com.guzzlio.domain.model.BrowseRecordItem
import com.guzzlio.domain.model.BrowseTripPaidStatus
import com.guzzlio.domain.model.RecordFamily
import com.guzzlio.domain.model.SavedBrowseSearch
import com.guzzlio.domain.model.VehicleLifecycle
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

    @Test
    fun buildBrowseFilterTokens_summarizesStructuredQuery() {
        val tokens = buildBrowseFilterTokens(
            filter = BrowseRecordFilter(
                family = RecordFamily.FILL_UP,
                query = "shell",
                tag = "roadtrip",
                fromDate = LocalDate.parse("2024-01-01"),
                toDate = LocalDate.parse("2024-01-31"),
                fuelBrand = "Shell",
                paymentType = "Cash",
            ),
            vehicleName = "Tundra",
        )

        assertThat(tokens.map { it.label }).containsExactly(
            "Vehicle: Tundra",
            "Type: Fuel-Ups",
            "Search: shell",
            "Tag: roadtrip",
            "From: 2024-01-01",
            "To: 2024-01-31",
            "Payment: Cash",
            "Fuel Brand: Shell",
        ).inOrder()
    }

    @Test
    fun browseDatePresetRange_and_resolution_matchExpectedRanges() {
        val today = LocalDate.parse("2024-04-20")

        val lastThirty = browseDatePresetRange(BrowseDatePreset.LAST_30_DAYS, today)
        val lastNinety = browseDatePresetRange(BrowseDatePreset.LAST_90_DAYS, today)
        val thisYear = browseDatePresetRange(BrowseDatePreset.THIS_YEAR, today)

        assertThat(lastThirty.first).isEqualTo(LocalDate.parse("2024-03-22"))
        assertThat(lastThirty.second).isEqualTo(today)
        assertThat(lastNinety.first).isEqualTo(LocalDate.parse("2024-01-22"))
        assertThat(lastNinety.second).isEqualTo(today)
        assertThat(thisYear.first).isEqualTo(LocalDate.parse("2024-01-01"))
        assertThat(thisYear.second).isEqualTo(today)
        assertThat(resolveBrowseDatePreset(lastThirty.first, lastThirty.second, today)).isEqualTo(BrowseDatePreset.LAST_30_DAYS)
        assertThat(resolveBrowseDatePreset(lastNinety.first, lastNinety.second, today)).isEqualTo(BrowseDatePreset.LAST_90_DAYS)
        assertThat(resolveBrowseDatePreset(thisYear.first, thisYear.second, today)).isEqualTo(BrowseDatePreset.THIS_YEAR)
        assertThat(resolveBrowseDatePreset(null, null, today)).isNull()
    }

    @Test
    fun savedBrowseSearch_roundTripsThroughFilterConversion() {
        val filter = BrowseRecordFilter(
            vehicleId = 44L,
            family = RecordFamily.TRIP,
            query = "client",
            tag = "tax",
            fromDate = LocalDate.parse("2024-02-01"),
            toDate = LocalDate.parse("2024-02-29"),
            subtype = "Business",
            paymentType = "Visa",
            eventPlace = "Office",
            fuelBrand = "Shell",
            fuelType = "Regular",
            fuelAdditive = "Cleaner",
            drivingMode = "Highway",
            tripPurpose = "Meeting",
            tripClient = "ACME",
            tripLocation = "Phoenix",
            tripPaidStatus = BrowseTripPaidStatus.UNPAID,
        )

        val saved = filter.toSavedBrowseSearch("Monthly Business")

        assertThat(saved).isEqualTo(
            SavedBrowseSearch(
                name = "Monthly Business",
                vehicleId = 44L,
                family = RecordFamily.TRIP,
                query = "client",
                tag = "tax",
                fromDateIso = "2024-02-01",
                toDateIso = "2024-02-29",
                subtype = "Business",
                paymentType = "Visa",
                eventPlace = "Office",
                fuelBrand = "Shell",
                fuelType = "Regular",
                fuelAdditive = "Cleaner",
                drivingMode = "Highway",
                tripPurpose = "Meeting",
                tripClient = "ACME",
                tripLocation = "Phoenix",
                tripPaidStatus = BrowseTripPaidStatus.UNPAID,
            ),
        )
        assertThat(saved.toBrowseRecordFilter()).isEqualTo(filter)
        assertThat(findSavedBrowseSearch(listOf(saved), "monthly business")).isEqualTo(saved)
    }
}
