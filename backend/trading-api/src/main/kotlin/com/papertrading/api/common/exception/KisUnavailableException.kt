package com.papertrading.api.common.exception

/**
 * J — KIS 를 쓸 수 없음 (인증·권한·한도가 아닌 응답 오류, 연결 실패 같은 전송 오류)
 * KIS 원문과 계좌번호는 cause 에만 두고 메시지에 싣지 않음
 */
class KisUnavailableException(cause: Throwable? = null) :
    ServiceUnavailableException(
        "KIS_UNAVAILABLE",
        "KIS 를 사용할 수 없습니다.",
        cause,
    )
