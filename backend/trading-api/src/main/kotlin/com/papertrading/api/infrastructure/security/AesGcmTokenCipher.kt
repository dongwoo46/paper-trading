package com.papertrading.api.infrastructure.security

import java.security.GeneralSecurityException
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * AES-256-GCM 토큰 암복호화, JDK javax.crypto 만 사용
 * 형식 v2.<키 식별자>.<IV>.<암호문+태그> (base64url, 패딩 없음), IV 12바이트는 암호화마다 무작위
 * 키 식별자는 키 SHA-256 앞 8바이트, 저장은 늘 현재 키이고 복호화는 식별자가 맞는 현재·이전 키로 (키 교체 중 공존)
 * AAD 는 호출자가 정한 문맥 문자열(Redis 키 이름), 다른 키 이름으로 옮겨 붙인 값은 복호화 실패
 */
class AesGcmTokenCipher(key: ByteArray, previousKey: ByteArray? = null) {
    private val currentKeyId = keyIdOf(key)
    private val currentKey = secretKeyOf(key)
    private val keysById: Map<String, SecretKeySpec> = buildMap {
        previousKey?.let { put(keyIdOf(it), secretKeyOf(it)) }
        put(currentKeyId, currentKey)
    }
    private val random = SecureRandom()

    fun encrypt(plain: String, aad: String): String {
        val iv = ByteArray(IV_BYTES).also(random::nextBytes)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, currentKey, GCMParameterSpec(TAG_BITS, iv))
        cipher.updateAAD(aad.toByteArray(Charsets.UTF_8))
        val body = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
        return listOf(VERSION, currentKeyId, ENCODER.encodeToString(iv), ENCODER.encodeToString(body))
            .joinToString(SEPARATOR)
    }

    fun decrypt(payload: String, aad: String): String {
        val parts = payload.split(SEPARATOR)
        if (parts.size != PART_COUNT || parts[0] != VERSION) throw TokenCipherException("암호문 형식이 올바르지 않습니다.")
        val key = keysById[parts[KEY_ID_INDEX]] ?: throw TokenCipherException("암호문의 키 식별자를 알 수 없습니다.")
        return decryptParts(key, parts[IV_INDEX], parts[BODY_INDEX], aad)
    }

    private fun decryptParts(key: SecretKeySpec, ivPart: String, bodyPart: String, aad: String): String = try {
        val iv = DECODER.decode(ivPart)
        val body = DECODER.decode(bodyPart)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_BITS, iv))
        cipher.updateAAD(aad.toByteArray(Charsets.UTF_8))
        String(cipher.doFinal(body), Charsets.UTF_8)
    } catch (e: GeneralSecurityException) {
        throw TokenCipherException("복호화에 실패했습니다.", e)
    } catch (e: IllegalArgumentException) {
        throw TokenCipherException("암호문 형식이 올바르지 않습니다.", e)
    }

    companion object {
        private const val VERSION = "v2"
        private const val SEPARATOR = "."
        private const val PART_COUNT = 4
        private const val KEY_ID_INDEX = 1
        private const val IV_INDEX = 2
        private const val BODY_INDEX = 3
        private const val KEY_BYTES = 32
        private const val KEY_ID_BYTES = 8
        private const val IV_BYTES = 12
        private const val TAG_BITS = 128
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private val ENCODER = Base64.getUrlEncoder().withoutPadding()
        private val DECODER = Base64.getUrlDecoder()

        private fun secretKeyOf(key: ByteArray): SecretKeySpec {
            require(key.size == KEY_BYTES) { "암호화 키는 ${KEY_BYTES}바이트여야 합니다." }
            return SecretKeySpec(key, "AES")
        }

        private fun keyIdOf(key: ByteArray): String {
            val digest = MessageDigest.getInstance("SHA-256").digest(key)
            return ENCODER.encodeToString(digest.copyOf(KEY_ID_BYTES))
        }

        // base64 키 문자열로 생성(이전 키는 선택), 틀린 입력은 값을 싣지 않는 예외로 거부
        fun fromBase64Key(base64Key: String, previousBase64Key: String? = null): AesGcmTokenCipher {
            val previous = previousBase64Key?.let(::decodeBase64)
            return AesGcmTokenCipher(decodeBase64(base64Key), previous)
        }

        @Suppress("SwallowedException") // 원인 예외 문구에 키 문자가 들어가 일부러 버림
        private fun decodeBase64(base64Key: String): ByteArray = try {
            Base64.getDecoder().decode(base64Key)
        } catch (e: IllegalArgumentException) {
            throw IllegalArgumentException("암호화 키가 base64 형식이 아닙니다.")
        }

        fun isValidBase64Key(base64Key: String): Boolean = runCatching { fromBase64Key(base64Key) }.isSuccess
    }
}
