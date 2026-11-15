package com.papertrading.api.infrastructure.security

import java.security.GeneralSecurityException
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * AES-256-GCM 토큰 암복호화, JDK javax.crypto 만 사용
 * 형식 v1.<IV>.<암호문+태그> (base64url, 패딩 없음), IV 12바이트는 암호화마다 무작위
 * AAD 는 호출자가 정한 문맥 문자열(Redis 키 이름), 다른 키 이름으로 옮겨 붙인 값은 복호화 실패
 */
class AesGcmTokenCipher(key: ByteArray) {
    private val secretKey: SecretKeySpec
    private val random = SecureRandom()

    init {
        require(key.size == KEY_BYTES) { "암호화 키는 ${KEY_BYTES}바이트여야 합니다." }
        secretKey = SecretKeySpec(key, "AES")
    }

    fun encrypt(plain: String, aad: String): String {
        val iv = ByteArray(IV_BYTES).also(random::nextBytes)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(TAG_BITS, iv))
        cipher.updateAAD(aad.toByteArray(Charsets.UTF_8))
        val body = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
        return listOf(VERSION, ENCODER.encodeToString(iv), ENCODER.encodeToString(body)).joinToString(SEPARATOR)
    }

    fun decrypt(payload: String, aad: String): String {
        val parts = payload.split(SEPARATOR)
        if (parts.size != PART_COUNT || parts[0] != VERSION) throw TokenCipherException("암호문 형식이 올바르지 않습니다.")
        return decryptParts(parts[1], parts[2], aad)
    }

    private fun decryptParts(ivPart: String, bodyPart: String, aad: String): String = try {
        val iv = DECODER.decode(ivPart)
        val body = DECODER.decode(bodyPart)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(TAG_BITS, iv))
        cipher.updateAAD(aad.toByteArray(Charsets.UTF_8))
        String(cipher.doFinal(body), Charsets.UTF_8)
    } catch (e: GeneralSecurityException) {
        throw TokenCipherException("복호화에 실패했습니다.", e)
    } catch (e: IllegalArgumentException) {
        throw TokenCipherException("암호문 형식이 올바르지 않습니다.", e)
    }

    companion object {
        private const val VERSION = "v1"
        private const val SEPARATOR = "."
        private const val PART_COUNT = 3
        private const val KEY_BYTES = 32
        private const val IV_BYTES = 12
        private const val TAG_BITS = 128
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private val ENCODER = Base64.getUrlEncoder().withoutPadding()
        private val DECODER = Base64.getUrlDecoder()

        // base64 키 문자열로 생성, 틀린 입력은 값을 싣지 않는 예외로 거부
        @Suppress("SwallowedException") // 원인 예외 문구에 키 문자가 들어가 일부러 버림
        fun fromBase64Key(base64Key: String): AesGcmTokenCipher {
            val bytes = try {
                Base64.getDecoder().decode(base64Key)
            } catch (e: IllegalArgumentException) {
                throw IllegalArgumentException("암호화 키가 base64 형식이 아닙니다.")
            }
            return AesGcmTokenCipher(bytes)
        }

        fun isValidBase64Key(base64Key: String): Boolean = runCatching { fromBase64Key(base64Key) }.isSuccess
    }
}
