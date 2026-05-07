package com.papertrading.api.application.portfolio.tax

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@JvmInline
value class TaxYear(val value: Int) {
    init {
        require(value in 1900..2200) { "유효하지 않은 taxYear 입니다. value=$value" }
    }

    fun startInclusive(): Instant = LocalDate.of(value, 1, 1).atStartOfDay().toInstant(ZoneOffset.UTC)

    fun endExclusive(): Instant = LocalDate.of(value + 1, 1, 1).atStartOfDay().toInstant(ZoneOffset.UTC)
}
