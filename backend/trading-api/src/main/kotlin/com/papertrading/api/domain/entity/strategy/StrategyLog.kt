package com.papertrading.api.domain.entity.strategy
import com.papertrading.api.domain.entity.account.Account

import com.papertrading.api.domain.entity.base.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.Instant

/**
 * 전략 실행 로그
 * 전략 실행 중 발생한 이벤트·오류를 시계열로 기록한다.
 * logLevel: INFO | WARN | ERROR. context(JSONB): 신호값·포지션 등 실행 맥락.
 */
@Entity
@Table(name = "strategy_logs")
class StrategyLog protected constructor() : BaseTimeEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "strategy_id", nullable = false)
    lateinit var strategy: Strategy
        private set

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    lateinit var account: Account
        private set

    @Column(name = "log_level", nullable = false, length = 10)
    lateinit var logLevel: String
        private set

    @Column(name = "message", nullable = false, columnDefinition = "text")
    lateinit var message: String
        private set

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "context", columnDefinition = "jsonb")
    var context: String? = null
        private set

    @Column(name = "logged_at", nullable = false)
    var loggedAt: Instant = Instant.now()
        private set

    fun updateContext(newContext: String?) {
        context = newContext?.trim()?.ifBlank { null }
    }

    companion object {
        private val ALLOWED_LOG_LEVELS = setOf("INFO", "WARN", "ERROR")

        fun create(
            strategy: Strategy,
            account: Account,
            logLevel: String,
            message: String,
            context: String? = null,
            loggedAt: Instant = Instant.now()
        ): StrategyLog {
            val normalizedLevel = logLevel.trim().uppercase()
            require(normalizedLevel in ALLOWED_LOG_LEVELS) {
                "logLevel은 INFO/WARN/ERROR 중 하나여야 합니다."
            }
            require(message.isNotBlank()) { "로그 메시지는 비어 있을 수 없습니다." }
            require(!loggedAt.isAfter(Instant.now().plusSeconds(5))) {
                "loggedAt은 미래 시각일 수 없습니다."
            }

            return StrategyLog().apply {
                this.strategy = strategy
                this.account = account
                this.logLevel = normalizedLevel
                this.message = message
                this.context = context?.trim()?.ifBlank { null }
                this.loggedAt = loggedAt
            }
        }
    }
}
