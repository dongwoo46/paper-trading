package com.papertrading.api.application.portfolio

import com.papertrading.api.domain.entity.portfolio.PortfolioSnapshot
import com.papertrading.api.infrastructure.persistence.AccountRepository
import com.papertrading.api.infrastructure.persistence.PortfolioSnapshotRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
@Transactional(readOnly = true)
class PortfolioSnapshotQueryService(
    private val accountRepository: AccountRepository,
    private val portfolioSnapshotRepository: PortfolioSnapshotRepository,
) {
    fun getPortfolioSnapshots(accountId: Long, businessDate: LocalDate): List<PortfolioSnapshot> {
        accountRepository.findById(accountId)
            .orElseThrow { NoSuchElementException("계좌를 찾을 수 없습니다. id=$accountId") }

        return portfolioSnapshotRepository.findByAccountIdAndBusinessDateOrderByTickerAsc(
            accountId = accountId,
            businessDate = businessDate,
        )
    }
}