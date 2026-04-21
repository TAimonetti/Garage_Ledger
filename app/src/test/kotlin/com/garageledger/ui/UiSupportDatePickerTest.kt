package com.garageledger.ui

import com.google.common.truth.Truth.assertThat
import java.time.LocalDate
import java.time.LocalDateTime
import org.junit.Test

class UiSupportDatePickerTest {
    @Test
    fun applyPickedDateToEditorDateTime_preservesTime() {
        val updated = applyPickedDateToEditorDateTime(
            raw = "2026-04-20 09:15",
            pickedDate = LocalDate.parse("2026-05-02"),
        )

        assertThat(updated).isEqualTo("2026-05-02 09:15")
    }

    @Test
    fun applyPickedTimeToEditorDateTime_preservesDate() {
        val updated = applyPickedTimeToEditorDateTime(
            raw = "2026-04-20 09:15",
            hour = 14,
            minute = 45,
        )

        assertThat(updated).isEqualTo("2026-04-20 14:45")
    }

    @Test
    fun applyPickedDateToEditorDateTime_fallsBackWhenTextIsBlank() {
        val updated = applyPickedDateToEditorDateTime(
            raw = "",
            pickedDate = LocalDate.parse("2026-05-02"),
            fallback = LocalDateTime.parse("2026-04-20T09:15:00"),
        )

        assertThat(updated).isEqualTo("2026-05-02 09:15")
    }

    @Test
    fun coerceDateText_formatsIsoDate() {
        assertThat(coerceDateText(LocalDate.parse("2026-05-02"))).isEqualTo("2026-05-02")
    }
}
