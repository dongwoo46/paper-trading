package com.papertrading.collector.presentation.internal

import com.papertrading.collector.application.kis.service.KisSubscriptionOrchestrator
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * 내부 서비스 전용 구독 API (trading-api → collector-api)
 *
 * trading-api 주문/포지션 생성 시 종목 자동 구독 요청.
 * 구독은 (mode, ticker) 단위. 여러 계좌가 같은 종목을 거래해도 구독은 1개만 유지.
 *
 * mode 매핑 (trading-api 측에서 변환):
 *   KIS_PAPER → paper
 *   KIS_LIVE  → live
 *   LOCAL     → live  (실전 시세 기반 시뮬레이션, KIS_LIVE와 구독 공유)
 */
@RestController
@RequestMapping("/api/internal/subscriptions")
class InternalSubscriptionController(
    private val orchestrator: KisSubscriptionOrchestrator,
) {
    @PostMapping("/{ticker}")
    fun subscribe(
        @PathVariable ticker: String,
        @RequestParam mode: String,
    ): ResponseEntity<Map<String, String>> {
        val status = orchestrator.subscribe(mode, ticker)
        return ResponseEntity.ok(mapOf("ticker" to ticker, "mode" to mode, "status" to status.name))
    }

    @DeleteMapping("/{ticker}")
    fun unsubscribe(
        @PathVariable ticker: String,
        @RequestParam mode: String,
    ): ResponseEntity<Map<String, String>> {
        val status = orchestrator.unsubscribe(mode, ticker)
        return ResponseEntity.ok(mapOf("ticker" to ticker, "mode" to mode, "status" to status.name))
    }
}
