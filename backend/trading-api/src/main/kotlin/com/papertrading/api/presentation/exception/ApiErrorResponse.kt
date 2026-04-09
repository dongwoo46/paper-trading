package com.papertrading.api.presentation.exception

data class ApiErrorResponse(
    val status: Int,
    val code: String,
    val message: String
)