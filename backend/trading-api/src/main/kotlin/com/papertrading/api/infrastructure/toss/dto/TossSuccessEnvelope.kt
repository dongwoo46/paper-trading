package com.papertrading.api.infrastructure.toss.dto

/** 토큰 외 API 성공 봉투 `{result: ...}`, 공식 명세 ApiResponse. result 가 없거나 null 이면 호출 쪽이 응답 불일치로 처리 */
data class TossSuccessEnvelope<T>(val result: T? = null)
