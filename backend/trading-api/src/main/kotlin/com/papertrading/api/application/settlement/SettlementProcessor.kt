package com.papertrading.api.application.settlement

import com.papertrading.api.domain.enums.TransactionType
import com.papertrading.api.domain.model.AccountLedger
import com.papertrading.api.domain.model.PendingSettlement
import com.papertrading.api.infrastructure.persistence.AccountLedgerRepository
import com.papertrading.api.infrastructure.persistence.AccountRepository
import com.papertrading.api.infrastructure.persistence.PendingSettlementRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.math.RoundingMode

/**
 * 단건 정산 처리 빈.
 *
 * REQUIRES_NEW 전파로 각 정산을 독립 트랜잭션에서 처리한다.
 * 이를 통해 배치 중 한 건의 DB 예외(예: ConstraintViolationException)가
 * EntityManager를 rollback-only로 오염시켜도 나머지 건이 영향받지 않는다.
 * 자기 호출(self-invocation)에서는 프록시를 통하지 않으므로 별도 빈으로 분리한다.
 */
@Service
class SettlementProcessor(
    private val pendingSettlementRepository: PendingSettlementRepository,
    private val accountRepository: AccountRepository,
    private val accountLedgerRepository: AccountLedgerRepository,
) {
    /**
     * 단건 정산을 독립 트랜잭션에서 원자적으로 수행한다.
     *
     * 1. 비관적 락으로 계좌 조회
     * 2. account.receiveSellProceeds(amount) — 예수금 증가
     * 3. accountRepository.save(account) — REQUIRES_NEW 범위에서 명시적 영속
     * 4. pendingSettlement.complete()
     * 5. AccountLedger(SETTLEMENT) 저장
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun processOne(ps: PendingSettlement) {
        val accountId = requireNotNull(ps.account?.id) { "account.id is null: pendingSettlementId=${ps.id}" }
        val account = accountRepository.findByIdWithLock(accountId)
            .orElseThrow { NoSuchElementException("계좌를 찾을 수 없습니다. accountId=$accountId") }

        val amount = ps.amount.setScale(4, RoundingMode.HALF_UP)
        account.receiveSellProceeds(amount)
        accountRepository.save(account)

        ps.complete()
        pendingSettlementRepository.save(ps)

        accountLedgerRepository.save(
            AccountLedger(
                account = account,
                transactionType = TransactionType.SETTLEMENT,
                amount = amount,
                balanceAfter = account.availableDeposit,
                idempotencyKey = "settlement-${ps.id}",
            )
        )
    }
}
