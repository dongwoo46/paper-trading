package com.papertrading.api.common.exception

/**
 * E-1 — 브로커 잔고 URL 의 대상 계좌가 브로커 계좌가 아님 (LOCAL 모의 계좌)
 * 거래 모드 문자열 인자 — common 이 domain 을 가리키는 패키지 순환 방지
 */
class NotBrokerAccountException(tradingMode: String) :
    NotFoundException(
        "NOT_BROKER_ACCOUNT",
        "브로커 계좌가 아니어서 브로커 잔고가 없습니다. tradingMode=$tradingMode",
    )
