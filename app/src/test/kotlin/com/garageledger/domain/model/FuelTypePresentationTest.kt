package com.garageledger.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FuelTypePresentationTest {
    @Test
    fun `displayName mirrors aCar style for rated fuel`() {
        val fuelType = FuelType(category = "gasoline", grade = "Regular", octane = 87)

        assertEquals("Regular [87]", fuelType.displayName)
        assertEquals("Gasoline", fuelType.categoryDisplayName)
        assertTrue(fuelType.hasStructuredChoiceData)
    }

    @Test
    fun `displayName falls back to category for malformed imported type`() {
        val fuelType = FuelType(category = "bioalcohol", grade = "", octane = null, cetane = null)

        assertEquals("Bioalcohol", fuelType.displayName)
        assertEquals("Bioalcohol", fuelType.categoryDisplayName)
    }
}
