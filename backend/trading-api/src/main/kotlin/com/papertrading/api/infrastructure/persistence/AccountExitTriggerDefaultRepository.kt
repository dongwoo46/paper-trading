package com.papertrading.api.infrastructure.persistence

import com.papertrading.api.domain.entity.account.AccountExitTriggerDefault
import org.springframework.data.jpa.repository.JpaRepository

interface AccountExitTriggerDefaultRepository : JpaRepository<AccountExitTriggerDefault, Long> {
    fun findByAccountId(accountId: Long): AccountExitTriggerDefault?
}

