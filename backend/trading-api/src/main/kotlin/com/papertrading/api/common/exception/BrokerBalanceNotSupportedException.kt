package com.papertrading.api.common.exception

/**
 * I-2 — 브로커 잔고 공통 조회 연동이 아직 없는 거래 모드의 잔고 조회 (KIS_PAPER, KIS_LIVE, UPBIT_LIVE)
 * 거래 모드 문자열 인자 — common 이 domain 을 가리키는 패키지 순환 방지
 */
class BrokerBalanceNotSupportedException(tradingMode: String) :
    NotImplementedException(
        "BROKER_BALANCE_NOT_SUPPORTED",
        "해당 거래 모드의 브로커 잔고 조회는 아직 지원하지 않습니다. tradingMode=$tradingMode",
    )
