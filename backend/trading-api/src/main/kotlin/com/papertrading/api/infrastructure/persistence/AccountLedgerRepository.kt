package com.papertrading.api.infrastructure.persistence

import com.papertrading.api.domain.model.AccountLedger
import org.springframework.data.jpa.repository.JpaRepository

interface AccountLedgerRepository : JpaRepository<AccountLedger, Long>, AccountLedgerRepositoryCustom {
    fun findByAccountIdOrderByCreatedAtDesc(accountId: Long): List<AccountLedger>
    fun existsByIdempotencyKey(idempotencyKey: String): Boolean
    fun findByIdempotencyKey(idempotencyKey: String): AccountLedger?
}
