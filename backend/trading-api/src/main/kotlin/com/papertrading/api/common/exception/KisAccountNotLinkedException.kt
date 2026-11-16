package com.papertrading.api.common.exception

/**
 * F-4 — KIS 계좌의 externalAccountId 가 비었거나 XXXXXXXX-XX 형식이 아니어서 KIS 계좌와 연결되지 않음
 * 값은 싣지 않고 연결 방법만 안내
 */
class KisAccountNotLinkedException :
    ConflictException(
        "KIS_ACCOUNT_NOT_LINKED",
        "KIS 계좌와 연결되지 않았습니다. 계좌의 externalAccountId 를 XXXXXXXX-XX 형식으로 등록해 주세요.",
    )
