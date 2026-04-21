package com.garageledger.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.garageledger.domain.model.OptionalFieldToggle
import com.garageledger.domain.model.RecordFamily

internal fun RecordFamily.routeSegment(): String = name.lowercase()

internal fun recordFamilyFromRouteSegment(routeSegment: String?): RecordFamily? = routeSegment
    ?.uppercase()
    ?.let { value -> runCatching { enumValueOf<RecordFamily>(value) }.getOrNull() }

internal data class VisibleFieldOption(
    val toggle: OptionalFieldToggle,
    val label: String,
)

internal val FuelUpVisibleFieldOptions: List<VisibleFieldOption> = listOf(
    VisibleFieldOption(OptionalFieldToggle.PAYMENT_TYPE, "Payment Type"),
    VisibleFieldOption(OptionalFieldToggle.FUEL_TYPE, "Fuel Type"),
    VisibleFieldOption(OptionalFieldToggle.FUEL_ADDITIVE, "Fuel Additive"),
    VisibleFieldOption(OptionalFieldToggle.FUELING_STATION, "Fuel Brand & Station"),
    VisibleFieldOption(OptionalFieldToggle.AVERAGE_SPEED, "Average Speed"),
    VisibleFieldOption(OptionalFieldToggle.DRIVING_MODE, "Driving Mode"),
    VisibleFieldOption(OptionalFieldToggle.DRIVING_CONDITION, "Driving Condition"),
    VisibleFieldOption(OptionalFieldToggle.TAGS, "Tags"),
    VisibleFieldOption(OptionalFieldToggle.NOTES, "Notes"),
)

internal val ServiceVisibleFieldOptions: List<VisibleFieldOption> = listOf(
    VisibleFieldOption(OptionalFieldToggle.PAYMENT_TYPE, "Payment Type"),
    VisibleFieldOption(OptionalFieldToggle.SERVICE_CENTER, "Service Center"),
    VisibleFieldOption(OptionalFieldToggle.TAGS, "Tags"),
    VisibleFieldOption(OptionalFieldToggle.NOTES, "Notes"),
)

internal val ExpenseVisibleFieldOptions: List<VisibleFieldOption> = listOf(
    VisibleFieldOption(OptionalFieldToggle.PAYMENT_TYPE, "Payment Type"),
    VisibleFieldOption(OptionalFieldToggle.EXPENSE_CENTER, "Expense Center"),
    VisibleFieldOption(OptionalFieldToggle.TAGS, "Tags"),
    VisibleFieldOption(OptionalFieldToggle.NOTES, "Notes"),
)

internal val TripVisibleFieldOptions: List<VisibleFieldOption> = listOf(
    VisibleFieldOption(OptionalFieldToggle.TRIP_LOCATION, "Start & End Location"),
    VisibleFieldOption(OptionalFieldToggle.TRIP_PURPOSE, "Purpose"),
    VisibleFieldOption(OptionalFieldToggle.TRIP_CLIENT, "Client"),
    VisibleFieldOption(OptionalFieldToggle.TRIP_TAX_DEDUCTION, "Tax Deduction"),
    VisibleFieldOption(OptionalFieldToggle.TRIP_REIMBURSEMENT, "Reimbursement & Paid"),
    VisibleFieldOption(OptionalFieldToggle.TAGS, "Tags"),
    VisibleFieldOption(OptionalFieldToggle.NOTES, "Notes"),
)

@Composable
internal fun VisibleFieldsDialog(
    title: String,
    options: List<VisibleFieldOption>,
    visibleFields: Set<OptionalFieldToggle>,
    onToggle: (OptionalFieldToggle, Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done") }
        },
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Turn optional fields on or off for this editor. Hidden fields keep their saved values.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                LazyColumn(
                    modifier = Modifier.heightIn(max = 360.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    items(options) { option ->
                        ToggleRow(
                            label = option.label,
                            checked = option.toggle in visibleFields,
                            onCheckedChange = { onToggle(option.toggle, it) },
                        )
                    }
                }
            }
        },
    )
}
