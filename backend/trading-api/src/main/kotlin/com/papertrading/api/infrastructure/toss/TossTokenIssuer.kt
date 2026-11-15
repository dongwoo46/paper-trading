package com.papertrading.api.infrastructure.toss

// 토스 토큰 발급 호출, 실제 HTTP 구현은 스키마 확정 뒤 step 5
fun interface TossTokenIssuer {
    fun issue(): TossIssuedToken
}
