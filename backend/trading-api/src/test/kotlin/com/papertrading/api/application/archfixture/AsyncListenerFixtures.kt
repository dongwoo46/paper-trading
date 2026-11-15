package com.papertrading.api.application.archfixture

import org.springframework.scheduling.annotation.Async
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

// ArchUnit 규칙 시험용 픽스처, 빈 아님(스테레오타입 없음)·미실행·DoNotIncludeTests 로 본 분석 제외

class AfterCommitAsyncListenerFixture {
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun on(event: String) = event
}

// phase 를 적지 않으면 Spring 기본값이 AFTER_COMMIT
class DefaultPhaseAsyncListenerFixture {
    @Async
    @TransactionalEventListener
    fun on(event: String) = event
}

class BeforeCommitAsyncListenerFixture {
    @Async
    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    fun on(event: String) = event
}

class AfterRollbackAsyncListenerFixture {
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_ROLLBACK)
    fun on(event: String) = event
}

class PlainAsyncMethodFixture {
    @Async
    fun run() = Unit
}

@Async
class ClassLevelAsyncFixture {
    fun run() = Unit
}

// Async 외 scheduling 타입은 허용 목록 밖
class ScheduledMethodFixture {
    @Scheduled(fixedDelay = 1000)
    fun run() = Unit
}
