package com.papertrading.api.domain.entity.account

import com.papertrading.api.domain.entity.base.BaseAuditEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.Version
import java.math.BigDecimal

@Entity
@Table(name = "account_exit_trigger_defaults")
class AccountExitTriggerDefault protected constructor() : BaseAuditEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
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
            }
    }
}
