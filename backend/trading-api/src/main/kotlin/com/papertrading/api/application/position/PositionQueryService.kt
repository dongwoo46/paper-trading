package com.papertrading.api.application.position

import com.papertrading.api.application.position.result.PositionResult
import com.papertrading.api.common.exception.PositionNotFoundException
import com.papertrading.api.domain.entity.position.Position
import com.papertrading.api.domain.enums.PriceSource
import com.papertrading.api.domain.port.LivePositionCachePort
import com.papertrading.api.domain.port.LivePositionSnapshot
import com.papertrading.api.domain.port.MarketQuotePort
import com.papertrading.api.infrastructure.persistence.PositionRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

// 포지션 조회는 Account에 가지고 있는 포지션 정보 조회해야함
@Service
@Transactional(readOnly = true)
class PositionQueryService(
    private val positionRepository: PositionRepository,
    private val livePositionCachePort: LivePositionCachePort,
    private val marketQuotePort: MarketQuotePort,
) {
    /** 보유 포지션 목록 (quantity > 0). Redis live position cache 우선, 없으면 DB fallback. */
    fun listPositionsWithCurrentPrice(accountId: Long): List<PositionResult> {
        val snapshots = livePositionCachePort.findByAccountId(accountId).ifEmpty {
            positionRepository.findByAccountIdAndQuantityGreaterThan(accountId, BigDecimal.ZERO)
                .map { snapshotFromDbWithLatestQuote(it) }
        }
        return snapshots.map { PositionResult.from(it) }
    }

    /** 단건 조회. Redis live position cache 우선, 없으면 DB fallback. */
    fun getPositionWithCurrentPrice(accountId: Long, ticker: String): PositionResult {
        val normalizedTicker = ticker.trim().uppercase()
        livePositionCachePort.find(accountId, normalizedTicker)?.let {
            return PositionResult.from(it)
        }

        val position = positionRepository.findByAccountIdAndTicker(accountId, normalizedTicker)
            .orElseThrow { PositionNotFoundException(ticker = normalizedTicker) }
        return PositionResult.from(snapshotFor(position))
    }

    private fun snapshotFor(position: Position): LivePositionSnapshot =
        livePositionCachePort.find(position.account.id!!, position.ticker)
            ?: snapshotFromDbWithLatestQuote(position)

    private fun snapshotFromDbWithLatestQuote(position: Position): LivePositionSnapshot {
        val snapshot = LivePositionSnapshot.fromPosition(position)
        val quote = marketQuotePort.getQuote(position.account.tradingMode, position.ticker)
        val evaluated = if (quote != null) {
            snapshot.evaluate(quote.price, PriceSource.REDIS_LIVE, quote.updatedAt)
        } else {
            snapshot
        }
        return livePositionCachePort.save(evaluated)
    }
}
