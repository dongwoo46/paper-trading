package com.papertrading.api.common.exception

/**
 * A-1 — 시세 조회 입력(종목, 개수, 봉 단위, 시각)이 우리 쪽 검증을 넘지 못함
 * 어느 입력이 어떤 규칙을 어겼는지만 싣고 입력값 자체는 문구에 싣지 않음
 */
class InvalidMarketQueryException(field: String, rule: String) :
    BadRequestException(
        "INVALID_MARKET_QUERY",
        "시세 조회 입력이 올바르지 않습니다. 입력=$field, 규칙=$rule",
    )
