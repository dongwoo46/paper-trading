package com.papertrading.api.infrastructure.toss

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.core.type.TypeReference
import com.papertrading.api.infrastructure.toss.dto.TossTokenResponse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.math.BigDecimal

// 토스 본문 역직렬화의 엄격성: 결측·절삭이 조용히 통과하지 않음 (D14, C-03)
class TossJsonTest {

    data class Item(val symbol: String, val price: BigDecimal)

    private val itemsType = TossJson.javaType(object : TypeReference<List<Item>>() {})

    @Test
    fun `목록 원소가 null 이면 실패한다`() {
        assertThrows<JsonProcessingException> { TossJson.readSuccess<List<Item>>("""{"result":[null]}""", itemsType) }
        assertThrows<JsonProcessingException> {
            TossJson.readSuccess<List<Item>>("""{"result":[{"symbol":"A","price":"1"},null]}""", itemsType)
        }
    }

    @Test
    fun `정상 목록과 빈 목록은 그대로 읽는다`() {
        val body = """{"result":[{"symbol":"A","price":"1.50"}]}"""

        val items = TossJson.readSuccess<List<Item>>(body, itemsType)?.result

        assertEquals(listOf(Item("A", BigDecimal("1.50"))), items)
        assertEquals(emptyList<Item>(), TossJson.readSuccess<List<Item>>("""{"result":[]}""", itemsType)?.result)
    }

    @ParameterizedTest(name = "expires_in {0} 은 정수가 아니라 실패한다")
    @ValueSource(strings = ["3600.9", "0.4", "3600.0", "1e3"])
    fun `실수는 정수 필드에서 절삭하지 않고 실패한다`(expiresIn: String) {
        val body = """{"access_token":"t","token_type":"Bearer","expires_in":$expiresIn}"""

        assertThrows<JsonProcessingException> { TossJson.readValue(body, TossTokenResponse::class.java) }
    }

    @Test
    fun `정수 expires_in 은 그대로 읽는다`() {
        val body = """{"access_token":"t","token_type":"Bearer","expires_in":3600}"""

        assertEquals(3600L, TossJson.readValue(body, TossTokenResponse::class.java)?.expiresIn)
    }

    // 명세의 배열에는 null 원소가 없어, 원소 타입이 null 을 허용해도 받지 않음
    @Test
    fun `원소 타입이 null 을 허용해도 목록 원소 null 은 실패한다`() {
        val nullableType = TossJson.javaType(object : TypeReference<List<String?>>() {})

        assertThrows<JsonProcessingException> {
            TossJson.readSuccess<List<String?>>("""{"result":[null,"a"]}""", nullableType)
        }
    }

    @Test
    fun `목록 안의 객체가 가진 목록 원소 null 도 실패한다`() {
        val body = """{"result":[{"symbol":"A","price":"1","tags":[null]}]}"""
        val type = TossJson.javaType(object : TypeReference<List<Tagged>>() {})

        assertThrows<JsonProcessingException> { TossJson.readSuccess<List<Tagged>>(body, type) }
    }

    data class Tagged(val symbol: String, val price: BigDecimal, val tags: List<String>)

    // ---- S6-01: 빈·공백 문자열이 스칼라 칸에서 조용히 null 이 되지 않음, 명시적 JSON null 만 null ----

    data class Amounts(val krw: BigDecimal, val usd: BigDecimal? = null)

    data class Counts(val total: Long, val seen: Long? = null)

    data class Flags(val on: Boolean, val maybe: Boolean? = null)

    data class Named(val name: String, val note: String? = null)

    private fun <T : Any> read(body: String, type: Class<T>): T? {
        val envelope = TossJson.readSuccess<T>(body, TossJson.javaType(type))
        return envelope?.result
    }

    // JSON 문자열 안에 들어갈 값이라 탭·줄바꿈은 이스케이프로 적음
    @ParameterizedTest(name = "금액 칸의 문자열 [{0}] 은 실패한다")
    @ValueSource(strings = ["", " ", "   ", "\\t", "\\n", "\\u00a0"])
    fun `null 허용 금액 칸의 빈 문자열과 공백 문자열은 null 이 되지 않고 실패한다`(blank: String) {
        assertThrows<JsonProcessingException> { read("""{"result":{"krw":"1","usd":"$blank"}}""", Amounts::class.java) }
    }

    @ParameterizedTest(name = "필수 금액 칸의 문자열 [{0}] 은 실패한다")
    @ValueSource(strings = ["", " ", "\\t"])
    fun `null 불가 금액 칸의 빈 문자열과 공백 문자열도 실패한다`(blank: String) {
        assertThrows<JsonProcessingException> { read("""{"result":{"krw":"$blank"}}""", Amounts::class.java) }
    }

    @Test
    fun `금액 칸은 명시적 JSON null 이나 생략일 때만 null 이다`() {
        assertEquals(Amounts(BigDecimal("1"), null), read("""{"result":{"krw":"1","usd":null}}""", Amounts::class.java))
        assertEquals(Amounts(BigDecimal("1"), null), read("""{"result":{"krw":"1"}}""", Amounts::class.java))
        assertEquals(
            Amounts(BigDecimal("1"), BigDecimal("2.50")),
            read("""{"result":{"krw":"1","usd":"2.50"}}""", Amounts::class.java),
        )
    }

    @ParameterizedTest(name = "정수 칸의 문자열 [{0}] 은 실패한다")
    @ValueSource(strings = ["", " ", "\\t"])
    fun `정수 칸의 빈 문자열과 공백 문자열은 0 이나 null 이 되지 않고 실패한다`(blank: String) {
        assertThrows<JsonProcessingException> { read("""{"result":{"total":"$blank"}}""", Counts::class.java) }
        assertThrows<JsonProcessingException> { read("""{"result":{"total":1,"seen":"$blank"}}""", Counts::class.java) }
    }

    @ParameterizedTest(name = "불린 칸의 문자열 [{0}] 은 실패한다")
    @ValueSource(strings = ["", " ", "\\t"])
    fun `불린 칸의 빈 문자열과 공백 문자열은 false 나 null 이 되지 않고 실패한다`(blank: String) {
        assertThrows<JsonProcessingException> { read("""{"result":{"on":"$blank"}}""", Flags::class.java) }
        assertThrows<JsonProcessingException> { read("""{"result":{"on":true,"maybe":"$blank"}}""", Flags::class.java) }
    }

    // ---- S7-02: 문자열 토큰 null 글자도 금액 칸에서 조용히 null 이 되지 않음 ----

    // JSON 문자열 안에 들어갈 값이라 탭·줄바꿈은 이스케이프로 적음
    @ParameterizedTest(name = "금액 칸의 문자열 [{0}] 은 null 이 되지 않고 실패한다")
    @ValueSource(strings = ["null", " null ", "\\tnull\\n", "  null"])
    fun `null 허용 금액 칸의 null 글자 문자열은 null 이 되지 않고 실패한다`(text: String) {
        assertThrows<JsonProcessingException> { read("""{"result":{"krw":"1","usd":"$text"}}""", Amounts::class.java) }
    }

    @Test
    fun `null 불가 금액 칸의 null 글자 문자열도 실패한다`() {
        assertThrows<JsonProcessingException> { read("""{"result":{"krw":"null"}}""", Amounts::class.java) }
        assertThrows<JsonProcessingException> { read("""{"result":{"krw":" null "}}""", Amounts::class.java) }
    }

    @Test
    fun `금액 칸의 정상 숫자 문자열 JSON 숫자 JSON null 은 기존대로 읽는다`() {
        assertEquals(
            Amounts(BigDecimal("1"), BigDecimal("2.50")),
            read("""{"result":{"krw":"1","usd":"2.50"}}""", Amounts::class.java),
        )
        assertEquals(
            Amounts(BigDecimal("1.5"), BigDecimal("0.10")),
            read("""{"result":{"krw":1.5,"usd":0.10}}""", Amounts::class.java),
        )
        assertEquals(Amounts(BigDecimal("1"), null), read("""{"result":{"krw":"1","usd":null}}""", Amounts::class.java))
    }

    @Test
    fun `문자열 칸의 null 글자는 그대로 읽는다 숫자 칸만 막는다`() {
        assertEquals(Named("null", "null"), read("""{"result":{"name":"null","note":"null"}}""", Named::class.java))
    }

    @Test
    fun `문자열 칸의 빈 문자열은 그대로 읽는다 오류 본문의 빈 message 가 해석을 깨지 않도록`() {
        assertEquals(Named("", ""), read("""{"result":{"name":"","note":""}}""", Named::class.java))
    }
}
