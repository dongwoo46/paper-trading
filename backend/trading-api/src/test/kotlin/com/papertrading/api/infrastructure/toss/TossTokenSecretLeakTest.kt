package com.papertrading.api.infrastructure.toss

import com.papertrading.api.common.exception.TossTokenUnavailableException
import com.papertrading.api.infrastructure.redis.RedisDistributedLock
import com.papertrading.api.infrastructure.security.AesGcmTokenCipher
import com.papertrading.api.support.LogCapture
import com.papertrading.api.support.SharedRedisContainer
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.util.Base64
import java.util.concurrent.atomic.AtomicLong

// 토큰·client secret·암호화 키가 로그와 예외 문구에 없는지 확인 (RULE-05)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TossTokenSecretLeakTest {

    // 시험 전용 가짜 값, 로그에 나오면 안 되는 문자열
    private val fakeToken = "fake-access-token-1"
    private val fakeSecret = "fake-client-secret-LEAKCHECK"
    private val keyBytes = ByteArray(32) { (it + 5).toByte() }
    private val encryptionKey = Base64.getEncoder().encodeToString(keyBytes)
    private val cipher = AesGcmTokenCipher(keyBytes)

    private val factory = SharedRedisContainer.newConnectionFactory()
    private val redisTemplate = SharedRedisContainer.newTemplate(factory)
    private val lock = RedisDistributedLock(redisTemplate)

    @BeforeEach
    fun clean() {
        redisTemplate.delete(listOf(TossTokenManager.TOKEN_KEY, TossTokenManager.LOCK_KEY))
    }

    @AfterAll
    fun close() {
        factory.destroy()
    }

    private fun properties(lockWaitMillis: Long = 30_000) = TossProperties().apply {
        baseUrl = "https://toss.example.invalid"
        clientId = "fake-client-id"
        clientSecret = fakeSecret
        tokenEncryptionKey = encryptionKey
        tokenLockWaitMillis = lockWaitMillis
    }

    private fun manager(
        issuer: TossTokenIssuer,
        properties: TossProperties = properties(),
        nowMillis: () -> Long = System::currentTimeMillis,
        sleeper: (Long) -> Unit = Thread::sleep,
    ) = TossTokenManager(properties, redisTemplate, lock, issuer, nowMillis, sleeper)

    private fun assertNoSecrets(logs: LogCapture, extra: List<String> = emptyList()) {
        val forbidden = listOf(fakeToken, fakeSecret, encryptionKey) + extra
        val leaked = logs.lines().filter { line -> forbidden.any { line.contains(it) } }
        assertTrue(leaked.isEmpty(), "비밀값이 로그에 있음: ${leaked.size}건")
    }

    @Test
    fun `발급 캐시 적중 무효화 경로의 로그에 토큰 secret 키가 없다`() {
        LogCapture().use { logs ->
            val manager = manager(FakeTossTokenIssuer(expiresInSeconds = 3_600))

            val token = manager.getToken()
            manager.getToken()
            val rawCiphertext = requireNotNull(redisTemplate.opsForValue().get(TossTokenManager.TOKEN_KEY))
            manager.invalidate("다른-토큰")
            manager.invalidate(token)

            assertTrue(token == fakeToken)
            assertNoSecrets(logs, listOf(rawCiphertext))
        }
    }

    @Test
    fun `복호화 실패 경로의 로그에 옛 암호문과 새 토큰이 없다`() {
        val stale = AesGcmTokenCipher(ByteArray(32) { 9 }).encrypt("키-교체-전-토큰", TossTokenManager.TOKEN_KEY)
        redisTemplate.opsForValue().set(TossTokenManager.TOKEN_KEY, stale)

        LogCapture().use { logs ->
            manager(FakeTossTokenIssuer()).getToken()

            assertNoSecrets(logs, listOf(stale, "키-교체-전-토큰"))
        }
    }

    @Test
    fun `락 대기 초과와 수명 오류 경로의 로그와 예외 문구에 비밀값이 없다`() {
        val holder = requireNotNull(lock.tryAcquire(TossTokenManager.LOCK_KEY, 60_000))
        val clock = AtomicLong(0)
        LogCapture().use { logs ->
            val waitTimeout = assertThrows(TossTokenUnavailableException::class.java) {
                manager(
                    FakeTossTokenIssuer(),
                    properties(lockWaitMillis = 200),
                    nowMillis = clock::get,
                    sleeper = { clock.addAndGet(it) },
                ).getToken()
            }
            lock.release(TossTokenManager.LOCK_KEY, holder)
            val shortLived = assertThrows(TossTokenUnavailableException::class.java) {
                manager(FakeTossTokenIssuer(expiresInSeconds = 10)).getToken()
            }

            assertNoSecrets(logs)
            listOf(waitTimeout, shortLived).forEach { error ->
                listOf(fakeToken, fakeSecret, encryptionKey).forEach { secret ->
                    assertFalse(error.message.orEmpty().contains(secret))
                }
            }
        }
    }

    @Test
    fun `토큰이 이미 저장된 상태의 무효화와 발급기 실패 경로에도 비밀값이 로그에 없다`() {
        val failing = object : TossTokenIssuer {
            override fun issue(): TossIssuedToken = error("발급 실패")
        }
        val encrypted = cipher.encrypt(fakeToken, TossTokenManager.TOKEN_KEY)
        redisTemplate.opsForValue().set(TossTokenManager.TOKEN_KEY, encrypted)
        LogCapture().use { logs ->
            manager(failing).invalidate(fakeToken)
            assertThrows(IllegalStateException::class.java) { manager(failing).getToken() }

            assertNoSecrets(logs)
        }
    }
}
