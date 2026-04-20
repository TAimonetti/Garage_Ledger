package com.garageledger.widgets

import com.garageledger.domain.model.ReminderWidgetItem

object ReminderWidgetFormatter {
    fun title(item: ReminderWidgetItem): String =
        listOf(item.vehicleName.ifBlank { "Vehicle" }, item.serviceTypeName)
            .joinToString(": ")

    fun subtitle(item: ReminderWidgetItem): String = listOfNotNull(
        item.dueDate?.let { "Due $it" },
        item.dueDistance?.let { "At ${it.toInt()} mi" },
    ).joinToString(" | ").ifBlank { "Scheduled" }
}
