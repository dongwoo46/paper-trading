package com.papertrading.api.infrastructure.toss

import com.papertrading.api.common.exception.TossTokenUnavailableException
import com.papertrading.api.infrastructure.redis.RedisDistributedLock
import com.papertrading.api.infrastructure.security.AesGcmTokenCipher
import com.papertrading.api.infrastructure.security.TokenCipherException
import mu.KotlinLogging
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.core.script.DefaultRedisScript
import java.time.Duration

/**
 * 토스 액세스 토큰 캐시·발급 직렬화·무효화 (토큰은 클라이언트당 1개만 유효 — RULE-06)
 * 순서: 캐시 확인 → 락 획득 → 캐시 재확인 → 발급 → 암호화 저장 → 락 해제, 락 없이 발급하는 경로 없음
 * Redis 에는 AES-GCM 암호문만 저장(RULE-05), 토큰·암호화 키·client secret 은 로그에 남기지 않음
 * 스프링 빈 등록은 실제 발급기(TossTokenIssuer 구현)가 생기는 step 5
 */
class TossTokenManager(
    private val properties: TossProperties,
    private val redisTemplate: StringRedisTemplate,
    private val lock: RedisDistributedLock,
    private val issuer: TossTokenIssuer,
    // 시계와 대기는 주입, 시험이 실제로 오래 잠들지 않게
    private val nowMillis: () -> Long = System::currentTimeMillis,
    private val sleeper: (Long) -> Unit = Thread::sleep,
) {
    private val log = KotlinLogging.logger {}

    // 비활성 설정에서는 만들어지지 않도록 첫 사용 때 만듦
    private val cipher: AesGcmTokenCipher by lazy { AesGcmTokenCipher.fromBase64Key(properties.tokenEncryptionKey) }

    fun getToken(): String {
        properties.requireEnabled()
        return readCached() ?: issueUnderLock()
    }

    // Redis 값이 이 토큰일 때만 삭제, 다른 쪽이 새로 저장한 토큰은 지우지 않음 (D12)
    fun invalidate(token: String): Boolean {
        properties.requireEnabled()
        val raw = redisTemplate.opsForValue().get(TOKEN_KEY) ?: return false
        // 암호문은 IV 가 매번 달라 평문 비교가 안 되므로 읽은 암호문 그대로를 비교해 삭제
        return decryptOrNull(raw) == token &&
            redisTemplate.execute(DELETE_IF_EQUALS_SCRIPT, listOf(TOKEN_KEY), raw) == 1L
    }

    private fun issueUnderLock(): String {
        val owner = lock.tryAcquire(LOCK_KEY, properties.tokenLockTtlMillis) ?: return waitForToken()
        return try {
            readCached() ?: issueAndStore()
        } finally {
            lock.release(LOCK_KEY, owner)
        }
    }

    private fun readCached(): String? = redisTemplate.opsForValue().get(TOKEN_KEY)?.let(::decryptOrNull)

    // 복호화 실패(키 교체·형식 깨짐)는 캐시 없음으로 보고 재발급으로 복구
    private fun decryptOrNull(raw: String): String? = try {
        cipher.decrypt(raw, TOKEN_KEY)
    } catch (e: TokenCipherException) {
        log.warn { "토스 토큰 캐시 복호화 실패, 캐시 없음으로 처리: reason=${e.message}" }
        null
    }

    private fun issueAndStore(): String {
        val issued = issuer.issue()
        val ttlSeconds = issued.expiresInSeconds - properties.tokenRefreshSkewSeconds
        if (ttlSeconds <= 0) {
            log.warn { "토스 토큰 수명이 갱신 여유 이하라 저장하지 않음: expiresIn=${issued.expiresInSeconds}s" }
            throw TossTokenUnavailableException("토큰 수명(expires_in)이 갱신 여유 이하")
        }
        val encrypted = cipher.encrypt(issued.accessToken, TOKEN_KEY)
        redisTemplate.opsForValue().set(TOKEN_KEY, encrypted, Duration.ofSeconds(ttlSeconds))
        log.info { "토스 토큰 발급·저장: ttl=${ttlSeconds}s" }
        return issued.accessToken
    }

    // 락을 못 얻은 쪽은 발급하지 않고 대기 한도까지 토큰 키만 다시 읽음
    private fun waitForToken(): String {
        val deadline = nowMillis() + properties.tokenLockWaitMillis
        while (nowMillis() < deadline) {
            sleeper(POLL_INTERVAL_MILLIS)
            readCached()?.let { return it }
        }
        log.warn { "토스 토큰 락 대기 한도 초과: waitMillis=${properties.tokenLockWaitMillis}" }
        throw TossTokenUnavailableException("락 대기 한도 초과")
    }

    companion object {
        const val TOKEN_KEY = "toss:token"
        const val LOCK_KEY = "toss:token:lock"
        private const val POLL_INTERVAL_MILLIS = 100L
        private val DELETE_IF_EQUALS_SCRIPT = DefaultRedisScript(
            "if redis.call('GET', KEYS[1]) == ARGV[1] then return redis.call('DEL', KEYS[1]) else return 0 end",
            Long::class.java,
        )
    }
}
