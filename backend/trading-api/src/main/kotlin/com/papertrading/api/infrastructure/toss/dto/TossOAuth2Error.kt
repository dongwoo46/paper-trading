package com.papertrading.api.infrastructure.toss.dto

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * 토큰 발급 실패 본문, 공식 명세 OAuth2ErrorResponse (BFF 공통 봉투가 아닌 OAuth2 표준 형식)
 * error 는 문자열이고 모르는 값을 허용, 설명은 로그에도 쓰지 않음
 */
data class TossOAuth2Error(
    val error: String? = null,
    @JsonProperty("error_description") val errorDescription: String? = null,
)
