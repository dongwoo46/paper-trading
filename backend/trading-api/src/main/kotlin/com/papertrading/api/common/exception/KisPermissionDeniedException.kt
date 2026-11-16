package com.papertrading.api.common.exception

/**
 * J — KIS 가 조회 권한 없음으로 거절
 * KIS 원문과 계좌번호는 cause 에만 두고 메시지에 싣지 않음
 */
class KisPermissionDeniedException(cause: Throwable? = null) :
    ServiceUnavailableException(
        "KIS_PERMISSION_DENIED",
        "KIS 조회 권한이 없습니다.",
        cause,
    )
