package com.papertrading.api.presentation.dto.portfolio

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

data class TradingJournalUpdateRequest(
    @field:NotNull val accountId: Long,
    @field:NotBlank @field:Size(max = 200) val title: String,
    @field:NotBlank val content: String,
    @field:Pattern(regexp = "^(BULLISH|BEARISH|NEUTRAL)$")
    @field:Size(max = 20) val sentiment: String? = null
)
