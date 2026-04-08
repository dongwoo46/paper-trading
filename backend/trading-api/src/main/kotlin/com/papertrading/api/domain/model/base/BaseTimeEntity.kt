package com.papertrading.api.domain.model.base

import jakarta.persistence.Column
import jakarta.persistence.MappedSuperclass
import org.hibernate.annotations.CreationTimestamp
import java.time.Instant

@MappedSuperclass
abstract class BaseTimeEntity {
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant? = null
        protected set
}
