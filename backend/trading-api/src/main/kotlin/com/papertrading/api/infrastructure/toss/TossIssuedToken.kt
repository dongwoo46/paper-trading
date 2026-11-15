package com.papertrading.api.infrastructure.toss

// 발급기가 돌려주는 토큰과 수명(초), toString 에 토큰을 싣지 않음
class TossIssuedToken(val accessToken: String, val expiresInSeconds: Long) {
    override fun toString(): String = "TossIssuedToken(expiresInSeconds=$expiresInSeconds)"
}
