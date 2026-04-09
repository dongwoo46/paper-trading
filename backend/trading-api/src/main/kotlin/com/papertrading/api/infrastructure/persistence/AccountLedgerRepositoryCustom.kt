package com.papertrading.api.infrastructure.persistence

import com.papertrading.api.application.account.query.LedgerFilter
import com.papertrading.api.application.account.result.LedgerResult
import org.springframework.data.domain.Page

interface AccountLedgerRepositoryCustom {
    fun findLedgers(accountId: Long, filter: LedgerFilter): Page<LedgerResult>
}