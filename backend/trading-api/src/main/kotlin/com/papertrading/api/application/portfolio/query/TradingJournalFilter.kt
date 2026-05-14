package com.papertrading.api.application.portfolio.query

import java.time.LocalDate

data class TradingJournalFilter(
    val accountId: Long,

    // 연결 대상 필터
    val orderId: Long? = null,
    val ticker: String? = null,

    // 일지 분류 필터
    val journalType: String? = null,
    val sentiment: String? = null,

    // 제목/본문 검색
    val keyword: String? = null,

    // 작성일 기준 기간 검색
    val createdFrom: LocalDate? = null,
    val createdTo: LocalDate? = null,
)
