package com.papertrading.api.presentation.exception

import com.papertrading.api.common.exception.BrokerOrderNotSupportedException
import com.papertrading.api.common.exception.InternalErrorException
import com.papertrading.api.common.exception.LockedDepositInsufficientException
import com.papertrading.api.common.exception.NotImplementedException
import com.papertrading.api.common.exception.ServiceUnavailableException
import com.papertrading.api.common.exception.TossAuthFailedException
import com.papertrading.api.common.exception.TossIntegrationDisabledException
import com.papertrading.api.common.exception.TossIpNotAllowedException
import com.papertrading.api.common.exception.TossMaintenanceException
import com.papertrading.api.common.exception.TossRateLimitedException
import com.papertrading.api.common.exception.TossRequestRejectedException
import com.papertrading.api.common.exception.TossResponseInvalidException
import com.papertrading.api.common.exception.TossTimeoutException
import com.papertrading.api.common.exception.TossTokenUnavailableException
import com.papertrading.api.common.exception.TossUnavailableException
import com.papertrading.api.support.LogCapture
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

/** 범주 예외(I 501, J 503, K 500)의 범주→HTTP 상태 매핑, 예외는 Spring 을 모르고 핸들러만 상태를 안다 */
class GlobalExceptionHandlerTest {

    private val handler = GlobalExceptionHandler()

    @Test
    fun `범주 I 예외는 501 과 예외 code 로 응답한다`() {
        val response = handler.handleNotImplemented(BrokerOrderNotSupportedException("TOSS_LIVE"))

        assertEquals(501, response.statusCode.value())
        assertEquals(501, response.body?.status)
        assertEquals("BROKER_ORDER_NOT_SUPPORTED", response.body?.code)
        assertEquals("해당 거래 모드의 주문은 아직 지원하지 않습니다. tradingMode=TOSS_LIVE", response.body?.message)
    }

    @Test
    fun `범주 I 기반 예외의 code 와 메시지가 그대로 응답에 실린다`() {
        val response = handler.handleNotImplemented(NotImplementedException("SOME_FEATURE", "아직 없음"))

        assertEquals(501, response.statusCode.value())
        assertEquals("SOME_FEATURE", response.body?.code)
        assertEquals("아직 없음", response.body?.message)
    }

    @Test
    fun `범주 K 예외는 500 과 예외 code 로 응답하고 내부 메시지를 싣지 않는다`() {
        val internalMessage = "잠금 예수금이 부족합니다. accountId=7"
        val response = handler.handleInternalError(InternalErrorException("SOME_DEFECT", internalMessage))

        assertEquals(500, response.statusCode.value())
        assertEquals(500, response.body?.status)
        assertEquals("SOME_DEFECT", response.body?.code)
        assertEquals("서버 오류가 발생했습니다.", response.body?.message)
        assertFalse(response.body?.message.orEmpty().contains("accountId"))
    }

    @Test
    fun `범주 K 하위 예외도 자기 code 로 500 응답한다`() {
        val response = handler.handleInternalError(LockedDepositInsufficientException())

        assertEquals(500, response.statusCode.value())
        assertEquals("LOCKED_DEPOSIT_INSUFFICIENT", response.body?.code)
    }

    // devkit:test-change D15 C-09 — 처리기는 예외 메시지를 응답에 싣지 않고 code 별 고정 문구만 싣는다(원문은 로그에만)
    @Test
    fun `범주 J 예외는 503 과 예외 code 로 응답하고 모르는 code 는 기본 고정 문구를 싣는다`() {
        val response = handler.handleServiceUnavailable(
            ServiceUnavailableException("SOME_UPSTREAM", "잠시 뒤 다시 시도해 주세요."),
        )

        assertEquals(503, response.statusCode.value())
        assertEquals(503, response.body?.status)
        assertEquals("SOME_UPSTREAM", response.body?.code)
        assertEquals("외부 서비스를 일시적으로 사용할 수 없습니다.", response.body?.message)
    }

    // devkit:test-change D15 C-09 — TOSS_TOKEN_UNAVAILABLE 응답 문구는 사유 없는 고정 문구
    @Test
    fun `범주 J 하위 예외 TOSS_TOKEN_UNAVAILABLE 도 503 과 고정 문구로 응답한다`() {
        val response = handler.handleServiceUnavailable(TossTokenUnavailableException("대기 한도 초과"))

        assertEquals(503, response.statusCode.value())
        assertEquals("TOSS_TOKEN_UNAVAILABLE", response.body?.code)
        assertEquals("토스 액세스 토큰을 사용할 수 없습니다.", response.body?.message)
    }

    @Test
    fun `범주 J 예외에 토스 원문이 실려도 응답에는 싣지 않고 로그에만 남긴다`() {
        val raw = "토스-원문-RAW-BODY-에러"
        LogCapture().use { logs ->
            listOf(
                handler.handleServiceUnavailable(TossTokenUnavailableException(raw)),
                handler.handleServiceUnavailable(ServiceUnavailableException("TOSS_AUTH_FAILED", raw)),
            ).forEach { response ->
                assertFalse(response.body?.message.orEmpty().contains(raw))
                assertFalse(response.body?.code.orEmpty().contains(raw))
            }

            assertEquals(2, logs.linesOf("GlobalExceptionHandler").count { it.contains(raw) })
        }
    }

    // 토스 J 하위 예외의 code 와 응답 고정 문구, 예외 메시지에 토스 원문이 있어도 응답에는 싣지 않는다 (D15, AE8)
    @ParameterizedTest(name = "{0} 는 503 과 고정 문구로 응답한다")
    @CsvSource(
        "TOSS_AUTH_FAILED, 토스 인증에 실패했습니다. 토스 키 설정을 확인해 주세요.",
        "TOSS_IP_NOT_ALLOWED, 토스증권에 이 서버의 IP 가 허용 IP 로 등록돼 있지 않습니다. 토스증권 설정에서 허용 IP 를 등록한 뒤 다시 시도해 주세요.",
        "TOSS_RATE_LIMITED, 토스 호출 한도를 넘었습니다. 잠시 후 다시 시도해 주세요.",
        "TOSS_MAINTENANCE, 토스증권이 점검 중입니다. 잠시 후 다시 시도해 주세요.",
        "TOSS_UNAVAILABLE, 토스증권을 일시적으로 사용할 수 없습니다. 잠시 후 다시 시도해 주세요.",
        "TOSS_TIMEOUT, 토스증권 응답이 지연되고 있습니다. 잠시 후 다시 시도해 주세요.",
        "TOSS_RESPONSE_INVALID, 토스증권 응답을 해석하지 못했습니다.",
    )
    fun `토스 J 범주 code 는 503 과 code 별 고정 문구로 응답하고 예외 메시지는 싣지 않는다`(code: String, expectedMessage: String) {
        val raw = "토스-원문-RAW-BODY-에러"

        val response = handler.handleServiceUnavailable(ServiceUnavailableException(code, raw))

        assertEquals(503, response.statusCode.value())
        assertEquals(code, response.body?.code)
        assertEquals(expectedMessage, response.body?.message)
        assertFalse(response.body?.message.orEmpty().contains(raw))
    }

    @Test
    fun `토스 J 하위 예외 클래스는 각자 code 를 가지고 범주 J 기반이다`() {
        val expected = mapOf(
            "TOSS_AUTH_FAILED" to TossAuthFailedException("사유"),
            "TOSS_IP_NOT_ALLOWED" to TossIpNotAllowedException("사유"),
            "TOSS_RATE_LIMITED" to TossRateLimitedException("사유"),
            "TOSS_MAINTENANCE" to TossMaintenanceException("사유"),
            "TOSS_UNAVAILABLE" to TossUnavailableException("사유"),
            "TOSS_TIMEOUT" to TossTimeoutException("사유"),
            "TOSS_RESPONSE_INVALID" to TossResponseInvalidException("사유"),
        )

        expected.forEach { (code, exception) ->
            assertEquals(code, exception.errorCode)
            assertEquals(503, handler.handleServiceUnavailable(exception).statusCode.value(), code)
        }
    }

    @Test
    fun `토스가 입력을 거부한 TOSS_REQUEST_REJECTED 는 A 범주 400 이고 토스 원문을 싣지 않는다`() {
        val response = handler.handleApiDomain(TossRequestRejectedException(404))

        assertEquals(400, response.statusCode.value())
        assertEquals("TOSS_REQUEST_REJECTED", response.body?.code)
        assertEquals("토스가 요청을 거부했습니다. 토스 상태=404", response.body?.message)
    }

    @Test
    fun `잡이 처리기는 500 고정 응답을 주고 원인 예외를 error 로그로 한 번 남긴다`() {
        LogCapture().use { logs ->
            val response = handler.handleException(RuntimeException("redis 연결 실패"))

            assertEquals(500, response.statusCode.value())
            assertEquals("INTERNAL_SERVER_ERROR", response.body?.code)
            assertFalse(response.body?.message.orEmpty().contains("redis"))
            val errors = logs.linesOf("GlobalExceptionHandler").filter { it.contains(" ERROR ") }
            assertEquals(1, errors.size, errors.toString())
            assertTrue(errors.single().contains("RuntimeException:redis 연결 실패"), errors.single())
        }
    }

    @Test
    fun `범주 K 하위 예외 TOSS_INTEGRATION_DISABLED 는 500 이고 키 이름을 응답에 싣지 않는다`() {
        val response = handler.handleInternalError(TossIntegrationDisabledException(listOf("TOSS_CLIENT_ID")))

        assertEquals(500, response.statusCode.value())
        assertEquals("TOSS_INTEGRATION_DISABLED", response.body?.code)
        assertFalse(response.body?.message.orEmpty().contains("TOSS_CLIENT_ID"))
    }

    // ---- MVC 디스패치 경유: @ExceptionHandler 등록까지 검증 (S3Q-02) ----

    private val mockMvc: MockMvc = MockMvcBuilders.standaloneSetup(ThrowingController())
        .setControllerAdvice(GlobalExceptionHandler())
        .build()

    @Test
    fun `MVC 경유 범주 K 예외는 catch-all 이 아니라 전용 처리기로 500 과 예외 code 응답`() {
        mockMvc.get("/test/internal-error").andExpect {
            status { isInternalServerError() }
            jsonPath("$.status") { value(500) }
            jsonPath("$.code") { value("SOME_DEFECT") }
            jsonPath("$.message") { value("서버 오류가 발생했습니다.") }
        }
    }

    @Test
    fun `MVC 경유 범주 K 하위 예외도 자기 code 로 500 응답`() {
        mockMvc.get("/test/locked-deposit-insufficient").andExpect {
            status { isInternalServerError() }
            jsonPath("$.code") { value("LOCKED_DEPOSIT_INSUFFICIENT") }
        }
    }

    @Test
    fun `MVC 경유 범주 I 하위 예외는 501 과 예외 code 응답`() {
        mockMvc.get("/test/not-implemented").andExpect {
            status { isNotImplemented() }
            jsonPath("$.status") { value(501) }
            jsonPath("$.code") { value("BROKER_ORDER_NOT_SUPPORTED") }
        }
    }

    @Test
    fun `MVC 경유 범주 J 하위 예외는 catch-all 이 아니라 전용 처리기로 503 과 예외 code 응답`() {
        mockMvc.get("/test/service-unavailable").andExpect {
            status { isServiceUnavailable() }
            jsonPath("$.status") { value(503) }
            jsonPath("$.code") { value("TOSS_TOKEN_UNAVAILABLE") }
        }
    }

    // 시험 전용 컨트롤러, 범주 예외를 그대로 던짐
    @RestController
    class ThrowingController {
        @GetMapping("/test/internal-error")
        fun internalError(): String = throw InternalErrorException("SOME_DEFECT", "잠금 예수금이 부족합니다. accountId=7")

        @GetMapping("/test/locked-deposit-insufficient")
        fun lockedDepositInsufficient(): String = throw LockedDepositInsufficientException()

        @GetMapping("/test/service-unavailable")
        fun serviceUnavailable(): String = throw TossTokenUnavailableException("대기 한도 초과")

        @GetMapping("/test/not-implemented")
        fun notImplemented(): String = throw BrokerOrderNotSupportedException("TOSS_LIVE")
    }
}
