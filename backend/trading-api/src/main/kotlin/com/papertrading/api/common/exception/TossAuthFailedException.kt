package com.papertrading.api.common.exception

/**
 * J — 토스 인증 실패 (자격증명 오류, 재발급 뒤에도 401, 권한 부족)
 * 사유는 우리가 정한 문구와 상태 코드·토스 code 만, 토스 원문과 비밀값은 싣지 않음
 */
class TossAuthFailedException(reason: String, cause: Throwable? = null) :
    ServiceUnavailableException(
        "TOSS_AUTH_FAILED",
        "토스 인증에 실패했습니다. 사유=$reason",
        cause,
    )
