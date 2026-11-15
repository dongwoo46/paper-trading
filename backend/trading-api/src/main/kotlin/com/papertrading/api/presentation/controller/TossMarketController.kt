package com.papertrading.api.presentation.controller

import com.papertrading.api.application.toss.TossMarketQueryService
import com.papertrading.api.presentation.dto.toss.TossCandlePageResponse
import com.papertrading.api.presentation.dto.toss.TossOrderbookResponse
import com.papertrading.api.presentation.dto.toss.TossPriceResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

// 모든 파라미터를 문자열로 받아 서비스가 한 곳에서 검증하고 400 INVALID_MARKET_QUERY 로 거부, 없는 파라미터는 null
@RestController
@RequestMapping("/api/toss/market")
class TossMarketController(
    private val tossMarketQueryService: TossMarketQueryService,
) {
    @GetMapping("/prices")
    fun prices(@RequestParam(required = false) symbols: String?): List<TossPriceResponse> {
        val prices = tossMarketQueryService.getPrices(symbols)
        return prices.map(TossPriceResponse::from)
    }

    @GetMapping("/orderbook")
    fun orderbook(@RequestParam(required = false) symbol: String?): TossOrderbookResponse {
        val orderbook = tossMarketQueryService.getOrderbook(symbol)
        return TossOrderbookResponse.from(orderbook)
    }

    @GetMapping("/candles")
    fun candles(
        @RequestParam(required = false) symbol: String?,
        @RequestParam(required = false) interval: String?,
        @RequestParam(required = false) count: String?,
        @RequestParam(required = false) before: String?,
        @RequestParam(required = false) adjusted: String?,
    ): TossCandlePageResponse {
        val page = tossMarketQueryService.getCandles(symbol, interval, count, before, adjusted)
        return TossCandlePageResponse.from(page)
    }
}
