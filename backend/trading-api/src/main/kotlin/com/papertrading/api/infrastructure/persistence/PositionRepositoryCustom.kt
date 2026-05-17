package com.papertrading.api.infrastructure.persistence

import com.papertrading.api.domain.entity.position.Position
import com.papertrading.api.domain.enums.TradingMode
import java.math.BigDecimal

interface PositionRepositoryCustom {
    fun findOpenByTickerAndMode(
        ticker: String,
        tradingMode: TradingMode,
        minQuantity: BigDecimal = BigDecimal.ZERO,
    ): List<Position>
}
