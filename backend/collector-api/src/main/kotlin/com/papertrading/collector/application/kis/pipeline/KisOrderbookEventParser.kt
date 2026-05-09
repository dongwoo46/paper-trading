package com.papertrading.collector.application.kis.pipeline

import com.papertrading.collector.domain.entity.kis.KisOrderbookEvent
import mu.KotlinLogging
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.time.Instant

/**
 * KIS WebSocket H0STASP0(실시간 호가잔량) raw 메시지 파서
 *
 * 메시지 포맷: {flag}|{tr_id}|{count}|{^구분 데이터}
 * - flag '0'/'1' = 실시간 데이터. 그 외(JSON) = 시스템 메시지.
 * - TR_ID must be exactly "H0STASP0".
 *
 * H0STASP0 사용 필드 (최소 38개):
 * [0]      MKSC_SHRN_ISCD  — 종목코드
 * [3..7]   ASKP1..ASKP5    — 매도호가 (best ask first, ascending)
 * [13..17] BIDP1..BIDP5    — 매수호가 (best bid first, descending)
 * [23..27] ASKP_RSQN1..5   — 매도잔량
 * [33..37] BIDP_RSQN1..5   — 매수잔량
 */
@Component
class KisOrderbookEventParser {

    private val log = KotlinLogging.logger {}

    companion object {
        private const val TR_ID_ORDERBOOK = "H0STASP0"
        private const val MIN_FIELD_COUNT = 38
        private const val IDX_TICKER = 0
        private const val IDX_ASKP_START = 3
        private const val IDX_BIDP_START = 13
        private const val IDX_ASKP_RSQN_START = 23
        private const val IDX_BIDP_RSQN_START = 33
        private const val DEPTH_LEVELS = 5
    }

    fun parse(payload: String): KisOrderbookEvent? {
        if (payload.isEmpty() || payload[0] !in ('0'..'1')) return null

        val parts = payload.split("|", limit = 4)
        if (parts.size < 2) return null
        if (parts[1] != TR_ID_ORDERBOOK) return null

        if (parts.size < 4) return null

        val fields = parts[3].split("^")
        if (fields.size < MIN_FIELD_COUNT) {
            val ticker = fields.getOrNull(IDX_TICKER)
            log.warn { "H0STASP0 필드 수 부족: expected>=$MIN_FIELD_COUNT, got=${fields.size}, ticker=$ticker" }
            return null
        }

        val ticker = fields[IDX_TICKER]
        return try {
            KisOrderbookEvent(
                ticker = ticker,
                askPrices = (0 until DEPTH_LEVELS).map { i -> BigDecimal(fields[IDX_ASKP_START + i]) },
                bidPrices = (0 until DEPTH_LEVELS).map { i -> BigDecimal(fields[IDX_BIDP_START + i]) },
                askQtys   = (0 until DEPTH_LEVELS).map { i -> BigDecimal(fields[IDX_ASKP_RSQN_START + i]) },
                bidQtys   = (0 until DEPTH_LEVELS).map { i -> BigDecimal(fields[IDX_BIDP_RSQN_START + i]) },
                receivedAt = Instant.now(),
            )
        } catch (e: NumberFormatException) {
            log.warn { "H0STASP0 숫자 파싱 오류: ticker=$ticker, ${e.message}" }
            null
        }
    }
}
