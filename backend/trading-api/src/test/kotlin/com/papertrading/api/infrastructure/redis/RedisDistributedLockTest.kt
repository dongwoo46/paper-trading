package com.papertrading.api.infrastructure.redis

import com.papertrading.api.support.SharedRedisContainer
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.util.concurrent.TimeUnit

// 실제 Redis 컨테이너로 SET NX PX 와 소유자 확인 해제를 확인 (PTT1)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RedisDistributedLockTest {

    private val factory = SharedRedisContainer.newConnectionFactory()
    private val redisTemplate = SharedRedisContainer.newTemplate(factory)
    private val lock = RedisDistributedLock(redisTemplate)
    private val key = "test:lock:1"

    @BeforeEach
    fun clean() {
        redisTemplate.delete(key)
    }

    @AfterAll
    fun close() {
        factory.destroy()
    }

    @Test
    fun `빈 키는 락을 얻고 값은 소유자 식별값이며 TTL 이 걸린다`() {
        val owner = lock.tryAcquire(key, 5_000)

        assertNotNull(owner)
        assertEquals(owner, redisTemplate.opsForValue().get(key))
        val ttl = redisTemplate.getExpire(key, TimeUnit.MILLISECONDS)
        assertTrue(ttl in 1..5_000, "ttl=$ttl")
    }

    @Test
    fun `이미 잡힌 락은 얻지 못하고 기존 소유자를 건드리지 않는다`() {
        val first = lock.tryAcquire(key, 5_000)

        assertNull(lock.tryAcquire(key, 5_000))
        assertEquals(first, redisTemplate.opsForValue().get(key))
    }

    @Test
    fun `소유자가 아닌 쪽의 해제는 락을 풀지 못한다`() {
        val owner = lock.tryAcquire(key, 5_000)

        assertFalse(lock.release(key, "다른-소유자"))
        assertEquals(owner, redisTemplate.opsForValue().get(key))
    }

    @Test
    fun `소유자의 해제는 락을 풀고 다음 획득이 가능하다`() {
        val owner = requireNotNull(lock.tryAcquire(key, 5_000))

        assertTrue(lock.release(key, owner))
        assertNull(redisTemplate.opsForValue().get(key))
        assertNotNull(lock.tryAcquire(key, 5_000))
    }

    @Test
    fun `TTL 이 지나면 락이 사라지고 옛 소유자의 해제는 새 소유자의 락을 풀지 못한다`() {
        val oldOwner = requireNotNull(lock.tryAcquire(key, 100))
        Thread.sleep(250)
        assertNull(redisTemplate.opsForValue().get(key), "TTL 만료")

        val newOwner = requireNotNull(lock.tryAcquire(key, 5_000))

        assertNotEquals(oldOwner, newOwner)
        assertFalse(lock.release(key, oldOwner))
        assertEquals(newOwner, redisTemplate.opsForValue().get(key))
    }

    @Test
    fun `이미 풀린 락의 해제는 false 이다`() {
        assertFalse(lock.release(key, "아무-소유자"))
    }
}
