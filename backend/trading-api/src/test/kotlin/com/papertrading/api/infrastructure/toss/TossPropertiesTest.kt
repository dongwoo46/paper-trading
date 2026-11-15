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
}
