package com.papertrading.api.common.exception

/**
 * K-1 — 토스 필수 설정이 비어 있거나 형식이 틀려 토스 연동이 꺼져 있음
 * 메시지에는 누락·오류 설정의 환경변수 이름만, 값은 싣지 않음
 */
class TossIntegrationDisabledException(keyNames: List<String>) :
    InternalErrorException(
        "TOSS_INTEGRATION_DISABLED",
        "토스 연동이 비활성입니다. 누락·오류 설정=${keyNames.joinToString(", ")}",
    )
