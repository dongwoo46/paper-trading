package com.papertrading.api.common.exception

/**
 * J — KIS 가 주문을 받았는지 알 수 없음 (요청 전송 뒤 읽기 타임아웃, 본문으로 분류 못 한 HTTP 5xx, rt_cd 0 인데 주문번호 없음)
 * 재시도 안내 대신 KIS 주문 내역 확인 안내(PTF3, step 11 리뷰 C-01), KIS 원문과 계좌번호는 cause 에만
 */
class KisOrderResultUnknownException(cause: Throwable? = null) :
    ServiceUnavailableException(
        "KIS_ORDER_RESULT_UNKNOWN",
        "KIS 접수 결과를 확인하지 못했습니다.",
        cause,
    )
