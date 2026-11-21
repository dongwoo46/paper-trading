package com.papertrading.api.infrastructure.kis

import com.fasterxml.jackson.core.JacksonException
import com.fasterxml.jackson.databind.ObjectMapper
import com.papertrading.api.common.exception.KisAuthFailedException
import com.papertrading.api.common.exception.KisOrderRejectedException
import com.papertrading.api.common.exception.KisOrderResultUnknownException
import com.papertrading.api.common.exception.KisPermissionDeniedException
import com.papertrading.api.common.exception.KisRateLimitedException
import com.papertrading.api.common.exception.KisTimeoutException
import com.papertrading.api.common.exception.KisUnavailableException
import com.papertrading.api.common.exception.ServiceUnavailableException
import com.papertrading.api.infrastructure.client.TransportTimeouts
import org.springframework.web.client.HttpStatusCodeException
import org.springframework.web.client.ResourceAccessException
import org.springframework.web.client.RestClientException
import java.net.ConnectException
import java.net.UnknownHostException
import java.net.http.HttpConnectTimeoutException

/**
 * KIS 실패 분류 (D28), 잔고·주문 어댑터 공용, KIS 원문은 cause 에만
 * 한도 판정은 msg_cd EGW00201 또는 «초당 거래건수» 문구만, rt_cd 1 판정보다 앞 (S9-02)
 * «초과» 단독 문구는 주문가능금액 초과 같은 주문 거부와 겹쳐 한도로 보지 않음
 * HTTP 4xx·5xx 도 본문이 KIS JSON 이면 본문으로 분류 (step 11 리뷰 C-02)
 */
internal object KisFailureClassifier {
    private const val RATE_LIMIT_MSG_CD = "EGW00201"
    private const val RATE_LIMIT_PHRASE = "초당 거래건수"
    private val objectMapper = ObjectMapper()

    private data class KisErrorBody(val rtCd: String, val msgCd: String?, val msg1: String)

    fun isRateLimited(msgCd: String?, message: String?): Boolean {
        val byPhrase = message.orEmpty().contains(RATE_LIMIT_PHRASE)
        return msgCd == RATE_LIMIT_MSG_CD || byPhrase
    }

    // 잔고(읽기 전용) 전송 실패: KIS 본문 → 본문 분류, 원인 체인 타임아웃 → KIS_TIMEOUT, 나머지 KIS_UNAVAILABLE (S9-01)
    fun transportFailure(e: RestClientException): ServiceUnavailableException {
        val body = (e as? HttpStatusCodeException)?.let(::kisErrorBody)
        if (body != null) return balanceFailure(body.rtCd, body.msgCd, body.msg1)
        val timedOut = e is ResourceAccessException && TransportTimeouts.hasTimeoutCause(e)
        return if (timedOut) KisTimeoutException(e) else KisUnavailableException(e)
    }

    // 잔고 응답 실패: 한도 → 인증 → 권한 → 그 밖 순서
    fun balanceFailure(rtCd: String?, msgCd: String?, message: String): ServiceUnavailableException {
        val cause = KisApiException(mapKisErrorCode(rtCd, message), rtCd, message)
        return when {
            isRateLimited(msgCd, message) -> KisRateLimitedException(cause)
            rtCd == "1" || isAuthMessage(message) -> KisAuthFailedException(cause)
            rtCd == "7" || message.contains("권한") -> KisPermissionDeniedException(cause)
            else -> KisUnavailableException(cause)
        }
    }

    // 주문 호출 실패, 분류 밖 예외는 그대로
    fun orderFailure(e: Throwable): Throwable = when (e) {
        is KisApiException -> orderResponseFailure(e, msgCd = null)

        is HttpStatusCodeException -> orderHttpFailure(e)

        is ResourceAccessException -> orderTransportFailure(e)

        // 응답은 받았으나 변환 실패 등, 접수 여부를 모름
        is RestClientException -> KisOrderResultUnknownException(e)

        // 보수 분류: rt_cd 0 인데 ORNO 없음 외에 주문 전 토큰 발급 실패(requireNotNull)·빈 base URL 의 IllegalArgumentException 도 섞임
        is IllegalArgumentException -> KisOrderResultUnknownException(e)

        else -> e
    }

    // 4xx 는 KIS 본문이면 확정 실패 분류·본문 없으면 처리 전 거절, 5xx 는 처리 안 한 게 확실한 것만 확정
    private fun orderHttpFailure(e: HttpStatusCodeException): RuntimeException {
        val body = kisErrorBody(e)
        val serverError = e.statusCode.is5xxServerError
        return when {
            serverError -> orderServerErrorFailure(e, body)
            body == null -> KisUnavailableException(e)
            else -> orderResponseFailure(kisApiException(body), body.msgCd)
        }
    }

    // 5xx 본문은 한도·인증만 확정, 그 밖(EGW00001·EGW00002 서버 에러, 주문 거부 문구 포함)은 결과 미확인 (R1-02)
    private fun orderServerErrorFailure(e: HttpStatusCodeException, body: KisErrorBody?): RuntimeException = when {
        body == null -> KisOrderResultUnknownException(e)
        isRateLimited(body.msgCd, body.msg1) -> KisRateLimitedException(kisApiException(body))
        isAuthMessage(body.msg1) -> KisAuthFailedException(kisApiException(body))
        else -> KisOrderResultUnknownException(e)
    }

    private fun kisApiException(body: KisErrorBody): KisApiException {
        val code = mapKisErrorCode(body.rtCd, body.msg1)
        return KisApiException(code, body.rtCd, body.msg1)
    }

    // 요청을 못 보낸 게 확실한 원인(연결 거부·DNS·연결 타임아웃)만 재시도 가능, 나머지는 보내졌을 수 있어 결과 미확인
    private fun orderTransportFailure(e: ResourceAccessException): ServiceUnavailableException {
        val notSent = TransportTimeouts.causeChain(e).any {
            it is ConnectException || it is UnknownHostException || it is HttpConnectTimeoutException
        }
        return if (notSent) KisUnavailableException(e) else KisOrderResultUnknownException(e)
    }

    // 주문 응답 실패는 rt_cd 가 아니라 KisErrorCode 로 가름(주문 거부의 흔한 rt_cd 가 1)
    private fun orderResponseFailure(e: KisApiException, msgCd: String?): RuntimeException = when {
        // HTTP 200 인데 rt_cd 없음·본문 없음은 문구와 무관하게 접수 여부 모름 (사람 결정 2026-11-21)
        e.kisRtCode == null -> KisOrderResultUnknownException(e)

        isRateLimited(msgCd, e.kisMessage) -> KisRateLimitedException(e)

        else -> orderFailureByCode(e)
    }

    // rt_cd 가 있고 0 이 아닌 KIS JSON 본문만, 빈 본문·HTML·깨진 JSON 은 null
    private fun kisErrorBody(e: HttpStatusCodeException): KisErrorBody? {
        val map = try {
            objectMapper.readValue(e.responseBodyAsString, Map::class.java)
        } catch (_: JacksonException) {
            null
        }
        val rtCd = map?.get("rt_cd")?.toString()
        if (rtCd == null || rtCd == "0") return null
        return KisErrorBody(rtCd, map["msg_cd"]?.toString(), map["msg1"]?.toString().orEmpty())
    }
}

// 인증 판정은 잔고·주문 공용 문구만, 인증 msg_cd 목록은 실호출 대조 몫 (object 함수 수 한도로 파일 수준에 둠)
private fun isAuthMessage(message: String): Boolean {
    val byToken = message.contains("token", ignoreCase = true)
    return message.contains("인증") || byToken
}

// KisErrorCode 별 주문 실패 (object 함수 수 한도로 파일 수준에 둠)
private fun orderFailureByCode(e: KisApiException): RuntimeException = when (e.code) {
    KisErrorCode.KIS_ORDER_REJECTED,
    KisErrorCode.KIS_INVALID_ACCOUNT,
    KisErrorCode.KIS_DUPLICATE_OR_ALREADY_PROCESSED,
    -> KisOrderRejectedException(e)

    // mapKisErrorCode 가 «초과»·rate·limit 문구만으로 붙인 한도 code, 엄격 판정을 못 넘으면 주문 거부
    KisErrorCode.KIS_RATE_LIMITED -> KisOrderRejectedException(e)

    KisErrorCode.KIS_AUTH_FAILED -> KisAuthFailedException(e)

    KisErrorCode.KIS_TEMPORARY_UNAVAILABLE -> KisUnavailableException(e)
}
