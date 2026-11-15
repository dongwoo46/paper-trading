package com.papertrading.api.common.exception

/** J — 토스 호출의 연결·읽기 타임아웃, 상대가 처리를 끝냈는지는 알 수 없음 */
class TossTimeoutException(reason: String, cause: Throwable? = null) :
    ServiceUnavailableException(
        "TOSS_TIMEOUT",
        "토스 응답 시간이 초과됐습니다. 사유=$reason",
        cause,
    )
