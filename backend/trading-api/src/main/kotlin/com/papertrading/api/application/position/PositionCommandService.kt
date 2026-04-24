package com.papertrading.api.application.position

import com.papertrading.api.domain.enums.PriceSource
import com.papertrading.api.infrastructure.persistence.PositionRepository
import mu.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

@Service
class PositionCommandService(
    private val positionRepository: PositionRepository,
) {
    private val log = KotlinLogging.logger {}

    /**
     * Redis 시세 수신 시 해당 ticker 보유 포지션 평가손익 갱신.
     * QuoteEventListener에서 호출.
     */
    @Transactional
    fun updateCurrentPriceByTicker(ticker: String, price: BigDecimal, source: PriceSource) {
        val positions = positionRepository.findByTickerAndQuantityGreaterThan(ticker, BigDecimal.ZERO)
        if (positions.isEmpty()) return
        positions.forEach { it.updatePrice(price, source) }
        log.debug { "포지션 시세 갱신: ticker=$ticker, price=$price, count=${positions.size}" }
    }
}