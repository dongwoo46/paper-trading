package com.papertrading.api.common.exception

/**
 * F-4 — 토스 계좌의 externalAccountId 가 비었거나 accountSeq(숫자)가 아니어서 토스 계좌와 연결되지 않음
 * 값은 싣지 않고 연결 방법만 안내
 */
class TossAccountNotLinkedException :
    ConflictException(
        "TOSS_ACCOUNT_NOT_LINKED",
        "토스 계좌와 연결되지 않았습니다. GET /api/toss/accounts 로 확인한 accountSeq 를 계좌의 externalAccountId 로 등록해 주세요.",
    )
