package com.garageledger.domain.calc

import java.time.LocalDateTime

data class ChronoOdometerRecord(
    val id: Long,
    val dateTime: LocalDateTime,
    val odometer: Double,
)

object RecordConsistencyValidator {
    fun validateInsertion(
        siblings: List<ChronoOdometerRecord>,
        candidate: ChronoOdometerRecord,
    ): ValidationResult {
        val combined = (siblings + candidate).sortedWith(compareBy<ChronoOdometerRecord> { it.dateTime }.thenBy { it.odometer })
        for (index in 1 until combined.size) {
            val previous = combined[index - 1]
            val current = combined[index]
            if (current.odometer < previous.odometer) {
                return ValidationResult.Invalid(
                    "Record odometer/date is not in sync with neighboring records.",
                    previous,
                    current,
                )
            }
        }
        return ValidationResult.Valid
    }

    sealed interface ValidationResult {
        data object Valid : ValidationResult
        data class Invalid(
            val message: String,
            val previous: ChronoOdometerRecord,
            val current: ChronoOdometerRecord,
        ) : ValidationResult
    }
}
