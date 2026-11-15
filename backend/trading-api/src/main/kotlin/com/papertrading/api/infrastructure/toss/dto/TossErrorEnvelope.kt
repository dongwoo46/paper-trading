package com.papertrading.api.infrastructure.toss.dto

/**
 * 토큰 외 API 실패 봉투 `{error:{requestId, code, message, data}}`, 공식 명세 ErrorResponse·ApiError
 * 전부 선택 필드로 받아 어떤 본문이어도 분류를 막지 않음, code 는 모르는 값을 허용
 */
data class TossErrorEnvelope(val error: TossErrorDetail? = null)

data class TossErrorDetail(
    val requestId: String? = null,
    val code: String? = null,
    val message: String? = null,
    val data: TossErrorData? = null,
)

// 에러 해결 힌트 중 이 계층이 쓰는 것만, 점검(maintenance)의 재시도 권장 초
data class TossErrorData(val retryAfterSeconds: Long? = null)
