package com.papertrading.api.common.exception

/**
 * A-1 — KIS 가 주문을 받지 않음 (주문 거부, 계좌 오류, 중복·기처리 주문)
 * 기존 400 기반(동결 기반 상속, FLAG-12), KIS 원문과 계좌번호는 cause 에만 두고 메시지에 싣지 않음
 */
class KisOrderRejectedException(cause: Throwable? = null) :
    BadRequestException(
        "KIS_ORDER_REJECTED",
        "KIS가 주문을 받지 않았습니다.",
        cause,
    )
