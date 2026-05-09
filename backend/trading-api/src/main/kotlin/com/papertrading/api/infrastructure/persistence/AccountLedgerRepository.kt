package com.papertrading.api.infrastructure.persistence

import com.papertrading.api.domain.entity.account.AccountLedger
import org.springframework.data.jpa.repository.JpaRepository

interface AccountLedgerRepository : JpaRepository<AccountLedger, Long>, AccountLedgerRepositoryCustom {
    fun findByAccountIdOrderByCreatedAtDesc(accountId: Long): List<AccountLedger>
    fun existsByIdempotencyKey(idempotencyKey: String): Boolean
    fun findByAccountIdAndIdempotencyKey(
        accountId: Long,
        idempotencyKey: String,
    ): AccountLedger?
}
