package com.papertrading.api.infrastructure.toss

import com.fasterxml.jackson.core.JsonProcessingException
import com.papertrading.api.common.exception.ServiceUnavailableException
import com.papertrading.api.common.exception.TossAuthFailedException
import com.papertrading.api.common.exception.TossIpNotAllowedException
import com.papertrading.api.common.exception.TossRateLimitedException
import com.papertrading.api.common.exception.TossResponseInvalidException
import com.papertrading.api.common.exception.TossUnavailableException
import com.papertrading.api.infrastructure.toss.dto.TossTokenResponse
import mu.KotlinLogging
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestClientResponseException
import org.springframework.web.client.RestTemplate
import java.net.URLEncoder

/**
 * POST /oauth2/token 로 토스 액세스 토큰을 발급 (Client Credentials, form 요청, 응답은 봉투 없는 최상위 JSON)
 * 재시도·대기를 하지 않음: 발급 최악 소요(연결+읽기 타임아웃)를 락 TTL 과 요청 전체 상한(R1-02)의 근거로 쓰기 때문
 * 오류는 OAuth2 형식(`error` 문자열)이고 분류는 development-plan D15 와 open-questions FLAG-18
 */
@Component
class TossTokenIssuerImpl(
    private val properties: TossProperties,
    private val restTemplate: RestTemplate,
    private val rateLimiter: TossRateLimiter,
) : TossTokenIssuer {
    private val log = KotlinLogging.logger {}

    override fun issue(): TossIssuedToken {
        properties.requireEnabled()
        rateLimiter.acquire(TossRateLimitGroup.AUTH)
        log.info { "토스 토큰 발급 요청" }
        val body = post()
        return toIssuedToken(parse(body))
    }

    // 본문을 바이트로 보냄: RestTemplate 이 form 객체를 DEBUG 로 그대로 찍어 client_secret 이 로그에 남는 경로를 막음
    private fun post(): String {
        val headers = HttpHeaders().apply {
            contentType = MediaType.APPLICATION_FORM_URLENCODED
            accept = listOf(MediaType.APPLICATION_JSON)
        }
        val form = formBody().toByteArray(Charsets.UTF_8)
        try {
            val response = restTemplate.exchange(
                "${properties.baseUrl.trimEnd('/')}/oauth2/token",
                HttpMethod.POST,
                HttpEntity(form, headers),
                String::class.java,
            )
            return response.body.orEmpty()
        } catch (e: RestClientResponseException) {
            throw errorFor(e)
        } catch (e: RestClientException) {
            log.warn { "토스 토큰 발급 전송 실패: ${e.javaClass.simpleName}" }
            throw TossTransportSupport.transportFailure(e)
        }
    }

    private fun formBody(): String = listOf(
        "grant_type" to GRANT_TYPE,
        "client_id" to properties.clientId,
        "client_secret" to properties.clientSecret,
    ).joinToString("&") { (name, value) -> "$name=${URLEncoder.encode(value, Charsets.UTF_8)}" }

    private fun errorFor(e: RestClientResponseException): ServiceUnavailableException {
        val status = e.statusCode.value()
        val error = TossJson.readOAuth2Error(e.responseBodyAsString)?.error
        val code = TossTransportSupport.safeCode(error)
        log.warn { "토스 토큰 발급 거절: status=$status error=$code" }
        return when {
            status == HttpStatus.FORBIDDEN.value() && error == ERROR_ACCESS_DENIED -> ipNotAllowed()
            status == HttpStatus.TOO_MANY_REQUESTS.value() -> TossRateLimitedException("토큰 발급 429")
            status >= HttpStatus.INTERNAL_SERVER_ERROR.value() -> TossUnavailableException("토큰 발급 서버 오류 $status")
            else -> TossAuthFailedException("토큰 발급 거절 $status $code")
        }
    }

    private fun ipNotAllowed() = TossIpNotAllowedException("토큰 발급 403 access_denied")

    private fun parse(body: String): TossTokenResponse {
        try {
            return TossJson.readValue(body, TossTokenResponse::class.java)
                ?: throw TossResponseInvalidException("토큰 응답 본문이 null")
        } catch (e: JsonProcessingException) {
            log.warn { "토스 토큰 응답 해석 실패: ${TossJson.reasonOf(e)}" }
            throw TossResponseInvalidException("토큰 응답 ${TossJson.reasonOf(e)}")
        }
    }

    private fun toIssuedToken(response: TossTokenResponse): TossIssuedToken {
        val invalidReason = when {
            response.accessToken.isBlank() -> "access_token 이 비어 있음"
            !response.tokenType.equals(TOKEN_TYPE, ignoreCase = true) -> "token_type 이 $TOKEN_TYPE 가 아님"
            response.expiresIn <= 0 -> "expires_in 이 양수가 아님"
            else -> null
        }
        if (invalidReason != null) throw TossResponseInvalidException("토큰 응답 $invalidReason")
        return TossIssuedToken(response.accessToken, response.expiresIn)
    }

    companion object {
        private const val GRANT_TYPE = "client_credentials"
        private const val TOKEN_TYPE = "Bearer"
        private const val ERROR_ACCESS_DENIED = "access_denied"
    }
}
