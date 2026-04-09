package com.papertrading.api.application.account

import com.papertrading.api.application.account.command.UpsertRiskPolicyCommand
import com.papertrading.api.domain.enums.AccountType
import com.papertrading.api.domain.enums.TradingMode
import com.papertrading.api.domain.model.Account
import com.papertrading.api.domain.model.RiskPolicy
import com.papertrading.api.infrastructure.persistence.AccountRepository
import com.papertrading.api.infrastructure.persistence.RiskPolicyRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.util.Optional

class RiskPolicyServiceTest {

    private val accountRepository = mockk<AccountRepository>()
    private val riskPolicyRepository = mockk<RiskPolicyRepository>()
    private lateinit var service: RiskPolicyService

    private val account = Account.create("테스트", AccountType.STOCK, TradingMode.LOCAL, BigDecimal.ZERO)

    @BeforeEach
    fun setUp() {
        service = RiskPolicyService(accountRepository, riskPolicyRepository)
    }

    @Test
    fun `리스크_정책이_없는_계좌에_처음_등록하면_정책이_생성된다`() {
        val command = UpsertRiskPolicyCommand(
            maxPositionRatio = BigDecimal("0.2"),
            maxDailyLoss = BigDecimal("100000"),
            maxOrderAmount = BigDecimal("500000")
        )
        every { accountRepository.findByIdWithLock(1L) } returns Optional.of(account)
        every { riskPolicyRepository.findByAccountIdAndIsActiveTrue(1L) } returns Optional.empty()
        every { riskPolicyRepository.save(any()) } answers { firstArg() }

        val result = service.upsertRiskPolicy(1L, command)

        assertThat(result.maxPositionRatio).isEqualByComparingTo("0.2")
        assertThat(result.isActive).isTrue()
        verify(exactly = 1) { riskPolicyRepository.save(any()) }
    }

    @Test
    fun `기존_활성_정책이_있으면_비활성화하고_새_정책을_생성한다`() {
        val existingPolicy = RiskPolicy(
            id = 1L,
            account = account,
            maxPositionRatio = BigDecimal("0.1"),
            isActive = true
        )
        val command = UpsertRiskPolicyCommand(
            maxPositionRatio = BigDecimal("0.3"),
            maxDailyLoss = null,
            maxOrderAmount = null
        )

        every { accountRepository.findByIdWithLock(1L) } returns Optional.of(account)
        every { riskPolicyRepository.findByAccountIdAndIsActiveTrue(1L) } returns Optional.of(existingPolicy)
        every { riskPolicyRepository.save(any()) } answers { firstArg() }

        service.upsertRiskPolicy(1L, command)

        assertThat(existingPolicy.isActive).isFalse()
        verify(exactly = 1) { riskPolicyRepository.save(any()) }
    }

    @Test
    fun `활성_리스크_정책을_조회한다`() {
        val policy = RiskPolicy(id = 1L, account = account, maxPositionRatio = BigDecimal("0.2"), isActive = true)
        every { riskPolicyRepository.findByAccountIdAndIsActiveTrue(1L) } returns Optional.of(policy)

        val result = service.getActiveRiskPolicy(1L)

        assertThat(result.maxPositionRatio).isEqualByComparingTo("0.2")
    }

    @Test
    fun `활성_정책이_없으면_조회시_예외를_던진다`() {
        every { riskPolicyRepository.findByAccountIdAndIsActiveTrue(99L) } returns Optional.empty()

        assertThatThrownBy { service.getActiveRiskPolicy(99L) }
            .isInstanceOf(NoSuchElementException::class.java)
    }

    @Test
    fun `잘못된_maxPositionRatio로_등록하면_도메인_예외를_던진다`() {
        val command = UpsertRiskPolicyCommand(
            maxPositionRatio = BigDecimal("1.5"),
            maxDailyLoss = null,
            maxOrderAmount = null
        )
        every { accountRepository.findByIdWithLock(1L) } returns Optional.of(account)
        every { riskPolicyRepository.findByAccountIdAndIsActiveTrue(1L) } returns Optional.empty()

        assertThatThrownBy { service.upsertRiskPolicy(1L, command) }
            .isInstanceOf(IllegalArgumentException::class.java)
    }
}