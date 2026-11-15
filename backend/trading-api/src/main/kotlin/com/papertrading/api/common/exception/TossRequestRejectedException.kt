package com.papertrading.api.common.exception

/**
 * A-1 — 토스가 우리가 보낸 입력(종목 등)을 거부한 4xx
 * 기존 400 기반(동결 기반 상속, FLAG-12), 토스 code 와 원문은 응답에 싣지 않고 로그에만 남김
 */
class TossRequestRejectedException(httpStatus: Int) :
    BadRequestException(
        "TOSS_REQUEST_REJECTED",
        "토스가 요청을 거부했습니다. 토스 상태=$httpStatus",
    )
