package com.papertrading.api.application.portfolio

import com.papertrading.api.domain.entity.portfolio.PortfolioSnapshot
import com.papertrading.api.domain.entity.position.Position
import com.papertrading.api.domain.port.MarketQuotePort
import com.papertrading.api.infrastructure.persistence.AccountRepository
import com.papertrading.api.infrastructure.persistence.PortfolioSnapshotRepository
import com.papertrading.api.infrastructure.persistence.PositionRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate

@Service
class PortfolioSnapshotCommandService(
    private val accountRepository: AccountRepository,
    private val positionRepository: PositionRepository,
    private val portfolioSnapshotRepository: PortfolioSnapshotRepository,
    private val marketQuotePort: MarketQuotePort,
) {
    @Transactional
    fun recalculate(accountId: Long, businessDate: LocalDate): List<PortfolioSnapshot> {
        val account = accountRepository.findByIdWithLock(accountId)
            .orElseThrow { NoSuchElementException("계좌를 찾을 수 없습니다. id=$accountId") }
        val positions = positionRepository.findByAccountIdAndQuantityGreaterThan(accountId, BigDecimal.ZERO)

        val priced = positions.map { position ->
            val closePrice = marketQuotePort.getQuote(position.ticker!!)?.price ?: position.currentPrice ?: BigDecimal.ZERO
            PricedPosition(position, closePrice, closePrice.multiply(position.quantity))
        }
        val totalMarketValue = priced.fold(BigDecimal.ZERO) { acc, item -> acc.add(item.marketValue) }

        return priced.map { item ->
            val weight = if (totalMarketValue.compareTo(BigDecimal.ZERO) == 0) {
                BigDecimal.ZERO.setScale(6)
            } else {
                item.marketValue.divide(totalMarketValue, 6, RoundingMode.HALF_UP)
            }
            val unrealizedPnl = item.closePrice.subtract(item.position.avgBuyPrice).multiply(item.position.quantity)

            val entity = portfolioSnapshotRepository.findByAccountIdAndBusinessDateAndTicker(
                accountId = accountId,
                businessDate = businessDate,
                ticker = item.position.ticker!!,
            ).map {
                it.refresh(
                    quantity = item.position.quantity,
                    avgBuyPrice = item.position.avgBuyPrice,
                    closePrice = item.closePrice,
                    marketValue = item.marketValue,
                    weight = weight,
                    unrealizedPnl = unrealizedPnl,
                )
                it
            }.orElseGet {
                PortfolioSnapshot.create(
                    account = account,
                    businessDate = businessDate,
                    ticker = item.position.ticker!!,
                    quantity = item.position.quantity,
                    avgBuyPrice = item.position.avgBuyPrice,
                    closePrice = item.closePrice,
                    marketValue = item.marketValue,
                    weight = weight,
                    unrealizedPnl = unrealizedPnl,
                )
            }
            portfolioSnapshotRepository.save(entity)
        }
    }

    private data class PricedPosition(
        val position: Position,
        val closePrice: BigDecimal,
        val marketValue: BigDecimal,
    )
}