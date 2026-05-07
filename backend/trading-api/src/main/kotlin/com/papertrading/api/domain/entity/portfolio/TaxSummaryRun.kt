package com.papertrading.api.domain.entity.portfolio

import com.papertrading.api.domain.entity.account.Account
import com.papertrading.api.domain.entity.base.BaseTimeEntity
import com.papertrading.api.domain.enums.TaxSummaryRunStatus
import com.papertrading.api.domain.enums.TaxSummaryRunType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "tax_summary_runs")
class TaxSummaryRun protected constructor() : BaseTimeEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    lateinit var account: Account
        private set

    @Column(name = "tax_year", nullable = false)
    var taxYear: Int = 0
        private set

    @Enumerated(EnumType.STRING)
    @Column(name = "run_type", nullable = false, length = 20)
    lateinit var runType: TaxSummaryRunType
        private set

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    var status: TaxSummaryRunStatus = TaxSummaryRunStatus.RUNNING
        private set

    @Column(name = "started_at", nullable = false)
    var startedAt: Instant = Instant.now()
        private set

    @Column(name = "finished_at")
    var finishedAt: Instant? = null
        private set

    @Column(name = "error_message", length = 500)
    var errorMessage: String? = null
        private set

    fun completeSuccess() {
        status = TaxSummaryRunStatus.SUCCESS
        finishedAt = Instant.now()
        errorMessage = null
    }

    fun fail(message: String?) {
        status = TaxSummaryRunStatus.FAILED
        finishedAt = Instant.now()
        errorMessage = message
    }

    companion object {
        fun start(account: Account, taxYear: Int, runType: TaxSummaryRunType): TaxSummaryRun =
            TaxSummaryRun().apply {
                this.account = account
                this.taxYear = taxYear
                this.runType = runType
                this.status = TaxSummaryRunStatus.RUNNING
                this.startedAt = Instant.now()
            }
    }
}
