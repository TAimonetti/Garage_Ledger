package com.garageledger.ui

import com.garageledger.domain.model.OptionalFieldToggle
import com.garageledger.domain.model.RecordFamily
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class RecordUiTest {
    @Test
    fun routeSegments_roundTripToRecordFamily() {
        RecordFamily.entries.forEach { family ->
            assertThat(recordFamilyFromRouteSegment(family.routeSegment())).isEqualTo(family)
        }
        assertThat(recordFamilyFromRouteSegment("unknown")).isNull()
    }

    @Test
    fun visibleFieldOptionGroups_includeExpectedToggles() {
        assertThat(FuelUpVisibleFieldOptions.map { it.toggle }).containsAtLeast(
            OptionalFieldToggle.PAYMENT_TYPE,
            OptionalFieldToggle.FUEL_TYPE,
            OptionalFieldToggle.FUEL_ADDITIVE,
            OptionalFieldToggle.FUELING_STATION,
            OptionalFieldToggle.TAGS,
            OptionalFieldToggle.NOTES,
        )
        assertThat(ServiceVisibleFieldOptions.map { it.toggle }).containsExactly(
            OptionalFieldToggle.PAYMENT_TYPE,
            OptionalFieldToggle.SERVICE_CENTER,
            OptionalFieldToggle.TAGS,
            OptionalFieldToggle.NOTES,
        )
        assertThat(ExpenseVisibleFieldOptions.map { it.toggle }).containsExactly(
            OptionalFieldToggle.PAYMENT_TYPE,
            OptionalFieldToggle.EXPENSE_CENTER,
            OptionalFieldToggle.TAGS,
            OptionalFieldToggle.NOTES,
        )
        assertThat(TripVisibleFieldOptions.map { it.toggle }).containsAtLeast(
            OptionalFieldToggle.TRIP_LOCATION,
            OptionalFieldToggle.TRIP_PURPOSE,
            OptionalFieldToggle.TRIP_CLIENT,
            OptionalFieldToggle.TRIP_TAX_DEDUCTION,
            OptionalFieldToggle.TRIP_REIMBURSEMENT,
        )
    }
}
