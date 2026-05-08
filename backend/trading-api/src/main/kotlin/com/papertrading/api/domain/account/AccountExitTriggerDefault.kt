package com.papertrading.api.domain.account

import com.papertrading.api.domain.entity.base.BaseAuditEntity
import jakarta.persistence.*
import java.math.BigDecimal

@Entity
@Table(name = "account_exit_trigger_defaults")
class AccountExitTriggerDefault protected constructor() : BaseAuditEntity() {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
    @Column(name = "account_id", nullable = false, unique = true)
    var accountId: Long = 0
    @Column(name = "enabled", nullable = false)
    var enabled: Boolean = false
    @Column(name = "stop_loss_percent", precision = 8, scale = 4)
    var stopLossPercent: BigDecimal? = null
    @Column(name = "take_profit_percent", precision = 8, scale = 4)
    var takeProfitPercent: BigDecimal? = null
    @Version
    @Column(name = "version", nullable = false)
    var version: Long = 0

    fun validate() {
        fun checkPercent(v: BigDecimal?) {
            if (v != null) require(v > BigDecimal.ZERO && v < BigDecimal("100")) { "percent must be in (0,100)" }
        }
        checkPercent(stopLossPercent)
        checkPercent(takeProfitPercent)
        if (enabled) require(stopLossPercent != null || takeProfitPercent != null) { "enabled trigger needs at least one percent" }
    }

    companion object {
        fun create(
            accountId: Long,
            enabled: Boolean,
            stopLossPercent: BigDecimal?,
            takeProfitPercent: BigDecimal?,
        ): AccountExitTriggerDefault =
            AccountExitTriggerDefault().apply {
                this.accountId = accountId
                this.enabled = enabled
                this.stopLossPercent = stopLossPercent
                this.takeProfitPercent = takeProfitPercent
                validate()
            }
    }
}
