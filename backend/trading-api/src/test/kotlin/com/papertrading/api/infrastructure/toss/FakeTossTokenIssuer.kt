package com.papertrading.api.infrastructure.toss

import java.util.concurrent.atomic.AtomicInteger

// 시험 전용 발급기, 호출 수를 세고 호출마다 다른 가짜 토큰을 돌려줌
class FakeTossTokenIssuer(
    private val expiresInSeconds: Long = 3_600,
    private val delayMillis: Long = 0,
    private val onIssue: () -> Unit = {},
) : TossTokenIssuer {
    private val calls = AtomicInteger()

    val callCount: Int get() = calls.get()

    override fun issue(): TossIssuedToken {
        val sequence = calls.incrementAndGet()
        onIssue()
        if (delayMillis > 0) Thread.sleep(delayMillis)
        return TossIssuedToken("fake-access-token-$sequence", expiresInSeconds)
    }
}
