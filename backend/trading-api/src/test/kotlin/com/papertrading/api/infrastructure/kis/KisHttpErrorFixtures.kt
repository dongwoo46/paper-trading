package com.papertrading.api.infrastructure.kis

import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatusCode
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.client.HttpStatusCodeException
import java.nio.charset.StandardCharsets

// RestTemplate 기본 오류 처리기가 던지는 모양의 HTTP 오류 예외 (본문 포함)
internal fun httpError(status: Int, body: String): HttpStatusCodeException {
    val code = HttpStatusCode.valueOf(status)
    val bytes = body.toByteArray(StandardCharsets.UTF_8)
    return if (code.is5xxServerError) {
        HttpServerErrorException.create(code, "error", HttpHeaders(), bytes, StandardCharsets.UTF_8)
    } else {
        HttpClientErrorException.create(code, "error", HttpHeaders(), bytes, StandardCharsets.UTF_8)
    }
}

// KIS 오류 본문 JSON
internal fun kisBody(rtCd: String, msgCd: String, msg1: String): String {
    val head = """{"rt_cd":"$rtCd","msg_cd":"$msgCd","""
    return head + """"msg1":"$msg1"}"""
}
