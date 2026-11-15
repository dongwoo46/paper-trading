package com.papertrading.api.application.toss

import com.papertrading.api.application.toss.port.TossAccountListPort
import com.papertrading.api.application.toss.port.TossAccountSummary
import com.papertrading.api.common.exception.TossIpNotAllowedException
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

// 토스 계좌 목록 조회 유스케이스: 포트 결과를 그대로 전달, 실패는 바꾸지 않음
class TossAccountListQueryServiceTest {

    private val port = mockk<TossAccountListPort>()
    private val service = TossAccountListQueryService(port)

    @Test
    fun `포트가 준 계좌 목록을 그대로 돌려준다`() {
        val accounts = listOf(
            TossAccountSummary("8901", 1L, "BROKERAGE"),
            TossAccountSummary("2100", 9_876_543_210L, "PENSION_SAVINGS"),
        )
        every { port.fetchAccounts() } returns accounts

        assertEquals(accounts, service.listAccounts())
    }

    @Test
    fun `계좌가 없으면 빈 목록이다`() {
        every { port.fetchAccounts() } returns emptyList()

        assertEquals(emptyList<TossAccountSummary>(), service.listAccounts())
    }

    @Test
    fun `포트 예외는 바꾸지 않고 그대로 올린다`() {
        every { port.fetchAccounts() } throws TossIpNotAllowedException("403")

        assertThrows<TossIpNotAllowedException> { service.listAccounts() }
    }
}
