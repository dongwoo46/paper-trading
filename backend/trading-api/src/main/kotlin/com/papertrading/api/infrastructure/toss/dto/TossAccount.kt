package com.papertrading.api.infrastructure.toss.dto

import com.papertrading.api.common.maskAllButLastFour

/**
 * GET /api/v1/accounts 의 result 원소, 공식 명세 1.2.28 Account, accountType 은 모르는 값을 허용(문자열)
 * toString 에 계좌번호를 싣지 않고 accountSeq 는 maskAllButLastFour 로만 보임
 */
data class TossAccount(val accountNo: String, val accountSeq: Long, val accountType: String) {
    private val maskedAccountSeq: String get() = maskAllButLastFour(accountSeq.toString())

    override fun toString(): String = "TossAccount(accountSeq=$maskedAccountSeq, accountType=$accountType)"
}
