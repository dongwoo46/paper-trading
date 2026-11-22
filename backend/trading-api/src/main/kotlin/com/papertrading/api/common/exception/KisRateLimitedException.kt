package com.papertrading.api.common.exception

/**
 * J — KIS 호출 한도 초과
 * KIS 원문과 계좌번호는 cause 에만 두고 메시지에 싣지 않음
 */
class KisRateLimitedException(cause: Throwable? = null) :
    ServiceUnavailableException(
        "KIS_RATE_LIMITED",
        "KIS 호출 한도를 넘었습니다.",
        cause,
    )
