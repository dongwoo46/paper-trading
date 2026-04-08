package com.papertrading.api.domain.model

import com.papertrading.api.domain.model.base.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

@Entity
@Table(name = "notifications")
class Notification(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    var account: Account? = null,

    @Column(name = "notification_type", nullable = false, length = 50)
    var notificationType: String? = null,

    @Column(name = "title", nullable = false, length = 200)
    var title: String? = null,

    @Column(name = "message", nullable = false, columnDefinition = "text")
    var message: String? = null,

    @Column(name = "is_read", nullable = false)
    var isRead: Boolean = false,

    @Column(name = "ref_order_id")
    var refOrderId: Long? = null,

    @Column(name = "ref_execution_id")
    var refExecutionId: Long? = null
) : BaseTimeEntity() {
    fun markAsRead() {
        isRead = true
    }
}
