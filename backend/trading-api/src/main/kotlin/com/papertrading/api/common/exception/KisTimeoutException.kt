package com.papertrading.api.common.exception

/**
 * J — KIS 호출의 연결·읽기 타임아웃, 상대가 처리를 끝냈는지는 알 수 없음
 * KIS 원문과 계좌번호는 cause 에만 두고 메시지에 싣지 않음
 */
class KisTimeoutException(cause: Throwable? = null) :
    ServiceUnavailableException(
        "KIS_TIMEOUT",
        "KIS 응답 시간이 초과됐습니다.",
        cause,
    )
