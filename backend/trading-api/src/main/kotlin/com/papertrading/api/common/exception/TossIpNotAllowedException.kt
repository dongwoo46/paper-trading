package com.papertrading.api.common.exception

/**
 * J — 토스에 허용 IP 로 등록되지 않은 곳에서 호출 (재시도하지 않음, 사람이 토스 설정에서 IP 등록)
 * 응답 문구는 허용 IP 등록 안내 고정 문구(GlobalExceptionHandler)
 */
class TossIpNotAllowedException(reason: String, cause: Throwable? = null) :
    ServiceUnavailableException(
        "TOSS_IP_NOT_ALLOWED",
        "토스 허용 IP 가 아닙니다. 사유=$reason",
        cause,
    )
