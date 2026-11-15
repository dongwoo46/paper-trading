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
 * 계좌 모드로 브로커 잔고 어댑터를 골라 조회 (D8)
 * 모드를 전부 명시해 새 모드가 생기면 컴파일이 누락을 알림 (RULE-03), 트랜잭션 없이 외부 호출만 함
 */
@Service
class BrokerAccountQueryService(
    private val accountQueryService: AccountQueryService,
    // 파라미터 이름이 토스 어댑터 빈 이름과 같아, 2단계에 구현이 늘어도 이름으로 가려짐
    private val tossAccountBalanceAdapter: BrokerAccountBalancePort,
) {
    fun getBalance(accountId: Long): BrokerAccountBalanceResult {
        val account = accountQueryService.getAccount(accountId)
        return when (val mode = account.tradingMode) {
            TradingMode.TOSS_LIVE -> fetchToss(accountId, account)

            TradingMode.KIS_PAPER, TradingMode.KIS_LIVE, TradingMode.UPBIT_LIVE ->
                throw BrokerBalanceNotSupportedException(mode.name)

            TradingMode.LOCAL -> throw NotBrokerAccountException(mode.name)
        }
    }

    private fun fetchToss(accountId: Long, account: Account): BrokerAccountBalanceResult {
        val accountSeq = linkedAccountSeq(account.externalAccountId)
        val snapshot = tossAccountBalanceAdapter.fetchBalance(
            BrokerAccountRef(accountId, TradingMode.TOSS_LIVE, accountSeq),
        )
        return BrokerAccountBalanceResult(
            accountId = accountId,
            source = AccountSource.TOSS,
            tradingMode = TradingMode.TOSS_LIVE,
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
}
