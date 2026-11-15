package com.papertrading.api.infrastructure.toss

import com.papertrading.api.common.exception.TossIntegrationDisabledException
import com.papertrading.api.common.exception.TossRateLimitedException
import org.springframework.stereotype.Component

/**
 * 토스 호출 한도 리미터 (프로세스 안, 그룹별 최소 간격 = 초당 한도의 역수), 호출 프로세스가 둘이 되는 3단계에서 Redis 공유로 대체
 * 슬롯을 락 안에서 예약하고 잠은 락 밖에서 자므로, 스레드가 여럿이어도 슬롯이 겹치지 않고 서로를 막지 않음
 * 예약 전 대기가 설정한 최대 대기 상한(rate-limit-max-wait-millis)을 넘으면 슬롯을 잡지 않고 TOSS_RATE_LIMITED (C-01)
 * 시계와 대기는 주입, 시험이 실제로 오래 잠들지 않게
 */
@Component
class TossRateLimiter(
    private val properties: TossProperties,
    private val nowMillis: () -> Long = System::currentTimeMillis,
    private val sleeper: (Long) -> Unit = Thread::sleep,
) {
    private val nextFreeSlotMillis = HashMap<TossRateLimitGroup, Long>()

    fun acquire(group: TossRateLimitGroup) {
        val intervalMillis = minIntervalMillis(group)
        val waitMillis = reserveSlot(group, intervalMillis)
        if (waitMillis > 0) sleeper(waitMillis)
    }

    // 올림, 나누어떨어지지 않는 한도도 한도를 넘기지 않는 쪽으로
    private fun minIntervalMillis(group: TossRateLimitGroup): Long {
        val perSecond = properties.rateLimits[group]
            ?: throw TossIntegrationDisabledException(listOf(envName(group)))
        if (perSecond <= 0) throw TossIntegrationDisabledException(listOf("${envName(group)}$OUT_OF_RANGE"))
        return (MILLIS_PER_SECOND + perSecond - 1) / perSecond
    }

    // 예약 전 대기가 최대 대기 상한을 넘으면 슬롯을 잡지 않고 실패, 대기열이 끝없이 길어져 요청 스레드가 묶이지 않게 (RISK-08, C-01)
    @Synchronized
    private fun reserveSlot(group: TossRateLimitGroup, intervalMillis: Long): Long {
        val now = nowMillis()
        val slot = maxOf(now, nextFreeSlotMillis[group] ?: now)
        val waitMillis = slot - now
        if (waitMillis > properties.rateLimitMaxWaitMillis) {
            throw TossRateLimitedException("한도 대기가 상한을 넘음: group=$group")
        }
        nextFreeSlotMillis[group] = slot + intervalMillis
        return waitMillis
    }

    /**
     * 429 를 받은 그룹의 다음 슬롯을 now + delay 뒤로 밀어, 모든 스레드가 같은 Retry-After 를 나눔 (C-05)
     * 이미 더 뒤에 있는 슬롯은 앞당기지 않음
     */
    @Synchronized
    fun deferNextSlot(group: TossRateLimitGroup, delayMillis: Long) {
        val deferredSlot = nowMillis() + delayMillis
        nextFreeSlotMillis[group] = maxOf(nextFreeSlotMillis[group] ?: deferredSlot, deferredSlot)
    }

    companion object {
        private const val MILLIS_PER_SECOND = 1_000L
        private const val OUT_OF_RANGE = "(범위 오류)"

        fun envName(group: TossRateLimitGroup): String = "TOSS_RATE_LIMIT_${group.name}"
    }
}
