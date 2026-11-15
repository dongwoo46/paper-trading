package com.papertrading.api.presentation.exception

import com.papertrading.api.common.exception.BrokerOrderNotSupportedException
import com.papertrading.api.common.exception.InternalErrorException
import com.papertrading.api.common.exception.LockedDepositInsufficientException
import com.papertrading.api.common.exception.NotImplementedException
import com.papertrading.api.common.exception.ServiceUnavailableException
import com.papertrading.api.common.exception.TossIntegrationDisabledException
import com.papertrading.api.common.exception.TossTokenUnavailableException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
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

    @Test
    fun `범주 J 예외는 503 과 예외 code 와 메시지로 응답한다`() {
        val response = handler.handleServiceUnavailable(
            ServiceUnavailableException("SOME_UPSTREAM", "잠시 뒤 다시 시도해 주세요."),
        )

        assertEquals(503, response.statusCode.value())
        assertEquals(503, response.body?.status)
        assertEquals("SOME_UPSTREAM", response.body?.code)
        assertEquals("잠시 뒤 다시 시도해 주세요.", response.body?.message)
    }

    @Test
    fun `범주 J 하위 예외 TOSS_TOKEN_UNAVAILABLE 도 503 으로 응답한다`() {
        val response = handler.handleServiceUnavailable(TossTokenUnavailableException("대기 한도 초과"))

        assertEquals(503, response.statusCode.value())
        assertEquals("TOSS_TOKEN_UNAVAILABLE", response.body?.code)
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
