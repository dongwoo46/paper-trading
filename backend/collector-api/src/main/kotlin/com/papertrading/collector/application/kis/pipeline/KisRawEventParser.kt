package com.papertrading.collector.application.kis.pipeline

import com.papertrading.collector.domain.kis.KisQuoteEvent
import mu.KotlinLogging
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.time.Instant

/**
 * KIS WebSocket H0STCNT0(실시간 체결가) raw 메시지 파서
 *
 * 메시지 포맷: {flag}|{tr_id}|{count}|{^구분 데이터}
 * - flag '0'/'1' = 실시간 데이터. 그 외(JSON) = 시스템 메시지.
 *
 * H0STCNT0 사용 필드만 파싱. 나머지는 버림.
 * [0]  MKSC_SHRN_ISCD  종목코드
 * [2]  STCK_PRPR       현재가
 * [8]  STCK_HGPR       고가
 * [9]  STCK_LWPR       저가
 * [10] ASKP1           매도호가1
 * [11] BIDP1           매수호가1
 * [12] CNTG_VOL        체결거래량
 */
@Component
class KisRawEventParser {

    private val log = KotlinLogging.logger {}

    companion object {
        private const val TR_ID_CONTRACT = "H0STCNT0"
        private const val IDX_TICKER = 0
        private const val IDX_PRICE = 2
        private const val IDX_HIGH = 8
        private const val IDX_LOW = 9
        private const val IDX_ASKP1 = 10
        private const val IDX_BIDP1 = 11
        private const val IDX_VOLUME = 12
        private const val MIN_FIELD_COUNT = 13
    }

    fun parse(payload: String): KisQuoteEvent? {
        if (payload.isEmpty() || payload[0] !in ('0'..'1')) return null

        val parts = payload.split("|", limit = 4)
        if (parts.size < 4) return null
        if (parts[1] != TR_ID_CONTRACT) return null

        val fields = parts[3].split("^")
        if (fields.size < MIN_FIELD_COUNT) {
            log.warn { "H0STCNT0 필드 수 부족: expected>=$MIN_FIELD_COUNT, got=${fields.size}" }
            return null
        }

        return try {
            KisQuoteEvent(
                ticker = fields[IDX_TICKER],
                price = BigDecimal(fields[IDX_PRICE]),
                high = BigDecimal(fields[IDX_HIGH]),
                low = BigDecimal(fields[IDX_LOW]),
                askp1 = BigDecimal(fields[IDX_ASKP1]),
                bidp1 = BigDecimal(fields[IDX_BIDP1]),
                volume = BigDecimal(fields[IDX_VOLUME]),
                receivedAt = Instant.now(),
            )
        } catch (e: NumberFormatException) {
            log.warn { "H0STCNT0 숫자 파싱 오류: ticker=${fields.getOrNull(IDX_TICKER)}, ${e.message}" }
            null
        }
    }
}