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
}
