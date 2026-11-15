package com.papertrading.api.infrastructure.toss

import com.fasterxml.jackson.annotation.JsonSetter
import com.fasterxml.jackson.annotation.Nulls
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JavaType
import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.module.kotlin.KotlinFeature
import com.fasterxml.jackson.module.kotlin.kotlinModule
import com.papertrading.api.infrastructure.toss.dto.TossErrorDetail
import com.papertrading.api.infrastructure.toss.dto.TossErrorEnvelope
import com.papertrading.api.infrastructure.toss.dto.TossOAuth2Error
import com.papertrading.api.infrastructure.toss.dto.TossSuccessEnvelope

/**
 * 토스 본문 역직렬화, 문자열 본문을 data class 로 직접 읽음 (D13)
 * Map·JsonNode 로 읽지 않으므로 JSON 소수가 Double 을 거치지 않고, BigDecimal 필드는 문자열 숫자도 정확히 받음
 * 모르는 필드는 무시(명세가 값 확장을 허용), 필수 필드(null 불가 필드) 누락은 JsonMappingException
 */
internal object TossJson {
    private val mapper: JsonMapper = JsonMapper.builder()
        // 목록·맵 원소 null 이 null 불가 원소 타입에 조용히 들어가지 않게 실패시킴 (D14, C-03)
        .addModule(kotlinModule { enable(KotlinFeature.NewStrictNullChecks) })
        // 봉투 result 처럼 제네릭 타입이 지워진 목록은 코틀린 검사가 못 보므로 원소 null 을 매퍼 전체에서 막음, 명세 배열에는 null 원소가 없음
        .defaultSetterInfo(JsonSetter.Value.forContentNulls(Nulls.FAIL))
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
        // null·빈 값이 원시형(Long·Int) 필드에서 조용히 0 이 되지 않게 실패시킴 (D14)
        .enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
        // 3600.9 가 3600 으로 절삭되어 정수 필드(Long·Int)에 들어가지 않게 실패시킴 (D14, C-03)
        .disable(DeserializationFeature.ACCEPT_FLOAT_AS_INT)
        .build()

    fun javaType(type: Class<*>): JavaType = mapper.constructType(type)

    fun javaType(type: TypeReference<*>): JavaType = mapper.constructType(type.type)

    // 본문이 JSON null 이면 null, 호출 쪽이 응답 불일치로 처리
    fun <T : Any> readValue(body: String, type: Class<T>): T? = mapper.readValue(body, type)

    fun <T> readSuccess(body: String, resultType: JavaType): TossSuccessEnvelope<T>? {
        val envelopeType = mapper.typeFactory.constructParametricType(TossSuccessEnvelope::class.java, resultType)
        return mapper.readValue(body, envelopeType)
    }

    // 오류 본문은 분류를 돕는 용도라 읽지 못하면 null, 실패를 새로 만들지 않음
    fun readErrorDetail(body: String?): TossErrorDetail? = readOrNull(body, TossErrorEnvelope::class.java)?.error

    fun readOAuth2Error(body: String?): TossOAuth2Error? = readOrNull(body, TossOAuth2Error::class.java)

    private fun <T> readOrNull(body: String?, type: Class<T>): T? {
        if (body.isNullOrBlank()) return null
        return try {
            mapper.readValue(body, type)
        } catch (_: JsonProcessingException) {
            null
        }
    }

    // 응답 값이 든 Jackson 메시지 대신 필드 경로(이름만)로 사유를 만듦
    fun reasonOf(e: JsonProcessingException): String = when (e) {
        is JsonMappingException -> "필드 경로=${e.path.joinToString(".") { it.fieldName ?: "[${it.index}]" }}"
        else -> "JSON 구문 오류"
    }
}
