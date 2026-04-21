package com.garageledger.ui

import com.garageledger.domain.model.BrowseRecordFilter
import com.garageledger.domain.model.BrowseRecordItem
import com.garageledger.domain.model.RecordFamily

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
