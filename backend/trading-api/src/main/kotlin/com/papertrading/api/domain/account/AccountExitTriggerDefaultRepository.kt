package com.papertrading.api.domain.account

import org.springframework.data.jpa.repository.JpaRepository

interface AccountExitTriggerDefaultRepository : JpaRepository<AccountExitTriggerDefault, Long> {
    fun findByAccountId(accountId: Long): AccountExitTriggerDefault?
}

