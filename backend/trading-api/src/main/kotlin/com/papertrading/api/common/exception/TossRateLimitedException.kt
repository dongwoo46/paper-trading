package com.papertrading.api.common.exception

/** J — 토스 호출 한도 초과(429)가 최대 재시도 횟수나 최대 대기 상한을 넘어 계속됨 */
class TossRateLimitedException(reason: String, cause: Throwable? = null) :
    ServiceUnavailableException(
        "TOSS_RATE_LIMITED",
        "토스 호출 한도를 넘었습니다. 사유=$reason",
        cause,
    )
