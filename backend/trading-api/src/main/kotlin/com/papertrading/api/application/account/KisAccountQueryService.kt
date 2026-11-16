package com.papertrading.api.application.account

import com.papertrading.api.application.account.port.BrokerAccountBalancePort
import com.papertrading.api.application.account.port.BrokerAccountRef
import com.papertrading.api.application.account.port.BrokerBalancePosition
import com.papertrading.api.application.account.port.BrokerBalanceSnapshot
import com.papertrading.api.application.account.port.BrokerCurrencyBalance
import com.papertrading.api.application.account.result.KisAccountBalanceResult
import com.papertrading.api.application.account.result.KisBalancePositionResult
import com.papertrading.api.application.account.result.KisReconciliationResult
import com.papertrading.api.common.exception.InternalErrorException
import com.papertrading.api.common.exception.InvalidAccountIdException
import com.papertrading.api.domain.entity.position.Position
import com.papertrading.api.domain.enums.KisAccountMode
import com.papertrading.api.domain.enums.TradingMode
import com.papertrading.api.infrastructure.persistence.PositionRepository
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.ZoneId
import java.time.ZoneOffset

/**
 * KIS 계좌 잔고 조회 (D23), 공통 잔고 포트의 KIS 구현을 부르고 대사를 더함
 * 어댑터 모드는 계좌의 tradingMode 가 아니라 mode 파라미터가 정함 (가정 2), 응답 표기는 옛 계약 그대로(금액 JSON 숫자, asOf UTC)
 */
@Service
class KisAccountQueryService(
    private val accountQueryService: AccountQueryService,
    balancePorts: List<BrokerAccountBalancePort>,
    private val positionRepository: PositionRepository,
) {
    private val portTable = BrokerBalancePortTable(balancePorts)

    fun getBalance(accountId: Long, mode: KisAccountMode): KisAccountBalanceResult {
        if (accountId <= 0) throw InvalidAccountIdException(accountId)

        val tradingMode = when (mode) {
            KisAccountMode.LIVE -> TradingMode.KIS_LIVE
            KisAccountMode.PAPER -> TradingMode.KIS_PAPER
        }
        val externalAccountId = linkedKisAccountNumber(accountQueryService.getAccount(accountId).externalAccountId)
        val snapshot = portTable.portFor(tradingMode)
            .fetchBalance(BrokerAccountRef(accountId, tradingMode, externalAccountId))
        val krw = krwBalance(snapshot)
        val localPositions = positionRepository.findByAccountIdAndQuantityGreaterThan(accountId, BigDecimal.ZERO)

        return KisAccountBalanceResult(
            accountId = accountId,
            mode = mode,
            asOf = snapshot.asOf.atZone(KST).withZoneSameInstant(ZoneOffset.UTC).toOffsetDateTime(),
            cashBalance = krw.cashBalance.orInvalid("cashBalance"),
            marketValue = krw.marketValue.orInvalid("marketValue"),
            unrealizedPnl = krw.unrealizedPnl.orInvalid("unrealizedPnl"),
            returnRate = krw.returnRate.orInvalid("returnRate"),
            positions = snapshot.positions.map {
                KisBalancePositionResult(
                    ticker = it.ticker,
                    quantity = it.quantity,
                    avgPrice = it.avgPrice,
                    currentPrice = it.currentPrice,
                    marketValue = it.marketValue,
                    unrealizedPnl = it.unrealizedPnl,
                    returnRate = it.returnRate,
                )
            },
            reconciliation = reconcile(snapshot.positions, localPositions),
        )
    }

    private fun reconcile(
        kisPositions: List<BrokerBalancePosition>,
        localPositions: List<Position>,
    ): KisReconciliationResult {
        val kisQuantities = kisPositions.associate { it.ticker to it.quantity }
        val localQuantities = localPositions.associate { (it.ticker ?: "") to it.quantity }

        return KisReconciliationResult(
            missingInLocal = kisQuantities.keys.filter { !localQuantities.containsKey(it) }.sorted(),
            missingInKis = localQuantities.keys.filter { it.isNotBlank() && !kisQuantities.containsKey(it) }.sorted(),
            quantityMismatch = kisQuantities
                .filter { (ticker, kisQuantity) ->
                    localQuantities[ticker]?.compareTo(kisQuantity)?.let { it != 0 } ?: false
                }
                .keys
                .sorted(),
        )
    }

    // KIS 어댑터는 KRW 한 줄을 항상 채움, 없으면 0 으로 메우지 않고 우리 쪽 결함으로 드러냄
    private fun krwBalance(snapshot: BrokerBalanceSnapshot): BrokerCurrencyBalance {
        val krw = snapshot.currencyBalances.singleOrNull { it.currency == KRW }
        return krw ?: throw snapshotInvalid("KRW 합계 행")
    }

    private fun BigDecimal?.orInvalid(field: String): BigDecimal = this ?: throw snapshotInvalid("KRW $field")

    private fun snapshotInvalid(what: String) = InternalErrorException(SNAPSHOT_INVALID, "KIS 스냅샷에 $what 값이 없습니다.")

    private companion object {
        const val KRW = "KRW"
        const val SNAPSHOT_INVALID = "KIS_BALANCE_SNAPSHOT_INVALID"
        val KST: ZoneId = ZoneId.of("Asia/Seoul")
    }
}
