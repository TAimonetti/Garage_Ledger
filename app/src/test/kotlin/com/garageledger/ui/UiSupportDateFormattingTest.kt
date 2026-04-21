package com.garageledger.ui

import com.garageledger.domain.model.AppPreferenceSnapshot
import com.google.common.truth.Truth.assertThat
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Locale
import org.junit.Test

class UiSupportDateFormattingTest {
    @Test
    fun formatForDisplay_usesPreferencePatterns() {
        val preferences = AppPreferenceSnapshot(
            fullDateFormat = "dd MMM yyyy",
            compactDateFormat = "yy/MM/dd",
            localeTag = "en-US",
        )

        assertThat(LocalDate.parse("2026-05-02").formatForDisplay(preferences)).isEqualTo("02 May 2026")
        assertThat(LocalDate.parse("2026-05-02").formatForDisplay(preferences, compact = true)).isEqualTo("26/05/02")
        assertThat(LocalDateTime.parse("2026-05-02T14:45:00").formatForDisplay(preferences)).isEqualTo("02 May 2026 14:45")
    }

    @Test
    fun formatForDisplay_fallsBackWhenPatternIsInvalid() {
        val preferences = AppPreferenceSnapshot(
            fullDateFormat = "not [valid",
            compactDateFormat = "still [bad",
            localeTag = "system",
        )

        assertThat(LocalDate.parse("2026-05-02").formatForDisplay(preferences)).isEqualTo("May 02, 2026")
        assertThat(LocalDate.parse("2026-05-02").formatForDisplay(preferences, compact = true)).isEqualTo("05/02/26")
        assertThat(LocalDateTime.parse("2026-05-02T14:45:00").formatForDisplay(preferences)).isEqualTo("May 02, 2026 14:45")
    }

    @Test
    fun formatForDisplay_honorsLocaleTag() {
        val previous = Locale.getDefault()
        Locale.setDefault(Locale.US)
        try {
            val preferences = AppPreferenceSnapshot(
                fullDateFormat = "dd MMM yyyy",
                localeTag = "fr-FR",
            )

            assertThat(LocalDate.parse("2026-05-02").formatForDisplay(preferences)).isEqualTo("02 mai 2026")
        } finally {
            Locale.setDefault(previous)
        }
    }

    @Test
    fun toDisplayDateOrNull_returnsNullForUnparseableInput() {
        val preferences = AppPreferenceSnapshot(fullDateFormat = "dd MMM yyyy")

        assertThat("bad".toDisplayDateOrNull(preferences)).isNull()
        assertThat("2026-05-02".toDisplayDateOrNull(preferences)).isEqualTo("02 May 2026")
    }
}
