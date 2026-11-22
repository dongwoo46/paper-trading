package com.papertrading.api.application.toss.port

import com.papertrading.api.common.maskAllButLastFour

/** 토스 계좌 목록 조회 포트, 운영자가 accountSeq 를 확인하는 용도 (D3) */
interface TossAccountListPort {
    fun fetchAccounts(): List<TossAccountSummary>
}

/**
 * accountNo 는 끝 4자리만(전체 번호는 포트 밖으로 나오지 않음), accountSeq 는 전체 값, accountType 은 모르는 값도 문자열 그대로
 * toString 은 accountSeq 를 maskAllButLastFour 로만 보임, accountNoLast4 는 이미 끝 4자리라 그대로
 */
data class TossAccountSummary(
    val accountNoLast4: String,
    val accountSeq: Long,
    val accountType: String,
) {
    private val maskedAccountSeq: String get() = maskAllButLastFour(accountSeq.toString())

    override fun toString(): String = "TossAccountSummary(accountNoLast4=$accountNoLast4, " +
        "accountSeq=$maskedAccountSeq, accountType=$accountType)"
}
