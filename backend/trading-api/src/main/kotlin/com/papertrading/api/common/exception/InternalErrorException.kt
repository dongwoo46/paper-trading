package com.papertrading.api.common.exception

/**
 * K — 우리 쪽 결함 (K-2 저장 데이터 어긋남, K-3 예상 밖 결함)
 * 범주·code·메시지만 보유, HTTP 상태(500) 매핑은 presentation 의 GlobalExceptionHandler
 */
open class InternalErrorException(
    val errorCode: String,
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)
