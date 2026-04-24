package com.papertrading.api.application.position

import com.papertrading.api.application.position.result.PositionResult
import com.papertrading.api.domain.enums.PriceSource
import com.papertrading.api.domain.port.MarketQuotePort
import com.papertrading.api.infrastructure.persistence.PositionRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

@Service
@Transactional(readOnly = true)
class PositionQueryService(
    private val positionRepository: PositionRepository,
    private val marketQuotePort: MarketQuotePort,
) {
    /** 보유 포지션 목록 (quantity > 0). Redis 현재가 주입. */
    fun listPositionsWithCurrentPrice(accountId: Long): List<PositionResult> {
        val positions = positionRepository.findByAccountIdAndQuantityGreaterThan(accountId, BigDecimal.ZERO)
        return positions.map { position ->
            val quote = marketQuotePort.getQuote(requireNotNull(position.ticker))
            if (quote != null) {
                position.updatePrice(quote.price, PriceSource.REDIS_LIVE)
            }
            PositionResult.from(position)
        }
    }

    /** 단건 조회. Redis 현재가 주입. */
    fun getPositionWithCurrentPrice(accountId: Long, ticker: String): PositionResult {
        val position = positionRepository.findByAccountIdAndTicker(accountId, ticker)
            .orElseThrow { NoSuchElementException("포지션을 찾을 수 없습니다. ticker=$ticker") }
        val quote = marketQuotePort.getQuote(ticker)
        if (quote != null) {
            position.updatePrice(quote.price, PriceSource.REDIS_LIVE)
        }
        return PositionResult.from(position)
    }
}