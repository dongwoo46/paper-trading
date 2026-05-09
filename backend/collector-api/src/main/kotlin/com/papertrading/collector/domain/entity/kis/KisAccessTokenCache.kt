package com.papertrading.collector.domain.entity.kis

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.UpdateTimestamp
import java.time.LocalDateTime

@Entity
@Table(name = "kis_access_token_cache")
data class KisAccessTokenCache(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    @Column(nullable = false, unique = true, length = 16)
    var mode: String,
    @Column(nullable = false, length = 2048)
    var token: String,
    @Column(name = "expires_at", nullable = false)
    var expiresAt: LocalDateTime,
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime? = null,
)

