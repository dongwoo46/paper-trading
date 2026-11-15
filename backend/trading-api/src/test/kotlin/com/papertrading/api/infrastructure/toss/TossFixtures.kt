package com.papertrading.api.infrastructure.toss

import org.springframework.http.HttpStatus
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.test.web.client.response.DefaultResponseCreator
import org.springframework.test.web.client.response.MockRestResponseCreators.withStatus

// 시험 전용, src/test/resources/fixtures/toss/ 의 가짜 응답 파일을 읽고 MockRestServiceServer 응답으로 만듦
object TossFixtures {
    fun read(name: String): String {
        val stream = requireNotNull(TossFixtures::class.java.getResourceAsStream("/fixtures/toss/$name")) {
            "fixture 없음: $name"
        }
        return stream.use { it.readBytes().toString(Charsets.UTF_8).trim() }
    }

    // fixture 파일 내용을 JSON 본문으로 돌려주는 응답
    fun reply(status: HttpStatus, fixture: String): DefaultResponseCreator = replyRaw(status, read(fixture))

    // 본문 문자열을 JSON 으로 돌려주는 응답
    fun replyRaw(status: HttpStatus, json: String) = withStatus(status).contentType(APPLICATION_JSON).body(json)

    // 429 응답, Retry-After 는 값이 있을 때만 실음
    fun tooManyRequests(retryAfter: String? = null): DefaultResponseCreator {
        val reply = reply(HttpStatus.TOO_MANY_REQUESTS, "error-rate-limit-exceeded.json")
        return if (retryAfter == null) reply else reply.header("Retry-After", retryAfter)
    }
}
