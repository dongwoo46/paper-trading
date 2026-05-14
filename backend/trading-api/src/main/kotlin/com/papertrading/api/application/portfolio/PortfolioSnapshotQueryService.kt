package com.papertrading.api.application.portfolio

import com.papertrading.api.common.exception.AccountNotFoundException
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
    // 특정 날짜 기준 포트폴리오 상태 조회
    fun getPortfolioSnapshots(accountId: Long, businessDate: LocalDate): List<PortfolioSnapshot> {
        accountRepository.findById(accountId)
            .orElseThrow { AccountNotFoundException(accountId) }

        return portfolioSnapshotRepository.searchByAccountIdAndBusinessDate(
            accountId = accountId,
            businessDate = businessDate,
        )
    }
}
