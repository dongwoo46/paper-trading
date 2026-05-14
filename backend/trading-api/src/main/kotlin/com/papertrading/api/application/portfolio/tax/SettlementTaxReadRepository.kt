package com.papertrading.api.application.portfolio.tax

import java.time.Instant

/**
 * Settlement 테이블에서 세금 요약 계산용 원천 데이터를 집계한다.
 * 계산 로직 자체는 CommandService가 담당하고, 이 포트는 조회/집계 책임만 가진다.
 */
interface SettlementTaxReadRepository {
    fun summarizeForTax(accountId: Long, yearStartInclusive: Instant, yearEndExclusive: Instant): TaxSettlementAggregate
}
