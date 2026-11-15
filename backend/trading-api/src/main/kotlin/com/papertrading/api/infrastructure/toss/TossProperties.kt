package com.papertrading.api.infrastructure.toss

import com.papertrading.api.common.exception.TossIntegrationDisabledException
import com.papertrading.api.infrastructure.config.RestTemplateConfig
import com.papertrading.api.infrastructure.security.AesGcmTokenCipher
import jakarta.annotation.PostConstruct
import mu.KotlinLogging
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component
import java.net.URI
import java.net.URISyntaxException

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

    // 선택, 키 교체 중에만 옛 키를 넣어 옛 키로 저장된 토큰도 읽음 (C-08), 비어 있으면 없음
    var tokenPreviousEncryptionKey: String = ""

    // 선택 키, 락 TTL 은 연결 5초 + 읽기 10초(RestTemplateConfig)보다 길게
    var tokenRefreshSkewSeconds: Long = DEFAULT_REFRESH_SKEW_SECONDS
    var tokenLockTtlMillis: Long = DEFAULT_LOCK_TTL_MILLIS
    var tokenLockWaitMillis: Long = DEFAULT_LOCK_WAIT_MILLIS

    // 토큰 요청 하나의 전체 상한(락 대기와 발급을 합친 시간), 락을 얻은 시점의 남은 시간이 발급 필요 시간보다 짧으면 발급하지 않음 (R1-02, C-04)
    var tokenRequestDeadlineMillis: Long = DEFAULT_REQUEST_DEADLINE_MILLIS

    // 429 재시도 횟수와 그 대기 시간 합계의 상한, 요청 스레드를 막는 시간을 묶음 (RISK-08)
    var rateLimitMaxRetries: Int = DEFAULT_RATE_LIMIT_MAX_RETRIES
    var rateLimitMaxWaitMillis: Long = DEFAULT_RATE_LIMIT_MAX_WAIT_MILLIS

    // 그룹별 초당 한도, 수치는 application.yaml 이 유일한 출처(코드 상수 금지), 빠진 그룹은 호출 때 TossRateLimiter 가 실패시킴
    var rateLimits: Map<TossRateLimitGroup, Int> = emptyMap()

    // 설정은 기동 때 한 번 묶이므로 첫 판정을 보관, 호출마다 키 디코딩·암호기 생성을 되풀이하지 않음
    private val disabledKeys: List<String> by lazy { computeDisabledKeyNames() }

    // 누락·오류 설정의 환경변수 이름만, 값은 돌려주지 않음
    fun disabledKeyNames(): List<String> = disabledKeys

    fun integrationEnabled(): Boolean = disabledKeys.isEmpty()

    fun requireEnabled() {
        if (disabledKeys.isNotEmpty()) throw TossIntegrationDisabledException(disabledKeys)
    }

    private fun computeDisabledKeyNames(): List<String> = missingOrMalformedKeyNames() + outOfRangeKeyNames()

    private fun missingOrMalformedKeyNames(): List<String> = buildList {
        when {
            baseUrl.isBlank() -> add(ENV_BASE_URL)
            !isAbsoluteHttpsUrl(baseUrl) -> add("$ENV_BASE_URL(형식 오류)")
        }
        if (clientId.isBlank()) add(ENV_CLIENT_ID)
        if (clientSecret.isBlank()) add(ENV_CLIENT_SECRET)
        when {
            tokenEncryptionKey.isBlank() -> add(ENV_ENCRYPTION_KEY)
            !AesGcmTokenCipher.isValidBase64Key(tokenEncryptionKey) -> add("$ENV_ENCRYPTION_KEY(형식 오류)")
        }
        val previousKey = tokenPreviousEncryptionKey
        if (previousKey.isNotBlank() && !AesGcmTokenCipher.isValidBase64Key(previousKey)) {
            add("$ENV_PREVIOUS_ENCRYPTION_KEY(형식 오류)")
        }
    }

    // scheme 이 없으면 RestTemplate 이 IllegalArgumentException 을 던져 400 으로 새고, http 는 client secret 과 토큰이 평문으로 나감 (C-02)
    private fun isAbsoluteHttpsUrl(value: String): Boolean {
        val uri = try {
            URI(value)
        } catch (_: URISyntaxException) {
            return false
        }
        return uri.scheme.equals(HTTPS_SCHEME, ignoreCase = true) && !uri.host.isNullOrBlank()
    }

    // 범위를 벗어난 선택 키도 토스만 비활성 (C-04), 값은 싣지 않고 이름만
    private fun outOfRangeKeyNames(): List<String> = buildList {
        if (tokenRefreshSkewSeconds < 0) add("$ENV_REFRESH_SKEW$OUT_OF_RANGE")
        if (tokenLockTtlMillis <= MIN_LOCK_TTL_EXCLUSIVE_MILLIS) add("$ENV_LOCK_TTL$OUT_OF_RANGE")
        if (tokenLockWaitMillis <= 0) add("$ENV_LOCK_WAIT$OUT_OF_RANGE")
        if (tokenRequestDeadlineMillis < MIN_REQUEST_DEADLINE_MILLIS) add("$ENV_REQUEST_DEADLINE$OUT_OF_RANGE")
        if (rateLimitMaxRetries < 0) add("$ENV_RATE_LIMIT_MAX_RETRIES$OUT_OF_RANGE")
        if (rateLimitMaxWaitMillis < 0) add("$ENV_RATE_LIMIT_MAX_WAIT$OUT_OF_RANGE")
        rateLimits.filterValues { it <= 0 }.keys.forEach { add("${TossRateLimiter.envName(it)}$OUT_OF_RANGE") }
    }

    // 기동 때 1회, 이름만 남기고 값은 남기지 않음
    @PostConstruct
    fun logStartupState() {
        val keyNames = disabledKeys
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
        private const val ENV_PREVIOUS_ENCRYPTION_KEY = "TOSS_TOKEN_PREVIOUS_ENCRYPTION_KEY"
        private const val ENV_REFRESH_SKEW = "TOSS_TOKEN_REFRESH_SKEW_SECONDS"
        private const val ENV_LOCK_TTL = "TOSS_TOKEN_LOCK_TTL_MILLIS"
        private const val ENV_LOCK_WAIT = "TOSS_TOKEN_LOCK_WAIT_MILLIS"
        private const val ENV_REQUEST_DEADLINE = "TOSS_TOKEN_REQUEST_DEADLINE_MILLIS"
        private const val ENV_RATE_LIMIT_MAX_RETRIES = "TOSS_RATE_LIMIT_MAX_RETRIES"
        private const val ENV_RATE_LIMIT_MAX_WAIT = "TOSS_RATE_LIMIT_MAX_WAIT_MILLIS"
        private const val OUT_OF_RANGE = "(범위 오류)"

        // 발급 최악 소요 = 연결 타임아웃 + 읽기 타임아웃(RestTemplateConfig), 락 TTL 과 요청 전체 상한이 이 값을 넘어야 함
        const val ISSUE_WORST_CASE_MILLIS =
            (RestTemplateConfig.CONNECT_TIMEOUT_SECONDS + RestTemplateConfig.READ_TIMEOUT_SECONDS) * 1_000L

        // 락 안에서 도는 AUTH 한도 대기 허용, AUTH 한도를 초당 1회까지 낮춰도 간격 1초 (C-04)
        const val AUTH_WAIT_ALLOWANCE_MILLIS = 1_000L

        // 락을 얻은 시점에 남아 있어야 하는 발급 필요 시간 = 발급 최악 + AUTH 한도 대기, TokenManager 의 실행 검사가 씀 (C-04)
        const val ISSUE_TIME_BUDGET_MILLIS = ISSUE_WORST_CASE_MILLIS + AUTH_WAIT_ALLOWANCE_MILLIS

        // 락 획득까지 Redis 왕복(캐시 확인·락 획득·재확인)에 드는 시간 여유 (C-04)
        const val LOCK_ACQUIRE_MARGIN_MILLIS = 1_000L

        // 요청 전체 상한의 하한 = 발급 필요 시간 + 락 획득 여유, 범위 검사가 씀, 이 값이어도 여유 안에서 락을 얻은 첫 요청은 발급함 (C-04)
        const val MIN_REQUEST_DEADLINE_MILLIS = ISSUE_TIME_BUDGET_MILLIS + LOCK_ACQUIRE_MARGIN_MILLIS

        private const val HTTPS_SCHEME = "https"

        // 락 TTL 은 발급 최악 소요를 넘어야 함, 이하이면 발급 도중 락이 만료
        private const val MIN_LOCK_TTL_EXCLUSIVE_MILLIS = ISSUE_WORST_CASE_MILLIS

        // 토스 토큰 수명은 스키마 확정 전이라 모름, 작은 여유로 시작하고 운영에서 조정
        private const val DEFAULT_REFRESH_SKEW_SECONDS = 60L
        private const val DEFAULT_LOCK_TTL_MILLIS = 20_000L
        private const val DEFAULT_LOCK_WAIT_MILLIS = 15_000L

        // 발급 최악 15초에 락 대기 5초를 더한 값, 기본 20초는 open-questions FLAG-16 사람 결정(2026-11-15)
        private const val DEFAULT_REQUEST_DEADLINE_MILLIS = 20_000L

        // 429 는 Retry-After 가 보통 1초라 2회 재시도, 합계 5초면 요청 스레드가 길게 잡히지 않음, 구현자 가정
        private const val DEFAULT_RATE_LIMIT_MAX_RETRIES = 2
        private const val DEFAULT_RATE_LIMIT_MAX_WAIT_MILLIS = 5_000L
    }
}
