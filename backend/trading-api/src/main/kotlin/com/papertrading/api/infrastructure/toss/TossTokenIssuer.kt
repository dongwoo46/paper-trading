package com.papertrading.api.infrastructure.toss

// 토스 토큰 발급 호출, 실제 HTTP 구현은 TossTokenIssuerImpl(시험은 가짜 발급기)
fun interface TossTokenIssuer {
    fun issue(): TossIssuedToken
}
