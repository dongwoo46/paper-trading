package com.papertrading.api.infrastructure.persistence

import com.papertrading.api.domain.enums.MarketType
import com.papertrading.api.domain.enums.TradingMode
import com.papertrading.api.domain.model.FeePolicy
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.Instant
import java.util.Optional

interface FeePolicyRepository : JpaRepository<FeePolicy, Long> {
    @Query(
        """
        SELECT f FROM FeePolicy f
        WHERE f.tradingMode = :mode AND f.marketType = :market
          AND f.effectiveFrom <= :now
          AND (f.effectiveUntil IS NULL OR f.effectiveUntil > :now)
        """
    )
    fun findActivePolicy(
        @Param("mode") tradingMode: TradingMode,
        @Param("market") marketType: MarketType,
        @Param("now") now: Instant
    ): Optional<FeePolicy>
}
