package com.papertrading.api.infrastructure.security

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.Base64

class AesGcmTokenCipherTest {

    // 시험 전용 가짜 키, 32바이트
    private val keyBytes = ByteArray(32) { (it + 1).toByte() }
    private val otherKeyBytes = ByteArray(32) { (it + 101).toByte() }
    private val cipher = AesGcmTokenCipher(keyBytes)
    private val aad = "toss:token"
    private val plain = "fake-access-token-0001"

    @Test
    fun `암호화한 값을 같은 키와 AAD 로 복호화하면 원문과 같다`() {
        assertEquals(plain, cipher.decrypt(cipher.encrypt(plain, aad), aad))
    }

    @Test
    fun `같은 평문을 두 번 암호화하면 암호문이 다르다`() {
        assertNotEquals(cipher.encrypt(plain, aad), cipher.encrypt(plain, aad))
    }

    @Test
    fun `형식은 v1 점 IV 점 암호문과 태그이고 base64url 이며 평문을 담지 않는다`() {
        val payload = cipher.encrypt(plain, aad)
        val parts = payload.split(".")

        assertEquals(3, parts.size)
        assertEquals("v1", parts[0])
        assertEquals(12, Base64.getUrlDecoder().decode(parts[1]).size)
        // 암호문 길이 = 평문 길이 + GCM 태그 16바이트
        assertEquals(plain.toByteArray().size + 16, Base64.getUrlDecoder().decode(parts[2]).size)
        assertFalse(payload.contains(plain))
        assertFalse(payload.contains("="), "패딩 없는 base64url")
    }

    @Test
    fun `암호문이 변조되면 복호화에 실패한다`() {
        val parts = cipher.encrypt(plain, aad).split(".")
        val body = Base64.getUrlDecoder().decode(parts[2])
        body[0] = (body[0].toInt() xor 1).toByte()
        val tampered = "${parts[0]}.${parts[1]}.${Base64.getUrlEncoder().withoutPadding().encodeToString(body)}"

        assertThrows(TokenCipherException::class.java) { cipher.decrypt(tampered, aad) }
    }

    @Test
    fun `다른 키로는 복호화에 실패한다`() {
        val payload = cipher.encrypt(plain, aad)

        assertThrows(TokenCipherException::class.java) { AesGcmTokenCipher(otherKeyBytes).decrypt(payload, aad) }
    }

    @Test
    fun `다른 AAD 로는 복호화에 실패한다`() {
        val payload = cipher.encrypt(plain, aad)

        assertThrows(TokenCipherException::class.java) { cipher.decrypt(payload, "kis:token:live") }
    }

    @Test
    fun `형식이 틀린 입력은 복호화에 실패한다`() {
        listOf("", "v1", "v1.abc", "v2.AAAA.AAAA", "v1.AAAA.AAAA.AAAA", "v1.!!!.@@@", "plain-token").forEach {
            assertThrows(TokenCipherException::class.java, { cipher.decrypt(it, aad) }, "입력=$it")
        }
    }

    @Test
    fun `키 길이가 32바이트가 아니면 거부한다`() {
        listOf(0, 16, 24, 31, 33, 64).forEach { size ->
            assertThrows(IllegalArgumentException::class.java, { AesGcmTokenCipher(ByteArray(size)) }, "길이=$size")
        }
    }

    @Test
    fun `base64 키로 만들 수 있고 틀린 base64 나 길이는 키 값 없이 거부한다`() {
        val encoded = Base64.getEncoder().encodeToString(keyBytes)
        assertEquals(plain, AesGcmTokenCipher.fromBase64Key(encoded).decrypt(cipher.encrypt(plain, aad), aad))
        assertTrue(AesGcmTokenCipher.isValidBase64Key(encoded))

        val shortKey = Base64.getEncoder().encodeToString(ByteArray(16))
        listOf("not-base64-@@@", shortKey, "").forEach { bad ->
            assertFalse(AesGcmTokenCipher.isValidBase64Key(bad), "입력=$bad")
            val error = assertThrows(IllegalArgumentException::class.java) { AesGcmTokenCipher.fromBase64Key(bad) }
            assertFalse(bad.isNotEmpty() && error.message.orEmpty().contains(bad), "예외 메시지에 키 값 없음")
        }
    }
}
