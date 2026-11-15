package com.papertrading.api.infrastructure.toss

import com.papertrading.api.common.exception.TossTokenUnavailableException
import com.papertrading.api.infrastructure.redis.RedisDistributedLock
import com.papertrading.api.infrastructure.security.AesGcmTokenCipher
import com.papertrading.api.infrastructure.security.TokenCipherException
import mu.KotlinLogging
import org.springframework.data.redis.core.StringRedisTemplate

/**
 * 토스 액세스 토큰 캐시·발급 직렬화·무효화 (토큰은 클라이언트당 1개만 유효 — RULE-06)
 * 순서: 캐시 확인 → 락 획득 → 캐시 재확인 → 발급 → 락을 쥔 채 암호화 저장 → 락 해제, 락 없이 발급하는 경로 없음
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

    // 비활성 설정에서는 만들어지지 않도록 첫 사용 때 만들고 이후 보관
    private val cipher: AesGcmTokenCipher by lazy {
        AesGcmTokenCipher.fromBase64Key(
            properties.tokenEncryptionKey,
            properties.tokenPreviousEncryptionKey.takeIf { it.isNotBlank() },
        )
    }

    fun getToken(): String {
        properties.requireEnabled()
        return readCached() ?: issueUnderLock()
    }

    // Redis 값이 이 토큰일 때만 삭제, 다른 쪽이 새로 저장한 토큰은 지우지 않음 (D12)
    fun invalidate(token: String): Boolean {
        properties.requireEnabled()
        val raw = redisTemplate.opsForValue().get(TOKEN_KEY) ?: return false
        // 암호문은 IV 가 매번 달라 평문 비교가 안 되므로 읽은 암호문 그대로를 비교해 삭제
        return decryptOrNull(raw) == token && lock.deleteIfEquals(TOKEN_KEY, raw)
    }

    // 락을 얻을 때까지 대기 한도 안에서 반복, 대기 중 토큰이 생기면 그 토큰을 쓰고 락이 비어도 토큰이 없으면 직접 발급 (C-02)
    private fun issueUnderLock(): String {
        val deadline = nowMillis() + properties.tokenLockWaitMillis
        while (true) {
            val owner = lock.tryAcquire(LOCK_KEY, properties.tokenLockTtlMillis)
            if (owner != null) return issueHolding(owner)
            if (nowMillis() >= deadline) break
            sleeper(POLL_INTERVAL_MILLIS)
            readCached()?.let { return it }
        }
        log.warn { "토스 토큰 락 대기 한도 초과: waitMillis=${properties.tokenLockWaitMillis}" }
        throw TossTokenUnavailableException("락 대기 한도 초과")
    }

    private fun issueHolding(owner: String): String = try {
        readCached() ?: issueAndStore(owner)
    } finally {
        lock.release(LOCK_KEY, owner)
    }

    private fun readCached(): String? = redisTemplate.opsForValue().get(TOKEN_KEY)?.let(::decryptOrNull)

    // 복호화 실패(키 교체·형식 깨짐)는 캐시 없음으로 보고 재발급으로 복구
    private fun decryptOrNull(raw: String): String? = try {
        cipher.decrypt(raw, TOKEN_KEY)
    } catch (e: TokenCipherException) {
        log.warn { "토스 토큰 캐시 복호화 실패, 캐시 없음으로 처리: reason=${e.message}" }
        null
    }

    private fun issueAndStore(owner: String): String {
        val issued = issuer.issue()
        val ttlSeconds = storageTtlSeconds(issued.expiresInSeconds)
        val encrypted = cipher.encrypt(issued.accessToken, TOKEN_KEY)
        // 락을 아직 쥔 경우에만 저장, 발급이 락 TTL 을 넘겨 락을 잃었으면 다른 쪽의 새 토큰을 덮어쓰지 않음 (C-03)
        if (!lock.storeIfHeld(LOCK_KEY, owner, TOKEN_KEY, encrypted, ttlSeconds * MILLIS_PER_SECOND)) {
            log.warn { "토스 토큰 저장 직전 락을 잃어 저장하지 않음" }
            return readCached() ?: throw TossTokenUnavailableException("발급 도중 락 만료")
        }
        log.info { "토스 토큰 발급·저장: ttl=${ttlSeconds}s" }
        return issued.accessToken
    }

    // expires_in − 갱신 여유, 0 이하이면 받은 토큰을 버리지 않고 expires_in 의 절반(최소 1초)으로 줄여 저장 (C-01)
    private fun storageTtlSeconds(expiresInSeconds: Long): Long {
        val ttlSeconds = expiresInSeconds - properties.tokenRefreshSkewSeconds
        if (ttlSeconds > 0) return ttlSeconds
        val reduced = maxOf(expiresInSeconds / 2, 1L)
        log.warn { "토스 토큰 수명이 갱신 여유 이하라 수명의 절반으로 저장: expiresIn=${expiresInSeconds}s ttl=${reduced}s" }
        return reduced
    }

    companion object {
        const val TOKEN_KEY = "toss:token"
        const val LOCK_KEY = "toss:token:lock"
        private const val POLL_INTERVAL_MILLIS = 100L
        private const val MILLIS_PER_SECOND = 1_000L
    }
}
