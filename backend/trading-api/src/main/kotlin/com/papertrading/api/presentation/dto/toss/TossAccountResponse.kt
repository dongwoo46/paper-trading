package com.papertrading.api.presentation.dto.toss

import com.papertrading.api.application.toss.port.TossAccountSummary

/** 토스 계좌 목록 응답, accountNo 는 끝 4자리만, accountSeq 는 전체 값을 문자열로(계좌의 externalAccountId 문자열에 그대로 넣는 값) */
data class TossAccountResponse(
    val accountNoLast4: String,
    val accountSeq: String,
    val accountType: String,
) {
    companion object {
        fun from(summary: TossAccountSummary) = TossAccountResponse(
            accountNoLast4 = summary.accountNoLast4,
            accountSeq = summary.accountSeq.toString(),
            accountType = summary.accountType,
        )
    }
}
