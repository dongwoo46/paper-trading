package com.papertrading.api.common.exception

/**
 * I-1 — 브로커 주문 연동이 아직 없는 거래 모드의 주문 (TOSS_LIVE, UPBIT_LIVE)
 * 거래 모드 문자열 인자 — common 이 domain 을 가리키는 패키지 순환 방지
 */
class BrokerOrderNotSupportedException(tradingMode: String) :
    NotImplementedException(
        "BROKER_ORDER_NOT_SUPPORTED",
        "해당 거래 모드의 주문은 아직 지원하지 않습니다. tradingMode=$tradingMode",
    )
