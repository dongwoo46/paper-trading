package com.papertrading.api.infrastructure.security

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.security.MessageDigest
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

    // devkit:test-change D10 C-08 — 암호문 형식에 키 식별자가 들어가 v1.<IV>.<본문> 이 v2.<키식별자>.<IV>.<본문> 이 됨
    @Test
    fun `형식은 v2 점 키 식별자 점 IV 점 암호문과 태그이고 base64url 이며 평문을 담지 않는다`() {
        val payload = cipher.encrypt(plain, aad)
        val parts = payload.split(".")

        assertEquals(4, parts.size)
        assertEquals("v2", parts[0])
        // 키 식별자 = 키 SHA-256 앞 8바이트의 base64url
        val expectedKeyId = MessageDigest.getInstance("SHA-256").digest(keyBytes).copyOf(8)
        assertEquals(Base64.getUrlEncoder().withoutPadding().encodeToString(expectedKeyId), parts[1])
        assertEquals(12, Base64.getUrlDecoder().decode(parts[2]).size)
        // 암호문 길이 = 평문 길이 + GCM 태그 16바이트
        assertEquals(plain.toByteArray().size + 16, Base64.getUrlDecoder().decode(parts[3]).size)
        assertFalse(payload.contains(plain))
        assertFalse(payload.contains("="), "패딩 없는 base64url")
    }

    @Test
    fun `암호문이 변조되면 복호화에 실패한다`() {
        val parts = cipher.encrypt(plain, aad).split(".")
        val body = Base64.getUrlDecoder().decode(parts[3])
        body[0] = (body[0].toInt() xor 1).toByte()
        val bodyPart = Base64.getUrlEncoder().withoutPadding().encodeToString(body)
        val tampered = "${parts[0]}.${parts[1]}.${parts[2]}.$bodyPart"

        assertThrows(TokenCipherException::class.java) { cipher.decrypt(tampered, aad) }
    }

    @Test
    fun `이전 키를 주면 그 키 식별자의 암호문은 이전 키로 복호화하고 저장은 늘 현재 키로 한다`() {
        val oldCipher = AesGcmTokenCipher(otherKeyBytes)
        val rotating = AesGcmTokenCipher(keyBytes, otherKeyBytes)
        val oldPayload = oldCipher.encrypt(plain, aad)

        assertEquals(plain, rotating.decrypt(oldPayload, aad), "이전 키 암호문")
        assertEquals(plain, rotating.decrypt(cipher.encrypt(plain, aad), aad), "현재 키 암호문")
        val written = rotating.encrypt(plain, aad)
        assertEquals(cipher.encrypt(plain, aad).split(".")[1], written.split(".")[1], "저장은 현재 키 식별자")
        assertEquals(plain, cipher.decrypt(written, aad), "현재 키만 가진 쪽도 읽음")
        assertThrows(TokenCipherException::class.java) { oldCipher.decrypt(written, aad) }
    }

    @Test
    fun `이전 키가 없거나 식별자가 어느 키와도 다르면 복호화에 실패한다`() {
        val oldPayload = AesGcmTokenCipher(otherKeyBytes).encrypt(plain, aad)
        val thirdKey = ByteArray(32) { (it + 51).toByte() }

        assertThrows(TokenCipherException::class.java) { cipher.decrypt(oldPayload, aad) }
        val unrelated = AesGcmTokenCipher(keyBytes, thirdKey)
        assertThrows(TokenCipherException::class.java) { unrelated.decrypt(oldPayload, aad) }
    }

    @Test
    fun `옛 v1 3부 형식은 복호화에 실패해 캐시 없음으로 다뤄진다`() {
        val iv = Base64.getUrlEncoder().withoutPadding().encodeToString(ByteArray(12))
        val body = Base64.getUrlEncoder().withoutPadding().encodeToString(ByteArray(32))

        assertThrows(TokenCipherException::class.java) { cipher.decrypt("v1.$iv.$body", aad) }
    }

    @Test
    fun `이전 키 길이가 32바이트가 아니면 거부한다`() {
        assertThrows(IllegalArgumentException::class.java) { AesGcmTokenCipher(keyBytes, ByteArray(16)) }
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
        listOf(
            "", "v2", "v2.abc", "v2.AAAA.AAAA", "v2.AAAA.AAAA.AAAA", "v3.AAAA.AAAA.AAAA.AAAA",
            "v2.!!!.@@@.###", "v1.AAAA.AAAA.AAAA.AAAA", "plain-token",
        ).forEach {
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
    fun `base64 키 둘로 만들면 이전 키 암호문도 읽고 틀린 이전 키는 값 없이 거부한다`() {
        val encoder = Base64.getEncoder()
        val current = encoder.encodeToString(keyBytes)
        val rotating = AesGcmTokenCipher.fromBase64Key(current, encoder.encodeToString(otherKeyBytes))

        assertEquals(plain, rotating.decrypt(AesGcmTokenCipher(otherKeyBytes).encrypt(plain, aad), aad))
        listOf("not-base64-@@@", encoder.encodeToString(ByteArray(16))).forEach { bad ->
            val error = assertThrows(IllegalArgumentException::class.java) {
                AesGcmTokenCipher.fromBase64Key(encoder.encodeToString(keyBytes), bad)
            }
            assertFalse(error.message.orEmpty().contains(bad), "예외 메시지에 키 값 없음")
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
