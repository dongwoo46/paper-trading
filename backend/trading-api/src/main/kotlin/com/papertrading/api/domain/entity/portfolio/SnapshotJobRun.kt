package com.papertrading.api.domain.entity.portfolio

import com.papertrading.api.domain.entity.account.Account
import com.papertrading.api.domain.entity.base.BaseAuditEntity
import com.papertrading.api.domain.enums.SnapshotJobRunStatus
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
@Table(name = "snapshot_job_runs")
class SnapshotJobRun protected constructor() : BaseAuditEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    lateinit var account: Account
        private set

    @Column(name = "business_date", nullable = false)
    lateinit var businessDate: java.time.LocalDate
        private set

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    lateinit var status: SnapshotJobRunStatus
        private set

    @Column(name = "started_at", nullable = false)
    lateinit var startedAt: Instant
        private set

    @Column(name = "finished_at")
    var finishedAt: Instant? = null
        private set

    @Column(name = "error_message", length = 500)
    var errorMessage: String? = null
        private set

    fun complete() {
        status = SnapshotJobRunStatus.SUCCESS
        finishedAt = Instant.now()
    }

    fun fail(message: String?) {
        status = SnapshotJobRunStatus.FAILED
        finishedAt = Instant.now()
        errorMessage = message?.take(500)
    }

    companion object {
        fun start(account: Account, businessDate: java.time.LocalDate): SnapshotJobRun = SnapshotJobRun().apply {
            this.account = account
            this.businessDate = businessDate
            this.status = SnapshotJobRunStatus.RUNNING
            this.startedAt = Instant.now()
        }
    }
}

