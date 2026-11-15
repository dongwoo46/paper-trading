package com.papertrading.api.infrastructure.toss

import com.fasterxml.jackson.core.type.TypeReference
import com.papertrading.api.application.toss.port.TossAccountListPort
import com.papertrading.api.application.toss.port.TossAccountSummary
import com.papertrading.api.infrastructure.toss.dto.TossAccount
import org.springframework.stereotype.Component

/**
 * 토스 계좌 목록 어댑터, GET /api/v1/accounts (ACCOUNT 그룹, 계좌 헤더 없음)
 * 응답의 계좌번호는 끝 4자리만 남기고 전체 번호는 이 클래스 밖으로 내보내지 않음 (사람 결정 2026-11-15, 필드표 부록 A)
 */
@Component
class TossAccountListClient(
    private val client: TossApiClient,
) : TossAccountListPort {

    override fun fetchAccounts(): List<TossAccountSummary> {
        val accounts = client.get(ACCOUNTS_REQUEST, ACCOUNTS_TYPE)
        return accounts.map(::toSummary)
    }

    private fun toSummary(account: TossAccount) = TossAccountSummary(
        accountNoLast4 = account.accountNo.takeLast(VISIBLE_TAIL_LENGTH),
        accountSeq = account.accountSeq,
        accountType = account.accountType,
    )

    companion object {
        private const val VISIBLE_TAIL_LENGTH = 4
        private val ACCOUNTS_REQUEST = TossApiRequest("/api/v1/accounts", TossRateLimitGroup.ACCOUNT)
        private val ACCOUNTS_TYPE = object : TypeReference<List<TossAccount>>() {}
    }
}
