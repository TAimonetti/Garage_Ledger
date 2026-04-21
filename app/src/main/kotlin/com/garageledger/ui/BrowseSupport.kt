package com.garageledger.ui

import com.garageledger.domain.model.BrowseRecordFilter
import com.garageledger.domain.model.BrowseRecordItem
import com.garageledger.domain.model.RecordFamily
import com.garageledger.domain.model.SavedBrowseSearch
import java.time.LocalDate

internal enum class BrowseRecordAction {
    VIEW,
    EDIT,
    FINISH_TRIP,
    COPY_TRIP,
    DELETE,
}

internal data class BrowseRecordActionItem(
    val action: BrowseRecordAction,
    val label: String,
    val enabled: Boolean = true,
)

internal data class BrowseFilterOptions(
    val tagSuggestions: List<String> = emptyList(),
    val subtypeSuggestions: List<String> = emptyList(),
    val paymentTypeSuggestions: List<String> = emptyList(),
    val eventPlaceSuggestions: List<String> = emptyList(),
    val fuelBrandSuggestions: List<String> = emptyList(),
    val fuelTypeSuggestions: List<String> = emptyList(),
    val fuelAdditiveSuggestions: List<String> = emptyList(),
    val drivingModeSuggestions: List<String> = emptyList(),
    val tripPurposeSuggestions: List<String> = emptyList(),
    val tripClientSuggestions: List<String> = emptyList(),
    val tripLocationSuggestions: List<String> = emptyList(),
)

internal enum class BrowseDatePreset(val label: String) {
    LAST_30_DAYS("Last 30 Days"),
    LAST_90_DAYS("Last 90 Days"),
    THIS_YEAR("This Year"),
}

internal enum class BrowseFilterTokenKey {
    VEHICLE,
    FAMILY,
    QUERY,
    TAG,
    FROM_DATE,
    TO_DATE,
    SUBTYPE,
    PAYMENT_TYPE,
    EVENT_PLACE,
    FUEL_BRAND,
    FUEL_TYPE,
    FUEL_ADDITIVE,
    DRIVING_MODE,
    TRIP_PURPOSE,
    TRIP_CLIENT,
    TRIP_LOCATION,
    TRIP_PAID_STATUS,
}

internal data class BrowseFilterToken(
    val key: BrowseFilterTokenKey,
    val label: String,
)

internal fun applyBrowseRecordFilter(
    records: List<BrowseRecordItem>,
    filter: BrowseRecordFilter,
): List<BrowseRecordItem> {
    val normalizedQuery = filter.query.trim().lowercase()
    val normalizedTag = filter.tag.trim().lowercase()
    val normalizedSubtype = filter.subtype.trim().lowercase()
    val normalizedPayment = filter.paymentType.trim().lowercase()
    val normalizedEventPlace = filter.eventPlace.trim().lowercase()
    val normalizedFuelBrand = filter.fuelBrand.trim().lowercase()
    val normalizedFuelType = filter.fuelType.trim().lowercase()
    val normalizedFuelAdditive = filter.fuelAdditive.trim().lowercase()
    val normalizedDrivingMode = filter.drivingMode.trim().lowercase()
    val normalizedTripPurpose = filter.tripPurpose.trim().lowercase()
    val normalizedTripClient = filter.tripClient.trim().lowercase()
    val normalizedTripLocation = filter.tripLocation.trim().lowercase()

    return records.filter { item ->
        (filter.vehicleId == null || item.vehicleId == filter.vehicleId) &&
            (filter.family == null || item.family == filter.family) &&
            (normalizedQuery.isBlank() || item.searchText.contains(normalizedQuery)) &&
            (normalizedTag.isBlank() || item.tags.any { it.lowercase().contains(normalizedTag) }) &&
            (filter.fromDate == null || !item.occurredAt.toLocalDate().isBefore(filter.fromDate)) &&
            (filter.toDate == null || !item.occurredAt.toLocalDate().isAfter(filter.toDate)) &&
            (normalizedSubtype.isBlank() || item.subtypeNames.any { it.lowercase().contains(normalizedSubtype) }) &&
            (normalizedPayment.isBlank() || item.paymentType.lowercase().contains(normalizedPayment)) &&
            (normalizedEventPlace.isBlank() || item.eventPlaceName.lowercase().contains(normalizedEventPlace)) &&
            (normalizedFuelBrand.isBlank() || item.fuelBrand.lowercase().contains(normalizedFuelBrand)) &&
            (normalizedFuelType.isBlank() || item.fuelTypeLabel.lowercase().contains(normalizedFuelType)) &&
            (normalizedFuelAdditive.isBlank() || item.fuelAdditiveName.lowercase().contains(normalizedFuelAdditive)) &&
            (normalizedDrivingMode.isBlank() || item.drivingMode.lowercase().contains(normalizedDrivingMode)) &&
            (normalizedTripPurpose.isBlank() || item.tripPurpose.lowercase().contains(normalizedTripPurpose)) &&
            (normalizedTripClient.isBlank() || item.tripClient.lowercase().contains(normalizedTripClient)) &&
            (normalizedTripLocation.isBlank() || item.tripLocations.any { it.lowercase().contains(normalizedTripLocation) }) &&
            (filter.tripPaidStatus == null || item.tripPaidStatus == filter.tripPaidStatus)
    }
}

internal fun buildBrowseFilterOptions(
    records: List<BrowseRecordItem>,
    family: RecordFamily?,
): BrowseFilterOptions {
    val scoped = records.filter { family == null || it.family == family }
    return BrowseFilterOptions(
        tagSuggestions = distinctSuggestions(scoped.flatMap(BrowseRecordItem::tags)),
        subtypeSuggestions = distinctSuggestions(scoped.flatMap(BrowseRecordItem::subtypeNames)),
        paymentTypeSuggestions = distinctSuggestions(scoped.map(BrowseRecordItem::paymentType)),
        eventPlaceSuggestions = distinctSuggestions(scoped.map(BrowseRecordItem::eventPlaceName)),
        fuelBrandSuggestions = distinctSuggestions(scoped.map(BrowseRecordItem::fuelBrand)),
        fuelTypeSuggestions = distinctSuggestions(scoped.map(BrowseRecordItem::fuelTypeLabel)),
        fuelAdditiveSuggestions = distinctSuggestions(scoped.map(BrowseRecordItem::fuelAdditiveName)),
        drivingModeSuggestions = distinctSuggestions(scoped.map(BrowseRecordItem::drivingMode)),
        tripPurposeSuggestions = distinctSuggestions(scoped.map(BrowseRecordItem::tripPurpose)),
        tripClientSuggestions = distinctSuggestions(scoped.map(BrowseRecordItem::tripClient)),
        tripLocationSuggestions = distinctSuggestions(scoped.flatMap(BrowseRecordItem::tripLocations)),
    )
}

internal fun buildBrowseActionItems(record: BrowseRecordItem): List<BrowseRecordActionItem> {
    val modificationsAllowed = record.vehicleLifecycle.allowsRecordModification()
    return buildList {
        add(BrowseRecordActionItem(BrowseRecordAction.VIEW, "View"))
        add(BrowseRecordActionItem(BrowseRecordAction.EDIT, "Edit", enabled = modificationsAllowed))
        if (record.family == RecordFamily.TRIP) {
            add(
                BrowseRecordActionItem(
                    action = if (record.tripOpen) BrowseRecordAction.FINISH_TRIP else BrowseRecordAction.COPY_TRIP,
                    label = if (record.tripOpen) "Finish Trip" else "Copy Trip",
                    enabled = modificationsAllowed,
                ),
            )
        }
        add(BrowseRecordActionItem(BrowseRecordAction.DELETE, "Delete", enabled = modificationsAllowed))
    }
}

internal fun sortBrowseRecords(
    records: List<BrowseRecordItem>,
    descending: Boolean,
): List<BrowseRecordItem> = if (descending) {
    records.sortedByDescending(BrowseRecordItem::occurredAt)
} else {
    records.sortedBy(BrowseRecordItem::occurredAt)
}

internal fun browseSortLabel(descending: Boolean): String = if (descending) {
    "Newest First"
} else {
    "Oldest First"
}

internal fun buildBrowseFilterTokens(
    filter: BrowseRecordFilter,
    vehicleName: String?,
): List<BrowseFilterToken> = buildList {
    vehicleName
        ?.takeIf(String::isNotBlank)
        ?.let { add(BrowseFilterToken(BrowseFilterTokenKey.VEHICLE, "Vehicle: $it")) }
    filter.family?.let { add(BrowseFilterToken(BrowseFilterTokenKey.FAMILY, "Type: ${it.displayLabel()}")) }
    filter.query.trim().takeIf(String::isNotBlank)?.let {
        add(BrowseFilterToken(BrowseFilterTokenKey.QUERY, "Search: $it"))
    }
    filter.tag.trim().takeIf(String::isNotBlank)?.let {
        add(BrowseFilterToken(BrowseFilterTokenKey.TAG, "Tag: $it"))
    }
    filter.fromDate?.let {
        add(BrowseFilterToken(BrowseFilterTokenKey.FROM_DATE, "From: $it"))
    }
    filter.toDate?.let {
        add(BrowseFilterToken(BrowseFilterTokenKey.TO_DATE, "To: $it"))
    }
    filter.subtype.trim().takeIf(String::isNotBlank)?.let {
        add(BrowseFilterToken(BrowseFilterTokenKey.SUBTYPE, "Subtype: $it"))
    }
    filter.paymentType.trim().takeIf(String::isNotBlank)?.let {
        add(BrowseFilterToken(BrowseFilterTokenKey.PAYMENT_TYPE, "Payment: $it"))
    }
    filter.eventPlace.trim().takeIf(String::isNotBlank)?.let {
        add(BrowseFilterToken(BrowseFilterTokenKey.EVENT_PLACE, "Place: $it"))
    }
    filter.fuelBrand.trim().takeIf(String::isNotBlank)?.let {
        add(BrowseFilterToken(BrowseFilterTokenKey.FUEL_BRAND, "Fuel Brand: $it"))
    }
    filter.fuelType.trim().takeIf(String::isNotBlank)?.let {
        add(BrowseFilterToken(BrowseFilterTokenKey.FUEL_TYPE, "Fuel Type: $it"))
    }
    filter.fuelAdditive.trim().takeIf(String::isNotBlank)?.let {
        add(BrowseFilterToken(BrowseFilterTokenKey.FUEL_ADDITIVE, "Additive: $it"))
    }
    filter.drivingMode.trim().takeIf(String::isNotBlank)?.let {
        add(BrowseFilterToken(BrowseFilterTokenKey.DRIVING_MODE, "Driving: $it"))
    }
    filter.tripPurpose.trim().takeIf(String::isNotBlank)?.let {
        add(BrowseFilterToken(BrowseFilterTokenKey.TRIP_PURPOSE, "Purpose: $it"))
    }
    filter.tripClient.trim().takeIf(String::isNotBlank)?.let {
        add(BrowseFilterToken(BrowseFilterTokenKey.TRIP_CLIENT, "Client: $it"))
    }
    filter.tripLocation.trim().takeIf(String::isNotBlank)?.let {
        add(BrowseFilterToken(BrowseFilterTokenKey.TRIP_LOCATION, "Location: $it"))
    }
    filter.tripPaidStatus?.let {
        add(BrowseFilterToken(BrowseFilterTokenKey.TRIP_PAID_STATUS, "Paid: ${it.name.lowercase().replaceFirstChar(Char::uppercase)}"))
    }
}

internal fun browseDatePresetRange(
    preset: BrowseDatePreset,
    today: LocalDate,
): Pair<LocalDate, LocalDate> = when (preset) {
    BrowseDatePreset.LAST_30_DAYS -> today.minusDays(29) to today
    BrowseDatePreset.LAST_90_DAYS -> today.minusDays(89) to today
    BrowseDatePreset.THIS_YEAR -> LocalDate.of(today.year, 1, 1) to today
}

internal fun resolveBrowseDatePreset(
    fromDate: LocalDate?,
    toDate: LocalDate?,
    today: LocalDate,
): BrowseDatePreset? = BrowseDatePreset.entries.firstOrNull { preset ->
    val (expectedFrom, expectedTo) = browseDatePresetRange(preset, today)
    fromDate == expectedFrom && toDate == expectedTo
}

internal fun BrowseRecordFilter.toSavedBrowseSearch(name: String): SavedBrowseSearch = SavedBrowseSearch(
    name = name.trim(),
    vehicleId = vehicleId,
    family = family,
    query = query.trim(),
    tag = tag.trim(),
    fromDateIso = fromDate?.let(::coerceDateText),
    toDateIso = toDate?.let(::coerceDateText),
    subtype = subtype.trim(),
    paymentType = paymentType.trim(),
    eventPlace = eventPlace.trim(),
    fuelBrand = fuelBrand.trim(),
    fuelType = fuelType.trim(),
    fuelAdditive = fuelAdditive.trim(),
    drivingMode = drivingMode.trim(),
    tripPurpose = tripPurpose.trim(),
    tripClient = tripClient.trim(),
    tripLocation = tripLocation.trim(),
    tripPaidStatus = tripPaidStatus,
)

internal fun SavedBrowseSearch.toBrowseRecordFilter(): BrowseRecordFilter = BrowseRecordFilter(
    vehicleId = vehicleId,
    family = family,
    query = query,
    tag = tag,
    fromDate = fromDateIso?.let(::parseFilterDate),
    toDate = toDateIso?.let(::parseFilterDate),
    subtype = subtype,
    paymentType = paymentType,
    eventPlace = eventPlace,
    fuelBrand = fuelBrand,
    fuelType = fuelType,
    fuelAdditive = fuelAdditive,
    drivingMode = drivingMode,
    tripPurpose = tripPurpose,
    tripClient = tripClient,
    tripLocation = tripLocation,
    tripPaidStatus = tripPaidStatus,
)

internal fun findSavedBrowseSearch(
    searches: List<SavedBrowseSearch>,
    name: String,
): SavedBrowseSearch? = searches.firstOrNull { it.name.equals(name.trim(), ignoreCase = true) }

private fun distinctSuggestions(values: List<String>): List<String> {
    val normalized = linkedMapOf<String, String>()
    values.forEach { value ->
        val trimmed = value.trim()
        if (trimmed.isNotBlank()) {
            normalized.putIfAbsent(trimmed.lowercase(), trimmed)
        }
    }
    return normalized.values.sortedBy(String::lowercase)
}
