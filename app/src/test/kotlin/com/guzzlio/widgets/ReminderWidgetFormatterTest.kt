package com.guzzlio.widgets

import com.guzzlio.domain.model.ReminderWidgetItem
import com.google.common.truth.Truth.assertThat
import java.time.LocalDate
import org.junit.Test

class ReminderWidgetFormatterTest {
    @Test
    fun formatsReminderTitleAndSubtitle() {
        val item = ReminderWidgetItem(
            reminderId = 7L,
            vehicleName = "Corolla",
            serviceTypeName = "Oil Change",
            dueDate = LocalDate.parse("2026-10-01"),
            dueDistance = 130000.0,
        )

        assertThat(ReminderWidgetFormatter.title(item)).isEqualTo("Corolla: Oil Change")
        assertThat(ReminderWidgetFormatter.subtitle(item)).isEqualTo("Due 2026-10-01 | At 130000 mi")
    }
}
