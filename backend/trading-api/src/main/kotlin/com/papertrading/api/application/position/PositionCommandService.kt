package com.papertrading.api.application.position

import com.papertrading.api.domain.enums.PriceSource
import com.papertrading.api.domain.enums.TradingMode
import com.papertrading.api.domain.port.LivePositionCachePort
import com.papertrading.api.domain.port.LivePositionSnapshot
import com.papertrading.api.infrastructure.persistence.PositionRepository
import mu.KotlinLogging
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.Instant

@Service
class PositionCommandService(
    private val positionRepository: PositionRepository,
    private val livePositionCachePort: LivePositionCachePort,
) {
    private val log = KotlinLogging.logger {}

    /**
     * Redis 시세 수신 시 해당 ticker 보유 포지션의 실시간 평가손익 캐시 갱신.
     * QuoteEventListener에서 호출.
     */
    fun updateCurrentPriceByTicker(
        ticker: String,
        price: BigDecimal,
        source: PriceSource,
        tradingMode: TradingMode,
    ) {
        val normalizedTicker = ticker.trim().uppercase()
        val now = Instant.now()
        val snapshots = livePositionCachePort.findByTickerAndMode(normalizedTicker, tradingMode).ifEmpty {
            positionRepository.findOpenByTickerAndMode(
                normalizedTicker,
                tradingMode,
                BigDecimal.ZERO,
            ).map { LivePositionSnapshot.fromPosition(it) }
        }
        if (snapshots.isEmpty()) return

        snapshots.forEach {
            livePositionCachePort.save(it.evaluate(price, source, now))
        }
        log.debug {
            "포지션 실시간 평가 캐시 갱신: ticker=$normalizedTicker, tradingMode=$tradingMode, price=$price, count=${snapshots.size}"
        }
    }
}
