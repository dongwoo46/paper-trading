package com.papertrading.api.domain.model.base

import jakarta.persistence.Column
import jakarta.persistence.MappedSuperclass
import org.hibernate.annotations.UpdateTimestamp
import java.time.Instant

@MappedSuperclass
abstract class BaseAuditEntity : BaseTimeEntity() {
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant? = null
        protected set
}
