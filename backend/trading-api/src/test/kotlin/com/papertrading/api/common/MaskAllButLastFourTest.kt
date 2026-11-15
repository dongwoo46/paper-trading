package com.papertrading.api.common

import com.papertrading.api.application.account.port.BrokerAccountRef
import com.papertrading.api.application.toss.port.TossAccountSummary
import com.papertrading.api.domain.enums.TradingMode
import com.papertrading.api.infrastructure.toss.TossApiRequest
import com.papertrading.api.infrastructure.toss.TossRateLimitGroup
import com.papertrading.api.infrastructure.toss.dto.TossAccount
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

// S6-05: 끝 4자리 마스킹은 한 함수, 4자리 이하는 전부 가리고 네 값 표현이 그 함수만 씀 (RULE-05)
class MaskAllButLastFourTest {

    @ParameterizedTest(name = "{0} 는 {1}")
    @CsvSource(
        "1234567890, ****7890",
        "12345, ****2345",
        "1234, ****",
        "12, ****",
        "1, ****",
    )
    fun `끝 4자리만 남기고 4자리 이하는 전부 가린다`(value: String, expected: String) {
        assertEquals(expected, maskAllButLastFour(value))
    }

    @Test
    fun `빈 문자열은 전부 가리고 null 은 none 이다`() {
        assertEquals("****", maskAllButLastFour(""))
        assertEquals("none", maskAllButLastFour(null))
    }

    @Test
    fun `TossApiRequest 는 accountSeq 를 함수 결과로만 보인다`() {
        val request = TossApiRequest("/api/v1/holdings", TossRateLimitGroup.ASSET, accountSeq = "1234567890")
        val shortSeq = TossApiRequest("/api/v1/holdings", TossRateLimitGroup.ASSET, accountSeq = "42")
        val noSeq = TossApiRequest("/api/v1/accounts", TossRateLimitGroup.ACCOUNT)

        assertEquals("TossApiRequest(path=/api/v1/holdings, group=ASSET, accountSeq=****7890)", request.toString())
        assertEquals("TossApiRequest(path=/api/v1/holdings, group=ASSET, accountSeq=****)", shortSeq.toString())
        assertEquals("TossApiRequest(path=/api/v1/accounts, group=ACCOUNT, accountSeq=none)", noSeq.toString())
    }

    @Test
    fun `BrokerAccountRef 는 4자리 이하 accountSeq 를 전부 가린다`() {
        val long = BrokerAccountRef(1L, TradingMode.TOSS_LIVE, "1234567890")
        val short = BrokerAccountRef(2L, TradingMode.TOSS_LIVE, "42")

        assertEquals("BrokerAccountRef(accountId=1, seq=****7890)", long.toString())
        assertEquals("BrokerAccountRef(accountId=2, seq=****)", short.toString())
        assertFalse(short.toString().contains("42"), short.toString())
    }

    @Test
    fun `TossAccount 는 accountSeq 전체와 accountNo 를 싣지 않는다`() {
        val account = TossAccount("12345678901", 9_876_543_210L, "BROKERAGE")
        val shortSeq = TossAccount("12345678901", 7L, "BROKERAGE")

        assertEquals("TossAccount(accountSeq=****3210, accountType=BROKERAGE)", account.toString())
        assertEquals("TossAccount(accountSeq=****, accountType=BROKERAGE)", shortSeq.toString())
        assertFalse(account.toString().contains("12345678901"), account.toString())
    }

    @Test
    fun `TossAccountSummary 는 accountSeq 를 가리고 끝 4자리 accountNo 는 그대로 보인다`() {
        val summary = TossAccountSummary("8901", 9_876_543_210L, "BROKERAGE")

        assertEquals(
            "TossAccountSummary(accountNoLast4=8901, accountSeq=****3210, accountType=BROKERAGE)",
            summary.toString(),
        )
        assertFalse(summary.toString().contains("9876543210"), summary.toString())
    }
}
