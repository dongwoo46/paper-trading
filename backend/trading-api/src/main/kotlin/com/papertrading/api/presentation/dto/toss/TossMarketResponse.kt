package com.papertrading.api.presentation.dto.toss

import com.papertrading.api.application.toss.port.TossCandle
import com.papertrading.api.application.toss.port.TossCandlePage
import com.papertrading.api.application.toss.port.TossOrderbook
import com.papertrading.api.application.toss.port.TossOrderbookEntry
import com.papertrading.api.application.toss.port.TossPrice

/**
 * 토스 시세 응답, 가격과 수량은 JSON 문자열(toPlainString, FLAG-10) — 새 API 에만 적용
 * 시각은 토스 원문 문자열 그대로(사람 결정 2026-11-15), 목록 순서도 토스 그대로
 */
data class TossPriceResponse(
    val symbol: String,
    val timestamp: String?,
    val lastPrice: String,
    val currency: String,
) {
    companion object {
        fun from(price: TossPrice) = TossPriceResponse(
            symbol = price.symbol,
            timestamp = price.timestamp,
            lastPrice = price.lastPrice.toPlainString(),
            currency = price.currency,
        )
    }
}

data class TossOrderbookResponse(
    val timestamp: String?,
    val currency: String,
    val asks: List<TossOrderbookEntryResponse>,
    val bids: List<TossOrderbookEntryResponse>,
) {
    companion object {
        fun from(orderbook: TossOrderbook) = TossOrderbookResponse(
            timestamp = orderbook.timestamp,
            currency = orderbook.currency,
            asks = orderbook.asks.map(TossOrderbookEntryResponse::from),
            bids = orderbook.bids.map(TossOrderbookEntryResponse::from),
        )
    }
}

data class TossOrderbookEntryResponse(val price: String, val volume: String) {
    companion object {
        fun from(entry: TossOrderbookEntry) = TossOrderbookEntryResponse(
            price = entry.price.toPlainString(),
            volume = entry.volume.toPlainString(),
        )
    }
}

data class TossCandlePageResponse(
    val candles: List<TossCandleResponse>,
    val nextBefore: String?,
) {
    companion object {
        fun from(page: TossCandlePage) = TossCandlePageResponse(
            candles = page.candles.map(TossCandleResponse::from),
            nextBefore = page.nextBefore,
        )
    }
}

data class TossCandleResponse(
    val timestamp: String,
    val openPrice: String,
    val highPrice: String,
    val lowPrice: String,
    val closePrice: String,
    val volume: String,
    val currency: String,
) {
    companion object {
        fun from(candle: TossCandle) = TossCandleResponse(
            timestamp = candle.timestamp,
            openPrice = candle.openPrice.toPlainString(),
            highPrice = candle.highPrice.toPlainString(),
            lowPrice = candle.lowPrice.toPlainString(),
            closePrice = candle.closePrice.toPlainString(),
            volume = candle.volume.toPlainString(),
            currency = candle.currency,
        )
    }
}
