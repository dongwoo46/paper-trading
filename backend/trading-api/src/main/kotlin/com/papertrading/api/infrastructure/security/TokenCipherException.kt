package com.papertrading.api.infrastructure.security

// 토큰 복호화 실패(변조·다른 키·다른 AAD·형식 오류), 메시지에 입력값을 싣지 않음
class TokenCipherException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
