package com.papertrading.api.infrastructure.toss

import com.papertrading.api.common.maskAllButLastFour

/**
 * 토스 REST 조회 요청 하나, 계좌 API 면 accountSeq 를 넣으면 X-Tossinvest-Account 헤더로 실림
 * 한도 그룹은 호출하는 쪽이 엔드포인트에 맞춰 지정(공식 명세의 Rate Limits Group)
 * toString 은 accountSeq 를 maskAllButLastFour 로만 보임
 */
data class TossApiRequest(
    val path: String,
    val group: TossRateLimitGroup,
    val query: Map<String, String> = emptyMap(),
    val accountSeq: String? = null,
) {
    private val maskedAccountSeq: String get() = maskAllButLastFour(accountSeq)

    override fun toString(): String = "TossApiRequest(path=$path, group=$group, accountSeq=$maskedAccountSeq)"
}
