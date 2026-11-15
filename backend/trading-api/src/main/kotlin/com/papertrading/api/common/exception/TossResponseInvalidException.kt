package com.papertrading.api.common.exception

/**
 * J-2 — 토스 응답을 해석하지 못함 (깨진 본문, 필수 필드 누락, 숫자 변환 실패)
 * 0 같은 대체값으로 채우지 않고 실패, 사유에 응답 값은 싣지 않음
 */
class TossResponseInvalidException(reason: String, cause: Throwable? = null) :
    ServiceUnavailableException(
        "TOSS_RESPONSE_INVALID",
        "토스 응답을 해석하지 못했습니다. 사유=$reason",
        cause,
    )
