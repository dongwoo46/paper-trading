package com.papertrading.api.infrastructure.toss.dto

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * POST /oauth2/token 성공 응답, 공식 명세 1.2.28 OAuth2TokenResponse (봉투 없이 최상위)
 * 세 필드 모두 필수, expires_in 은 만료까지 남은 초(int64)
 * toString 에 토큰을 싣지 않음
 */
data class TossTokenResponse(
    @JsonProperty("access_token") val accessToken: String,
    @JsonProperty("token_type") val tokenType: String,
    @JsonProperty("expires_in") val expiresIn: Long,
) {
    override fun toString(): String = "TossTokenResponse(tokenType=$tokenType, expiresIn=$expiresIn)"
}
