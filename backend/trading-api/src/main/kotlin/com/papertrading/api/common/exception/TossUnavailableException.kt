package com.papertrading.api.common.exception

/** J — 토스를 쓸 수 없음 (점검이 아닌 5xx, 연결 거부 같은 네트워크 오류) */
class TossUnavailableException(reason: String, cause: Throwable? = null) :
    ServiceUnavailableException(
        "TOSS_UNAVAILABLE",
        "토스를 사용할 수 없습니다. 사유=$reason",
        cause,
    )
