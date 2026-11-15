package com.papertrading.api.infrastructure.toss

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.JavaType
import com.papertrading.api.common.exception.TossResponseInvalidException
import com.papertrading.api.infrastructure.toss.dto.TossErrorDetail
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
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

/**
 * 토스 REST 조회 공통 전송 (Authorization Bearer, 계좌 헤더, 한도, 401 재시도, 429 대기, 실패 분류)
 * 성공 봉투 `{result: ...}` 의 result 를 호출 쪽이 정한 data class 로 돌려줌 (D13, 잔고·시세 DTO 는 step 6~7)
 * 실패는 D15 의 code 로 분류(TossFailureClassifier), 토스 원문은 응답·예외 문구에 싣지 않고 code·requestId 만 로그에 남김
 * 모든 재시도(401, 429)는 새 한도 슬롯을 쓰며, 재시도 횟수와 대기 합계에 상한이 있음 (D12, D16, RISK-08)
 */
@Component
class TossApiClient(
    private val properties: TossProperties,
    private val tokenManager: TossTokenManager,
    private val rateLimiter: TossRateLimiter,
    private val restTemplate: RestTemplate,
    private val sleeper: (Long) -> Unit = Thread::sleep,
) {
    private val log = KotlinLogging.logger {}

    fun <T : Any> get(request: TossApiRequest, type: Class<T>): T = execute(request, TossJson.javaType(type))

    fun <T : Any> get(request: TossApiRequest, type: TypeReference<T>): T = execute(request, TossJson.javaType(type))

    // 응답 하나의 상태·헤더·본문, 성공과 오류를 한 모양으로 다루려고 묶음
    private class Reply(val status: Int, val headers: HttpHeaders?, val body: String?) {
        val errorDetail: TossErrorDetail? by lazy { TossJson.readErrorDetail(body) }
    }

    // 요청 하나의 재시도 이력, 재시도에 걸쳐 이어지는 상태
    private class Call(val request: TossApiRequest) {
        var authRetried = false
        var rateRetries = 0
        var rateWaitedMillis = 0L
    }

    private fun <T : Any> execute(request: TossApiRequest, type: JavaType): T {
        properties.requireEnabled()
        val uri = uriOf(request)
        val call = Call(request)
        var token = tokenManager.getToken()
        while (true) {
            rateLimiter.acquire(request.group)
            val reply = send(uri, call, token)
            if (reply.status in SUCCESS_RANGE) return parseResult(reply.body, type, request)
            token = nextToken(reply, call, token) ?: throw failureFor(reply, call)
        }
    }

    private fun uriOf(request: TossApiRequest): URI {
        val builder = UriComponentsBuilder.fromUriString(properties.baseUrl.trimEnd('/')).path(request.path)
        request.query.forEach { (name, value) -> builder.queryParam(name, value) }
        return builder.build().encode().toUri()
    }

    private fun send(uri: URI, call: Call, token: String): Reply {
        val headers = HttpHeaders().apply {
            setBearerAuth(token)
            accept = listOf(MediaType.APPLICATION_JSON)
            call.request.accountSeq?.let { set(ACCOUNT_HEADER, it) }
        }
        try {
            val response = restTemplate.exchange(uri, HttpMethod.GET, HttpEntity<Void>(headers), String::class.java)
            return Reply(response.statusCode.value(), response.headers, response.body)
        } catch (e: RestClientResponseException) {
            return Reply(e.statusCode.value(), e.responseHeaders, e.responseBodyAsString)
        } catch (e: RestClientException) {
            log.warn { "토스 전송 실패: ${call.request} ${e.javaClass.simpleName}" }
            throw TossTransportSupport.transportFailure(e)
        }
    }

    // 재시도할 수 있으면 다음 시도에 쓸 토큰, 재시도하지 않으면 null (호출 쪽이 실패로 던짐)
    private fun nextToken(reply: Reply, call: Call, token: String): String? = when (reply.status) {
        HttpStatus.UNAUTHORIZED.value() -> refreshedTokenOrNull(reply, call, token)
        HttpStatus.TOO_MANY_REQUESTS.value() -> if (waitForRateLimit(reply, call)) token else null
        else -> null
    }

    // 토큰 문제로 보이는 401 만 1회, 다른 곳이 먼저 새로 저장했으면 invalidate 가 지우지 않고 getToken 이 그 토큰을 읽음 (D12)
    private fun refreshedTokenOrNull(reply: Reply, call: Call, token: String): String? {
        val code = reply.errorDetail?.code
        if (call.authRetried || code !in RETRYABLE_UNAUTHORIZED_CODES) return null
        call.authRetried = true
        log.warn { "토스 401 토큰 갱신 후 재시도: ${call.request} code=${TossTransportSupport.safeCode(code)}" }
        tokenManager.invalidate(token)
        return tokenManager.getToken()
    }

    // Retry-After(없으면 1초)만큼 잠, 재시도 횟수나 대기 합계 상한을 넘기면 잠들지 않고 false
    private fun waitForRateLimit(reply: Reply, call: Call): Boolean {
        val seconds = TossTransportSupport.retryAfterSeconds(reply.headers) ?: DEFAULT_RETRY_AFTER_SECONDS
        // 터무니없이 큰 값이 곱셈에서 넘쳐 음수가 되어 상한 검사를 통과하지 않도록 먼저 자름
        val waitMillis = seconds.coerceAtMost(MAX_RETRY_AFTER_SECONDS) * MILLIS_PER_SECOND
        val withinLimit = call.rateRetries < properties.rateLimitMaxRetries &&
            waitMillis <= properties.rateLimitMaxWaitMillis - call.rateWaitedMillis
        if (!withinLimit) return false
        call.rateRetries++
        call.rateWaitedMillis += waitMillis
        log.warn { "토스 429 대기 후 재시도: ${call.request} waitMillis=$waitMillis retry=${call.rateRetries}" }
        if (waitMillis > 0) sleeper(waitMillis)
        return true
    }

    // 재시도하지 않을 실패를 D15 code 로 분류하고, 토스 code 와 requestId 만 로그에 남김
    private fun failureFor(reply: Reply, call: Call): RuntimeException {
        val detail = reply.errorDetail
        val code = TossTransportSupport.safeCode(detail?.code)
        log.warn {
            "토스 오류 응답: ${call.request} status=${reply.status} code=$code " +
                "requestId=${TossTransportSupport.safeCode(detail?.requestId)} " +
                "retryAfterSeconds=${detail?.data?.retryAfterSeconds}"
        }
        val retries = TossFailureClassifier.Retries(call.authRetried, call.rateRetries, call.rateWaitedMillis)
        return TossFailureClassifier.classify(reply.status, code, retries)
    }

    // result 를 data class 로 꺼냄, 깨진 본문과 필수 필드 누락·숫자 변환 실패는 0 으로 채우지 않고 실패 (D14)
    private fun <T : Any> parseResult(body: String?, type: JavaType, request: TossApiRequest): T {
        try {
            val envelope = TossJson.readSuccess<T>(body.orEmpty(), type)
            return envelope?.result ?: throw TossResponseInvalidException("result 가 없음")
        } catch (e: JsonProcessingException) {
            val reason = TossJson.reasonOf(e)
            log.warn { "토스 응답 해석 실패: $request $reason" }
            throw TossResponseInvalidException(reason)
        }
    }

    companion object {
        const val ACCOUNT_HEADER = "X-Tossinvest-Account"
        private const val DEFAULT_RETRY_AFTER_SECONDS = 1L
        private const val MAX_RETRY_AFTER_SECONDS = 86_400L
        private const val MILLIS_PER_SECOND = 1_000L
        private val RETRYABLE_UNAUTHORIZED_CODES = setOf("invalid-token", "expired-token", "token-revoked")
        private val SUCCESS_RANGE = 200..299
    }
}
