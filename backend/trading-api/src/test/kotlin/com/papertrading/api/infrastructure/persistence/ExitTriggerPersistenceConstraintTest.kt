package com.papertrading.api.infrastructure.persistence

import com.papertrading.api.domain.entity.account.Account
import com.papertrading.api.domain.entity.order.Order
import com.papertrading.api.domain.enums.AccountType
import com.papertrading.api.domain.enums.MarketType
import com.papertrading.api.domain.enums.OrderCondition
import com.papertrading.api.domain.enums.OrderSide
import com.papertrading.api.domain.enums.OrderType
import com.papertrading.api.domain.enums.TradingMode
import com.papertrading.api.domain.position.PositionExitTrigger
import com.querydsl.jpa.impl.JPAQueryFactory
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.test.context.ActiveProfiles
import org.hibernate.exception.ConstraintViolationException
import java.math.BigDecimal

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(ExitTriggerPersistenceConstraintTest.QuerydslTestConfig::class)
class ExitTriggerPersistenceConstraintTest {
    @Autowired
    lateinit var accountRepository: AccountRepository

    @Autowired
    lateinit var orderRepository: OrderRepository

    @Autowired
    lateinit var entityManager: TestEntityManager

    @Test
    fun `position_id unique 제약을 위반하면 예외`() {
        val first = PositionExitTrigger.create(9001L, 1001L, "005930", true, BigDecimal("3"), BigDecimal("7"))
        val second = PositionExitTrigger.create(9001L, 1002L, "005930", true, BigDecimal("4"), BigDecimal("8"))
        entityManager.persistAndFlush(first)

        assertThrows(ConstraintViolationException::class.java) {
            entityManager.persistAndFlush(second)
        }
    }

    @Test
    fun `orders(account_id, idempotency_key) unique 제약을 위반하면 예외`() {
        val account = accountRepository.saveAndFlush(
            Account.create("constraint-test", AccountType.STOCK, TradingMode.LOCAL, BigDecimal("100000"))
        )
        val key = "auto-exit:10:1:STOP_LOSS"

        val first = Order.create(
            account = account,
            ticker = "005930",
            marketType = MarketType.KOSPI,
            orderType = OrderType.MARKET,
            orderSide = OrderSide.SELL,
            orderCondition = OrderCondition.DAY,
            quantity = BigDecimal("1"),
            idempotencyKey = key,
        )
        val second = Order.create(
            account = account,
            ticker = "005930",
            marketType = MarketType.KOSPI,
            orderType = OrderType.MARKET,
            orderSide = OrderSide.SELL,
            orderCondition = OrderCondition.DAY,
            quantity = BigDecimal("1"),
            idempotencyKey = key,
        )

        orderRepository.saveAndFlush(first)

        assertThrows(DataIntegrityViolationException::class.java) {
            orderRepository.saveAndFlush(second)
        }
    }

    @TestConfiguration
    class QuerydslTestConfig {
        @Bean
        fun jpaQueryFactory(entityManager: EntityManager): JPAQueryFactory = JPAQueryFactory(entityManager)
    }
}
