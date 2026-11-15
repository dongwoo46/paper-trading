package com.papertrading.api.infrastructure.toss

import com.papertrading.api.common.exception.TossIntegrationDisabledException
import com.papertrading.api.infrastructure.security.AesGcmTokenCipher
import jakarta.annotation.PostConstruct
import mu.KotlinLogging
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

/**
 * 토스 연동 설정 (trading.toss.*), 값은 환경변수로만 받음
 * 필수 4개 중 하나라도 비었거나 암호화 키 형식이 틀리면 토스만 비활성, 앱 기동은 막지 않음 (EDGE-01)
 */
@Component
@ConfigurationProperties(prefix = "trading.toss")
class TossProperties {
    private val log = KotlinLogging.logger {}

    // 필수 4개, 기본값 없음(REST 호스트는 조사 문서에 없어 날조하지 않음)
    var baseUrl: String = ""
    var clientId: String = ""
    var clientSecret: String = ""
    var tokenEncryptionKey: String = ""

    // 선택 키, 락 TTL 은 연결 5초 + 읽기 10초(RestTemplateConfig)보다 길게
    var tokenRefreshSkewSeconds: Long = DEFAULT_REFRESH_SKEW_SECONDS
    var tokenLockTtlMillis: Long = DEFAULT_LOCK_TTL_MILLIS
    var tokenLockWaitMillis: Long = DEFAULT_LOCK_WAIT_MILLIS

    // 누락·오류 설정의 환경변수 이름만, 값은 돌려주지 않음
    fun disabledKeyNames(): List<String> = buildList {
        if (baseUrl.isBlank()) add(ENV_BASE_URL)
        if (clientId.isBlank()) add(ENV_CLIENT_ID)
        if (clientSecret.isBlank()) add(ENV_CLIENT_SECRET)
        when {
            tokenEncryptionKey.isBlank() -> add(ENV_ENCRYPTION_KEY)
            !AesGcmTokenCipher.isValidBase64Key(tokenEncryptionKey) -> add("$ENV_ENCRYPTION_KEY(형식 오류)")
        }
    }

    fun integrationEnabled(): Boolean = disabledKeyNames().isEmpty()

    fun requireEnabled() {
        val keyNames = disabledKeyNames()
        if (keyNames.isNotEmpty()) throw TossIntegrationDisabledException(keyNames)
    }

    // 기동 때 1회, 이름만 남기고 값은 남기지 않음
    @PostConstruct
    fun logStartupState() {
        val keyNames = disabledKeyNames()
        if (keyNames.isEmpty()) {
            log.info { "토스 연동 활성" }
        } else {
            log.warn { "토스 연동 비활성: 누락·오류 설정=${keyNames.joinToString(", ")}" }
        }
    }

    companion object {
        const val ENV_BASE_URL = "TOSS_BASE_URL"
        const val ENV_CLIENT_ID = "TOSS_CLIENT_ID"
        const val ENV_CLIENT_SECRET = "TOSS_CLIENT_SECRET"
        const val ENV_ENCRYPTION_KEY = "TOSS_TOKEN_ENCRYPTION_KEY"

        // 토스 토큰 수명은 스키마 확정 전이라 모름, 작은 여유로 시작하고 운영에서 조정
        private const val DEFAULT_REFRESH_SKEW_SECONDS = 60L
        private const val DEFAULT_LOCK_TTL_MILLIS = 20_000L
        private const val DEFAULT_LOCK_WAIT_MILLIS = 15_000L
    }
}
