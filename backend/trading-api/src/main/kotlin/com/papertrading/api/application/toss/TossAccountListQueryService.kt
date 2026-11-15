package com.papertrading.api.application.toss

import com.papertrading.api.application.toss.port.TossAccountListPort
import com.papertrading.api.application.toss.port.TossAccountSummary
import org.springframework.stereotype.Service

/** 토스 계좌 목록 조회 유스케이스, 외부 호출만 하므로 트랜잭션 없음 */
@Service
class TossAccountListQueryService(
    private val tossAccountListPort: TossAccountListPort,
) {
    fun listAccounts(): List<TossAccountSummary> = tossAccountListPort.fetchAccounts()
}
