package com.papertrading.api.application.account

import com.papertrading.api.application.account.port.BrokerAccountBalancePort
import com.papertrading.api.application.account.port.BrokerAccountRef
import com.papertrading.api.application.account.result.BrokerAccountBalanceResult
import com.papertrading.api.common.exception.BrokerBalanceNotSupportedException
import com.papertrading.api.common.exception.NotBrokerAccountException
import com.papertrading.api.common.exception.TossAccountNotLinkedException
import com.papertrading.api.domain.entity.account.Account
import com.papertrading.api.domain.enums.AccountSource
import com.papertrading.api.domain.enums.TradingMode
import org.springframework.stereotype.Service

/**
 * 계좌 모드로 브로커 잔고 어댑터를 골라 조회 (D25)
 * 어느 모드를 어댑터·501·404 로 보낼지는 전수 when 이 정하고(새 모드는 컴파일이 알림, RULE-03), 생성 때 그 결과와 구현 표를 맞춰 보아 어긋나면 기동 실패
 * 트랜잭션 없이 외부 호출만 함
 */
@Service
class BrokerAccountQueryService(
    private val accountQueryService: AccountQueryService,
    balancePorts: List<BrokerAccountBalancePort>,
) {
    private val portTable = BrokerBalancePortTable(balancePorts)

    init {
        val routedToAdapter = TradingMode.entries.filter { routeOf(it).usesAdapter }.toSet()
        val missing = routedToAdapter - portTable.modes
        val unrouted = portTable.modes - routedToAdapter
        check(missing.isEmpty()) { "어댑터로 보내는 모드에 잔고 포트 구현이 없습니다: ${missing.names()}" }
        check(unrouted.isEmpty()) { "어댑터로 보내지 않는 모드를 잔고 포트 구현이 지원합니다: ${unrouted.names()}" }
    }

    fun getBalance(accountId: Long): BrokerAccountBalanceResult {
        val account = accountQueryService.getAccount(accountId)
        val mode = account.tradingMode
        return when (routeOf(mode)) {
            Route.TOSS -> fetch(accountId, mode, AccountSource.TOSS, linkedAccountSeq(account.externalAccountId))
            Route.KIS -> fetch(accountId, mode, AccountSource.KIS, linkedKisAccountNumber(account.externalAccountId))
            Route.NOT_SUPPORTED -> throw BrokerBalanceNotSupportedException(mode.name)
            Route.NOT_BROKER_ACCOUNT -> throw NotBrokerAccountException(mode.name)
        }
    }

    private fun fetch(
        accountId: Long,
        mode: TradingMode,
        source: AccountSource,
        externalAccountId: String,
    ): BrokerAccountBalanceResult {
        val snapshot = portTable.portFor(mode).fetchBalance(BrokerAccountRef(accountId, mode, externalAccountId))
        return BrokerAccountBalanceResult(
            accountId = accountId,
            source = source,
            tradingMode = mode,
            asOf = snapshot.asOf,
            currencyBalances = snapshot.currencyBalances,
            positions = snapshot.positions,
        )
    }

    // 앞뒤 공백을 지운 뒤 숫자만(Long 범위)이면 accountSeq, 아니면 미연결
    private fun linkedAccountSeq(externalAccountId: String?): String {
        val seq = externalAccountId?.trim().orEmpty()
        val isAccountSeq = seq.isNotEmpty() && seq.all { it in '0'..'9' } && seq.toLongOrNull() != null
        if (!isAccountSeq) throw TossAccountNotLinkedException()
        return seq
    }

    private fun Set<TradingMode>.names(): String = joinToString { it.name }

    private enum class Route(val usesAdapter: Boolean) {
        TOSS(true),
        KIS(true),
        NOT_SUPPORTED(false),
        NOT_BROKER_ACCOUNT(false),
    }

    // 모드 분기의 유일한 자리, else 없이 전수로 둠
    private fun routeOf(mode: TradingMode): Route = when (mode) {
        TradingMode.TOSS_LIVE -> Route.TOSS
        TradingMode.KIS_PAPER, TradingMode.KIS_LIVE -> Route.KIS
        TradingMode.UPBIT_LIVE -> Route.NOT_SUPPORTED
        TradingMode.LOCAL -> Route.NOT_BROKER_ACCOUNT
    }
}
