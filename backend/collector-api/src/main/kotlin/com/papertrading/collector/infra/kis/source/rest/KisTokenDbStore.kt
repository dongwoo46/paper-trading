package com.papertrading.collector.infra.kis.source.rest

import com.papertrading.collector.domain.entity.kis.KisAccessTokenCache
import com.papertrading.collector.infra.kis.persistence.KisAccessTokenCacheRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

@Component
class KisTokenDbStore(
    private val repository: KisAccessTokenCacheRepository,
) {
    fun findValidToken(mode: String): String? {
        return findValid(mode)?.token
    }

    fun findValid(mode: String): DbToken? {
        val row = repository.findByMode(mode) ?: return null
        if (!row.expiresAt.isAfter(LocalDateTime.now())) return null
        return DbToken(
            token = row.token,
            expiresAt = row.expiresAt.atZone(ZoneId.systemDefault()).toInstant(),
        )
    }

    @Transactional
    fun save(mode: String, token: String, expiresAt: Instant) {
        val expiresAtLocal = LocalDateTime.ofInstant(expiresAt, ZoneId.systemDefault())
        val row = repository.findByMode(mode)
        if (row == null) {
            repository.save(
                KisAccessTokenCache(
                    mode = mode,
                    token = token,
                    expiresAt = expiresAtLocal,
                ),
            )
            return
        }
        row.token = token
        row.expiresAt = expiresAtLocal
        repository.save(row)
    }
}

data class DbToken(
    val token: String,
    val expiresAt: Instant,
)
