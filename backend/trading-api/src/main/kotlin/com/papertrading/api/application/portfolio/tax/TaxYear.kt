package com.papertrading.api.application.portfolio.tax

import com.papertrading.api.common.exception.InvalidTaxYearException
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@JvmInline
value class TaxYear(val value: Int) {
    init {
        if (value !in 1900..2200) throw InvalidTaxYearException(value)
    }

    fun startInclusive(): Instant = LocalDate.of(value, 1, 1).atStartOfDay().toInstant(ZoneOffset.UTC)

    fun endExclusive(): Instant = LocalDate.of(value + 1, 1, 1).atStartOfDay().toInstant(ZoneOffset.UTC)
}

