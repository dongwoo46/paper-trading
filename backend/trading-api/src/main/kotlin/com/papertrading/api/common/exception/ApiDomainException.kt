package com.papertrading.api.common.exception

import org.springframework.http.HttpStatus

open class ApiDomainException(
    val errorCode: String,
    val status: HttpStatus,
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)

