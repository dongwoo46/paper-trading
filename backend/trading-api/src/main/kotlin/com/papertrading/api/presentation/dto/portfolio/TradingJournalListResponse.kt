package com.papertrading.api.presentation.dto.portfolio

import org.springframework.data.domain.Page

data class TradingJournalListResponse(
    val items: List<TradingJournalResponse>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int
) {
    companion object {
        fun from(pageResult: Page<TradingJournalResponse>): TradingJournalListResponse = TradingJournalListResponse(
            items = pageResult.content,
            page = pageResult.number,
            size = pageResult.size,
            totalElements = pageResult.totalElements,
            totalPages = pageResult.totalPages
        )
    }
}
