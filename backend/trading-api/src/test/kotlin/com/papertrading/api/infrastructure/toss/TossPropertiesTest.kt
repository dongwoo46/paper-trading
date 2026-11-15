package com.papertrading.api.infrastructure.toss

import com.papertrading.api.common.exception.TossIntegrationDisabledException
import com.papertrading.api.support.LogCapture
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.context.annotation.Configuration
import java.util.Base64

class TossPropertiesTest {

    // 시험 전용 가짜 값, 실제 토스 키 아님
    private val validEncryptionKey = Base64.getEncoder().encodeToString(ByteArray(32) { (it + 7).toByte() })
    private val fakeSecret = "fake-client-secret-XYZ"

    @Configuration
    @EnableConfigurationProperties(TossProperties::class)
    class PropertiesConfig

    private val runner = ApplicationContextRunner().withUserConfiguration(PropertiesConfig::class.java)

    private fun fullProperties(vararg overrides: Pair<String, String>): Array<String> {
        val base = mapOf(
            "trading.toss.base-url" to "https://toss.example.invalid",
            "trading.toss.client-id" to "fake-client-id",
            "trading.toss.client-secret" to fakeSecret,
            "trading.toss.token-encryption-key" to validEncryptionKey,
        )
        return (base + overrides.toMap()).map { "${it.key}=${it.value}" }.toTypedArray()
    }

    @Test
    fun `필수 4개가 모두 있고 암호화 키 형식이 맞으면 활성이고 누락 키가 없다`() {
        runner.withPropertyValues(*fullProperties()).run { context ->
            val properties = context.getBean(TossProperties::class.java)

            assertTrue(properties.integrationEnabled())
            assertEquals(emptyList<String>(), properties.disabledKeyNames())
            properties.requireEnabled()
        }
    }

    @ParameterizedTest(name = "{0} 가 비면 토스만 비활성이고 컨텍스트는 기동한다")
    @CsvSource(
        "trading.toss.base-url, TOSS_BASE_URL",
        "trading.toss.client-id, TOSS_CLIENT_ID",
        "trading.toss.client-secret, TOSS_CLIENT_SECRET",
        "trading.toss.token-encryption-key, TOSS_TOKEN_ENCRYPTION_KEY",
    )
    fun `필수 키 하나가 비면 비활성이고 호출 때 키 이름만 담은 예외가 난다`(property: String, envName: String) {
        runner.withPropertyValues(*fullProperties(property to "  ")).run { context ->
            assertNull(context.startupFailure)
            val properties = context.getBean(TossProperties::class.java)

            assertFalse(properties.integrationEnabled())
            assertEquals(listOf(envName), properties.disabledKeyNames())
            val error = assertThrows(TossIntegrationDisabledException::class.java) { properties.requireEnabled() }
            assertEquals("TOSS_INTEGRATION_DISABLED", error.errorCode)
            assertTrue(error.message.orEmpty().contains(envName))
            assertFalse(error.message.orEmpty().contains(fakeSecret))
        }
    }

    @Test
    fun `필수 키가 전부 비어도 컨텍스트는 기동하고 누락 키 4개를 모두 알린다`() {
        runner.run { context ->
            assertNull(context.startupFailure)
            val properties = context.getBean(TossProperties::class.java)

            assertFalse(properties.integrationEnabled())
            assertEquals(
                listOf("TOSS_BASE_URL", "TOSS_CLIENT_ID", "TOSS_CLIENT_SECRET", "TOSS_TOKEN_ENCRYPTION_KEY"),
                properties.disabledKeyNames(),
            )
        }
    }

    @ParameterizedTest(name = "암호화 키 {0} 는 형식 오류로 비활성")
    @CsvSource(
        "base64 아님, not-base64-@@@",
        "16바이트, AAAAAAAAAAAAAAAAAAAAAA==",
    )
    fun `암호화 키 형식이 틀리면 비활성이고 키 값은 어디에도 싣지 않는다`(label: String, badKey: String) {
        LogCapture().use { logs ->
            runner.withPropertyValues(*fullProperties("trading.toss.token-encryption-key" to badKey)).run { context ->
                assertNull(context.startupFailure)
                val properties = context.getBean(TossProperties::class.java)

                assertFalse(properties.integrationEnabled(), label)
                assertEquals(listOf("TOSS_TOKEN_ENCRYPTION_KEY(형식 오류)"), properties.disabledKeyNames())
                val message = assertThrows(TossIntegrationDisabledException::class.java) {
                    properties.requireEnabled()
                }.message.orEmpty()
                assertFalse(message.contains(badKey))
                assertTrue(logs.lines().none { it.contains(badKey) || it.contains(fakeSecret) })
            }
        }
    }

    @Test
    fun `비활성이면 기동 때 누락 키 이름만 한 번 로그하고 값은 남기지 않는다`() {
        LogCapture().use { logs ->
            runner.withPropertyValues(*fullProperties("trading.toss.client-id" to "")).run { context ->
                assertNotNull(context.getBean(TossProperties::class.java))
            }

            val tossLines = logs.linesOf("TossProperties")
            assertEquals(1, tossLines.size, tossLines.toString())
            assertTrue(tossLines.single().contains("TOSS_CLIENT_ID"))
            assertTrue(logs.lines().none { it.contains(fakeSecret) || it.contains(validEncryptionKey) })
        }
    }

    @Test
    fun `선택 키는 기본값이 있고 설정으로 바꿀 수 있다`() {
        runner.run { context ->
            val properties = context.getBean(TossProperties::class.java)

            assertTrue(properties.tokenRefreshSkewSeconds > 0)
            assertTrue(properties.tokenLockTtlMillis > 15_000, "락 TTL 은 연결 5초 + 읽기 10초보다 길어야 함")
            assertTrue(properties.tokenLockWaitMillis > 0)
        }
        runner.withPropertyValues(
            "trading.toss.token-refresh-skew-seconds=11",
            "trading.toss.token-lock-ttl-millis=22000",
            "trading.toss.token-lock-wait-millis=33",
        ).run { context ->
            val properties = context.getBean(TossProperties::class.java)

            assertEquals(11L, properties.tokenRefreshSkewSeconds)
            assertEquals(22_000L, properties.tokenLockTtlMillis)
            assertEquals(33L, properties.tokenLockWaitMillis)
        }
    }

    @ParameterizedTest(name = "{0}={1} 는 범위 오류라 토스만 비활성이고 값은 어디에도 싣지 않는다")
    @CsvSource(
        "trading.toss.token-lock-ttl-millis, 15000, TOSS_TOKEN_LOCK_TTL_MILLIS(범위 오류)",
        "trading.toss.token-lock-ttl-millis, 1000, TOSS_TOKEN_LOCK_TTL_MILLIS(범위 오류)",
        "trading.toss.token-lock-ttl-millis, 0, TOSS_TOKEN_LOCK_TTL_MILLIS(범위 오류)",
        "trading.toss.token-lock-ttl-millis, -1, TOSS_TOKEN_LOCK_TTL_MILLIS(범위 오류)",
        "trading.toss.token-refresh-skew-seconds, -1, TOSS_TOKEN_REFRESH_SKEW_SECONDS(범위 오류)",
        "trading.toss.token-lock-wait-millis, 0, TOSS_TOKEN_LOCK_WAIT_MILLIS(범위 오류)",
        "trading.toss.token-lock-wait-millis, -5, TOSS_TOKEN_LOCK_WAIT_MILLIS(범위 오류)",
        "trading.toss.token-request-deadline-millis, 15000, TOSS_TOKEN_REQUEST_DEADLINE_MILLIS(범위 오류)",
        "trading.toss.token-request-deadline-millis, 15001, TOSS_TOKEN_REQUEST_DEADLINE_MILLIS(범위 오류)",
        "trading.toss.token-request-deadline-millis, 16999, TOSS_TOKEN_REQUEST_DEADLINE_MILLIS(범위 오류)",
        "trading.toss.token-request-deadline-millis, 0, TOSS_TOKEN_REQUEST_DEADLINE_MILLIS(범위 오류)",
        "trading.toss.token-request-deadline-millis, -1, TOSS_TOKEN_REQUEST_DEADLINE_MILLIS(범위 오류)",
        "trading.toss.rate-limit-max-retries, -1, TOSS_RATE_LIMIT_MAX_RETRIES(범위 오류)",
        "trading.toss.rate-limit-max-wait-millis, -1, TOSS_RATE_LIMIT_MAX_WAIT_MILLIS(범위 오류)",
        "trading.toss.rate-limit-max-queue-wait-millis, 0, TOSS_RATE_LIMIT_MAX_QUEUE_WAIT_MILLIS(범위 오류)",
        "trading.toss.rate-limit-max-queue-wait-millis, -1, TOSS_RATE_LIMIT_MAX_QUEUE_WAIT_MILLIS(범위 오류)",
        "trading.toss.rate-limits.ASSET, 0, TOSS_RATE_LIMIT_ASSET(범위 오류)",
        "trading.toss.rate-limits.MARKET_DATA_CHART, -2, TOSS_RATE_LIMIT_MARKET_DATA_CHART(범위 오류)",
    )
    fun `선택 키가 범위를 벗어나면 토스만 비활성이고 컨텍스트는 기동한다`(property: String, value: String, expectedName: String) {
        runner.withPropertyValues(*fullProperties(property to value)).run { context ->
            assertNull(context.startupFailure)
            val properties = context.getBean(TossProperties::class.java)

            assertFalse(properties.integrationEnabled())
            assertEquals(listOf(expectedName), properties.disabledKeyNames())
            val error = assertThrows(TossIntegrationDisabledException::class.java) { properties.requireEnabled() }
            assertTrue(error.message.orEmpty().contains(expectedName))
            assertFalse(error.message.orEmpty().contains(" $value"), "범위 오류 값은 싣지 않음")
        }
    }

    @ParameterizedTest(name = "{0}={1} 는 경계 안이라 활성이다")
    @CsvSource(
        "trading.toss.token-lock-ttl-millis, 15001",
        "trading.toss.token-refresh-skew-seconds, 0",
        "trading.toss.token-lock-wait-millis, 1",
        // devkit:test-change C-04 — 하한이 15초에서 17초(발급 필요 16초 + 락 획득 여유 1초)로 바뀜
        "trading.toss.token-request-deadline-millis, 17000",
        "trading.toss.rate-limit-max-retries, 0",
        "trading.toss.rate-limit-max-wait-millis, 0",
        "trading.toss.rate-limit-max-queue-wait-millis, 1",
        "trading.toss.rate-limits.ASSET, 1",
    )
    fun `선택 키가 경계값 안이면 활성이다`(property: String, value: String) {
        runner.withPropertyValues(*fullProperties(property to value)).run { context ->
            assertTrue(context.getBean(TossProperties::class.java).integrationEnabled())
        }
    }

    @Test
    fun `이전 암호화 키는 선택이라 비어 있어도 활성이고 올바른 키를 주면 활성이다`() {
        runner.withPropertyValues(*fullProperties()).run { context ->
            val properties = context.getBean(TossProperties::class.java)
            assertEquals("", properties.tokenPreviousEncryptionKey)
            assertTrue(properties.integrationEnabled())
        }
        runner.withPropertyValues(*fullProperties("trading.toss.token-previous-encryption-key" to validEncryptionKey))
            .run { context ->
                assertTrue(context.getBean(TossProperties::class.java).integrationEnabled())
            }
    }

    @ParameterizedTest(name = "이전 암호화 키 {0} 는 형식 오류로 비활성이고 값은 어디에도 싣지 않는다")
    @CsvSource(
        "base64 아님, not-base64-@@@",
        "16바이트, AAAAAAAAAAAAAAAAAAAAAA==",
    )
    fun `이전 암호화 키 형식이 틀리면 비활성이고 키 값은 어디에도 싣지 않는다`(label: String, badKey: String) {
        LogCapture().use { logs ->
            runner.withPropertyValues(*fullProperties("trading.toss.token-previous-encryption-key" to badKey))
                .run { context ->
                    val properties = context.getBean(TossProperties::class.java)

                    assertFalse(properties.integrationEnabled(), label)
                    assertEquals(listOf("TOSS_TOKEN_PREVIOUS_ENCRYPTION_KEY(형식 오류)"), properties.disabledKeyNames())
                    assertTrue(logs.lines().none { it.contains(badKey) })
                }
        }
    }

    @Test
    fun `비활성 판정 목록은 한 번 계산해 보관하고 호출마다 새로 만들지 않는다`() {
        val disabled = TossProperties()

        assertSame(disabled.disabledKeyNames(), disabled.disabledKeyNames())
    }

    @Test
    fun `base-url 은 기본값을 두지 않는다`() {
        runner.run { context ->
            assertEquals("", context.getBean(TossProperties::class.java).baseUrl)
        }
    }

    // ---- C-02: base-url 은 절대 https URL 이어야 함, 아니면 토스만 비활성 ----

    @ParameterizedTest(name = "base-url [{0}] 는 형식 오류라 토스만 비활성이다")
    @CsvSource(
        "api.example.com",
        "http://api.example.com",
        "ftp://api.example.com",
        "https://",
        "https:///path",
        "//api.example.com",
        "/relative/path",
        "not a url",
        "https://exa mple.com",
    )
    fun `base-url 이 절대 https URL 이 아니면 형식 오류로 비활성이고 값은 어디에도 싣지 않는다`(badUrl: String) {
        LogCapture().use { logs ->
            runner.withPropertyValues(*fullProperties("trading.toss.base-url" to badUrl)).run { context ->
                assertNull(context.startupFailure)
                val properties = context.getBean(TossProperties::class.java)

                assertFalse(properties.integrationEnabled(), badUrl)
                assertEquals(listOf("TOSS_BASE_URL(형식 오류)"), properties.disabledKeyNames())
                val message = assertThrows(TossIntegrationDisabledException::class.java) {
                    properties.requireEnabled()
                }.message.orEmpty()
                assertFalse(message.contains(badUrl), message)
                assertTrue(logs.lines().none { it.contains(badUrl) })
            }
        }
    }

    @ParameterizedTest(name = "base-url [{0}] 는 절대 https URL 이라 활성이다")
    @CsvSource(
        "https://api.example.com",
        "https://api.example.com/",
        "https://api.example.com:8443/base",
        "HTTPS://API.EXAMPLE.COM",
    )
    fun `base-url 이 절대 https URL 이면 활성이다`(goodUrl: String) {
        runner.withPropertyValues(*fullProperties("trading.toss.base-url" to goodUrl)).run { context ->
            assertTrue(context.getBean(TossProperties::class.java).integrationEnabled(), goodUrl)
        }
    }

    // ---- C-04: 요청 상한의 하한 한 상수가 범위 검사와 실행 검사를 함께 이끎 ----

    @Test
    fun `요청 상한 하한은 발급 필요 시간에 락 획득 여유를 더한 값이고 발급 필요 시간은 발급 최악에 AUTH 대기 허용을 더한 값이다`() {
        assertEquals(
            TossProperties.ISSUE_WORST_CASE_MILLIS + TossProperties.AUTH_WAIT_ALLOWANCE_MILLIS,
            TossProperties.ISSUE_TIME_BUDGET_MILLIS,
        )
        assertEquals(
            TossProperties.ISSUE_TIME_BUDGET_MILLIS + TossProperties.LOCK_ACQUIRE_MARGIN_MILLIS,
            TossProperties.MIN_REQUEST_DEADLINE_MILLIS,
        )
    }

    @Test
    fun `요청 상한이 하한 바로 아래면 비활성이고 하한이면 활성이며 기본값은 하한 이상이다`() {
        val min = TossProperties.MIN_REQUEST_DEADLINE_MILLIS
        listOf(min - 1 to false, min to true).forEach { (deadline, enabled) ->
            runner.withPropertyValues(*fullProperties("trading.toss.token-request-deadline-millis" to "$deadline"))
                .run { context ->
                    assertEquals(enabled, context.getBean(TossProperties::class.java).integrationEnabled(), "$deadline")
                }
        }
        assertTrue(TossProperties().tokenRequestDeadlineMillis >= min)
    }

    @Test
    fun `호출 한도 그룹은 설정으로 받고 코드에는 기본 수치를 두지 않는다`() {
        runner.withPropertyValues(
            "trading.toss.rate-limits.MARKET_DATA_CHART=7",
            "trading.toss.rate-limits.AUTH=3",
            "trading.toss.rate-limits.order-info=4",
        ).run { context ->
            assertEquals(
                mapOf(
                    TossRateLimitGroup.MARKET_DATA_CHART to 7,
                    TossRateLimitGroup.AUTH to 3,
                    TossRateLimitGroup.ORDER_INFO to 4,
                ),
                context.getBean(TossProperties::class.java).rateLimits,
            )
        }
        runner.run { context ->
            assertEquals(emptyMap<TossRateLimitGroup, Int>(), context.getBean(TossProperties::class.java).rateLimits)
        }
    }

    // 한도 기본값의 출처는 application.yaml 하나, 값은 조사 문서(2026-11-15) 수치
    @Test
    fun `application yaml 이 새 선택 키의 기본값과 그룹별 한도 기본값을 준다`() {
        runner.withInitializer(ConfigDataApplicationContextInitializer()).run { context ->
            val properties = context.getBean(TossProperties::class.java)

            assertEquals(20_000L, properties.tokenRequestDeadlineMillis)
            assertEquals(2, properties.rateLimitMaxRetries)
            assertEquals(5_000L, properties.rateLimitMaxWaitMillis)
            assertEquals(5_000L, properties.rateLimitMaxQueueWaitMillis)
            assertEquals(
                mapOf(
                    TossRateLimitGroup.AUTH to 5,
                    TossRateLimitGroup.ACCOUNT to 1,
                    TossRateLimitGroup.ASSET to 5,
                    TossRateLimitGroup.ORDER_INFO to 6,
                    TossRateLimitGroup.MARKET_DATA to 15,
                    TossRateLimitGroup.MARKET_DATA_CHART to 20,
                ),
                properties.rateLimits,
            )
        }
    }

    @Test
    fun `환경변수 이름으로 그룹별 한도와 새 선택 키를 덮을 수 있다`() {
        runner.withInitializer(ConfigDataApplicationContextInitializer())
            .withSystemProperties(
                "TOSS_RATE_LIMIT_ACCOUNT=2",
                "TOSS_TOKEN_REQUEST_DEADLINE_MILLIS=25000",
                "TOSS_RATE_LIMIT_MAX_RETRIES=4",
                "TOSS_RATE_LIMIT_MAX_WAIT_MILLIS=9000",
                "TOSS_RATE_LIMIT_MAX_QUEUE_WAIT_MILLIS=7000",
            ).run { context ->
                val properties = context.getBean(TossProperties::class.java)

                assertEquals(2, properties.rateLimits[TossRateLimitGroup.ACCOUNT])
                assertEquals(25_000L, properties.tokenRequestDeadlineMillis)
                assertEquals(4, properties.rateLimitMaxRetries)
                assertEquals(9_000L, properties.rateLimitMaxWaitMillis)
                assertEquals(7_000L, properties.rateLimitMaxQueueWaitMillis)
            }
    }
}
