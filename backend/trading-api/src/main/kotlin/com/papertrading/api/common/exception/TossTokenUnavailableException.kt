package com.papertrading.api.common.exception

/**
 * J — 토스 액세스 토큰을 얻지 못함 (락 대기 한도 초과, 토큰 수명 이상)
 * 사유 문구는 우리가 정한 고정 문구만, 토스 원문과 비밀값은 싣지 않음
 */
class TossTokenUnavailableException(reason: String) :
    ServiceUnavailableException(
        "TOSS_TOKEN_UNAVAILABLE",
        "토스 액세스 토큰을 사용할 수 없습니다. 사유=$reason",
    )
