package com.papertrading.api.application.portfolio

import com.papertrading.api.domain.entity.portfolio.DailyBalance
import com.papertrading.api.infrastructure.persistence.AccountRepository
import com.papertrading.api.infrastructure.persistence.DailyBalanceRepository
import com.papertrading.api.infrastructure.persistence.PositionRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate

@Service
class DailyBalanceCommandService(
    private val accountRepository: AccountRepository,
    private val positionRepository: PositionRepository,
    private val dailyBalanceRepository: DailyBalanceRepository,
) {
    @Transactional
    fun recalculate(accountId: Long, businessDate: LocalDate): DailyBalance {
        val account = accountRepository.findByIdWithLock(accountId)
            .orElseThrow { NoSuchElementException("계좌를 찾을 수 없습니다. id=$accountId") }
        val positions = positionRepository.findByAccountIdAndQuantityGreaterThan(accountId, BigDecimal.ZERO)

        val cashBalance = account.availableDeposit
        val stockMarketValue = positions.fold(BigDecimal.ZERO) { acc, position ->
            acc.add((position.currentPrice ?: BigDecimal.ZERO).multiply(position.quantity))
        }
        val totalAssetValue = cashBalance.add(stockMarketValue)
        val pnlAmount = totalAssetValue.subtract(account.deposit)
        val pnlRate = if (account.deposit.compareTo(BigDecimal.ZERO) == 0) {
            BigDecimal.ZERO.setScale(6)
        } else {
            pnlAmount.divide(account.deposit, 6, RoundingMode.HALF_UP)
        }

        val entity = dailyBalanceRepository.findByAccountIdAndBusinessDate(accountId, businessDate)
            .map {
                it.refresh(
                    cashBalance = cashBalance,
                    stockMarketValue = stockMarketValue,
                    totalAssetValue = totalAssetValue,
                    pnlAmount = pnlAmount,
                    pnlRate = pnlRate,
                )
                it
            }
            .orElseGet {
                DailyBalance.create(
                    account = account,
                    businessDate = businessDate,
                    cashBalance = cashBalance,
                    stockMarketValue = stockMarketValue,
                    totalAssetValue = totalAssetValue,
                    pnlAmount = pnlAmount,
                    pnlRate = pnlRate,
                )
            }

        return dailyBalanceRepository.save(entity)
    }
}