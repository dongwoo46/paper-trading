package com.papertrading.collector.infra.kis.persistence

import com.papertrading.collector.domain.entity.kis.KisAccessTokenCache
import org.springframework.data.jpa.repository.JpaRepository

interface KisAccessTokenCacheRepository : JpaRepository<KisAccessTokenCache, Long> {
    fun findByMode(mode: String): KisAccessTokenCache?
}

