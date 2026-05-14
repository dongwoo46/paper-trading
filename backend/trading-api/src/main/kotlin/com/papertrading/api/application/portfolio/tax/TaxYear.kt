package com.papertrading.api.application.portfolio.tax

import com.papertrading.api.common.exception.InvalidTaxYearException
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@JvmInline
value class TaxYear(val value: Int) {
    init {
        if (value !in MIN_YEAR..MAX_YEAR) throw InvalidTaxYearException(value)
    }

    fun startInclusive(): Instant = LocalDate.of(value, 1, 1).atStartOfDay().toInstant(ZoneOffset.UTC)

    fun endExclusive(): Instant = LocalDate.of(value + 1, 1, 1).atStartOfDay().toInstant(ZoneOffset.UTC)

    fun contains(at: Instant): Boolean = !at.isBefore(startInclusive()) && at.isBefore(endExclusive())

    companion object {
        const val MIN_YEAR: Int = 1900
        const val MAX_YEAR: Int = 2200
    }
}
