package com.papertrading.api.application.account

import com.papertrading.api.application.account.kis.KisAccountBalancePort
import com.papertrading.api.application.account.kis.KisAccountBalanceResult
import com.papertrading.api.application.account.kis.KisAccountMode
import com.papertrading.api.application.account.kis.KisBalancePositionResult
import com.papertrading.api.application.account.kis.KisReconciliationResult
import com.papertrading.api.infrastructure.persistence.PositionRepository
import org.springframework.stereotype.Service
import java.math.BigDecimal

@Service
class KisAccountQueryService(
    private val kisAccountBalancePort: KisAccountBalancePort,
    private val positionRepository: PositionRepository,
) {
    fun getBalance(accountId: Long, mode: KisAccountMode): KisAccountBalanceResult {
        require(accountId > 0) { "accountId must be positive" }

        val trId = when (mode) {
            KisAccountMode.LIVE -> "TTTC8434R"
            KisAccountMode.PAPER -> "VTTC8434R"
        }
        val snapshot = kisAccountBalancePort.fetchBalance(accountId, trId)
        val localPositions = positionRepository.findByAccountIdAndQuantityGreaterThan(accountId, BigDecimal.ZERO)

        val kisQuantities = snapshot.positions.associate { it.ticker to it.quantity }
        val localQuantities = localPositions.associate { (it.ticker ?: "") to it.quantity }

        val missingInLocal = kisQuantities.keys.filter { !localQuantities.containsKey(it) }.sorted()
        val missingInKis = localQuantities.keys.filter { it.isNotBlank() && !kisQuantities.containsKey(it) }.sorted()
        val quantityMismatch = kisQuantities.keys
            .filter { localQuantities.containsKey(it) && localQuantities[it]!!.compareTo(kisQuantities[it]) != 0 }
            .sorted()

        return KisAccountBalanceResult(
            accountId = accountId,
            mode = mode,
            asOf = snapshot.asOf,
            cashBalance = snapshot.cashBalance,
            marketValue = snapshot.marketValue,
            unrealizedPnl = snapshot.unrealizedPnl,
            returnRate = snapshot.returnRate,
            positions = snapshot.positions.map {
                KisBalancePositionResult(
                    ticker = it.ticker,
                    quantity = it.quantity,
                    avgPrice = it.avgPrice,
                    currentPrice = it.currentPrice,
                    marketValue = it.marketValue,
                    unrealizedPnl = it.unrealizedPnl,
                    returnRate = it.returnRate
                )
            },
            reconciliation = KisReconciliationResult(
                missingInLocal = missingInLocal,
                missingInKis = missingInKis,
                quantityMismatch = quantityMismatch
            )
        )
    }
}
