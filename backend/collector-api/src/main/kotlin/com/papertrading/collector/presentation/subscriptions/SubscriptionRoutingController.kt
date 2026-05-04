package com.papertrading.collector.presentation.subscriptions

import com.papertrading.collector.application.subscriptions.service.FavoritesListResponse
import com.papertrading.collector.application.subscriptions.service.FavoritesWriteResponse
import com.papertrading.collector.application.subscriptions.service.RoutingStatusResponse
import com.papertrading.collector.application.subscriptions.service.StrategySymbolsListResponse
import com.papertrading.collector.application.subscriptions.service.StrategySymbolsWriteResponse
import com.papertrading.collector.application.subscriptions.service.SubscriptionRoutingService
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/subscriptions")
class SubscriptionRoutingController(
    private val subscriptionRoutingService: SubscriptionRoutingService,
) {
    @GetMapping("/favorites")
    fun getFavorites(
        @RequestParam mode: String?,
        @RequestParam channel: String?,
    ): FavoritesListResponse {
        return subscriptionRoutingService.getFavorites(mode, channel)
    }

    @PostMapping("/favorites")
    fun addFavorite(
        @RequestBody request: FavoritesWriteRequest,
    ): FavoritesWriteResponse {
        return subscriptionRoutingService.addFavorite(request.mode, request.channel, request.symbol)
    }

    @DeleteMapping("/favorites")
    fun removeFavorite(
        @RequestBody request: FavoritesWriteRequest,
    ): FavoritesWriteResponse {
        return subscriptionRoutingService.removeFavorite(request.mode, request.channel, request.symbol)
    }

    @GetMapping("/strategy-symbols")
    fun getStrategySymbols(
        @RequestParam mode: String?,
    ): StrategySymbolsListResponse {
        return subscriptionRoutingService.getStrategySymbols(mode)
    }

    @PostMapping("/strategy-symbols")
    fun addStrategySymbol(
        @RequestBody request: StrategySymbolsWriteRequest,
    ): StrategySymbolsWriteResponse {
        return subscriptionRoutingService.addStrategySymbol(request.mode, request.symbol)
    }

    @DeleteMapping("/strategy-symbols")
    fun removeStrategySymbol(
        @RequestBody request: StrategySymbolsWriteRequest,
    ): StrategySymbolsWriteResponse {
        return subscriptionRoutingService.removeStrategySymbol(request.mode, request.symbol)
    }

    @GetMapping("/routing-status")
    fun getRoutingStatus(
        @RequestParam mode: String?,
    ): RoutingStatusResponse {
        return subscriptionRoutingService.getRoutingStatus(mode)
    }
}

data class FavoritesWriteRequest(
    val mode: String?,
    val channel: String?,
    val symbol: String?,
)

data class StrategySymbolsWriteRequest(
    val mode: String?,
    val symbol: String?,
)
