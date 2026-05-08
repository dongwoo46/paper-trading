package com.papertrading.collector.application.subscriptions.service

import com.papertrading.collector.application.kis.service.KisRestWatchlistService
import com.papertrading.collector.application.kis.service.KisWsSubscriptionService
import com.papertrading.collector.infra.kis.KisProperties
import com.papertrading.collector.infra.redis.RedisSetClient
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class SubscriptionRoutingService(
    private val redisSetClient: RedisSetClient,
    private val wsSubscriptionService: KisWsSubscriptionService,
    private val restWatchlistService: KisRestWatchlistService,
    private val kisProperties: KisProperties,
) {
    fun getFavorites(mode: String?, channel: String?): FavoritesListResponse {
        val normalized = normalizeModeAndChannel(mode, channel)
        if (normalized.error != null) {
            return FavoritesListResponse(
                mode = normalized.mode ?: "",
                channel = normalized.channel ?: "",
                items = emptyList(),
                returnedCount = 0,
                status = normalized.error,
            )
        }
        val key = favoritesKey(normalized.mode!!, normalized.channel!!)
        val items = redisSetClient.members(key)
        return FavoritesListResponse(
            mode = normalized.mode,
            channel = normalized.channel,
            items = items,
            returnedCount = items.size,
            status = "ok",
        )
    }

    fun addFavorite(mode: String?, channel: String?, symbol: String?): FavoritesWriteResponse {
        val normalized = normalizeModeAndChannelAndSymbol(mode, channel, symbol)
        if (normalized.error != null) {
            return FavoritesWriteResponse(
                status = normalized.error,
                mode = normalized.mode ?: "",
                channel = normalized.channel ?: "",
                symbol = normalized.symbol ?: "",
                totalSelected = 0,
            )
        }
        return try {
            val key = favoritesKey(normalized.mode!!, normalized.channel!!)
            val exists = redisSetClient.members(key).contains(normalized.symbol)
            if (exists) {
                FavoritesWriteResponse("already_exists", normalized.mode!!, normalized.channel!!, normalized.symbol!!, redisSetClient.size(key).toInt())
            } else {
                redisSetClient.add(key, normalized.symbol!!)
                FavoritesWriteResponse("added", normalized.mode, normalized.channel, normalized.symbol, redisSetClient.size(key).toInt())
            }
        } catch (_: Exception) {
            FavoritesWriteResponse("internal_error", normalized.mode!!, normalized.channel!!, normalized.symbol!!, 0)
        }
    }

    fun removeFavorite(mode: String?, channel: String?, symbol: String?): FavoritesWriteResponse {
        val normalized = normalizeModeAndChannelAndSymbol(mode, channel, symbol)
        if (normalized.error != null) {
            return FavoritesWriteResponse(
                status = normalized.error,
                mode = normalized.mode ?: "",
                channel = normalized.channel ?: "",
                symbol = normalized.symbol ?: "",
                totalSelected = 0,
            )
        }
        return try {
            val key = favoritesKey(normalized.mode!!, normalized.channel!!)
            val exists = redisSetClient.members(key).contains(normalized.symbol)
            if (!exists) {
                FavoritesWriteResponse("not_found", normalized.mode!!, normalized.channel!!, normalized.symbol!!, redisSetClient.size(key).toInt())
            } else {
                redisSetClient.remove(key, normalized.symbol!!)
                FavoritesWriteResponse("removed", normalized.mode, normalized.channel, normalized.symbol, redisSetClient.size(key).toInt())
            }
        } catch (_: Exception) {
            FavoritesWriteResponse("internal_error", normalized.mode!!, normalized.channel!!, normalized.symbol!!, 0)
        }
    }

    fun getStrategySymbols(mode: String?): StrategySymbolsListResponse {
        val normalized = normalizeMode(mode)
        if (normalized.error != null) {
            return StrategySymbolsListResponse(
                mode = normalized.mode ?: "",
                items = emptyList(),
                returnedCount = 0,
                status = normalized.error,
            )
        }
        val items = redisSetClient.members(strategyKey(normalized.mode!!))
        return StrategySymbolsListResponse(
            mode = normalized.mode,
            items = items,
            returnedCount = items.size,
            status = "ok",
        )
    }

    fun addStrategySymbol(mode: String?, symbol: String?): StrategySymbolsWriteResponse {
        val normalized = normalizeModeAndSymbol(mode, symbol)
        if (normalized.error != null) {
            return StrategySymbolsWriteResponse(normalized.error, normalized.mode ?: "", normalized.symbol ?: "", 0)
        }
        return try {
            val key = strategyKey(normalized.mode!!)
            val exists = redisSetClient.members(key).contains(normalized.symbol)
            if (exists) {
                StrategySymbolsWriteResponse("already_exists", normalized.mode!!, normalized.symbol!!, redisSetClient.size(key).toInt())
            } else {
                redisSetClient.add(key, normalized.symbol!!)
                StrategySymbolsWriteResponse("added", normalized.mode, normalized.symbol, redisSetClient.size(key).toInt())
            }
        } catch (_: Exception) {
            StrategySymbolsWriteResponse("internal_error", normalized.mode!!, normalized.symbol!!, 0)
        }
    }

    fun removeStrategySymbol(mode: String?, symbol: String?): StrategySymbolsWriteResponse {
        val normalized = normalizeModeAndSymbol(mode, symbol)
        if (normalized.error != null) {
            return StrategySymbolsWriteResponse(normalized.error, normalized.mode ?: "", normalized.symbol ?: "", 0)
        }
        return try {
            val key = strategyKey(normalized.mode!!)
            val exists = redisSetClient.members(key).contains(normalized.symbol)
            if (!exists) {
                StrategySymbolsWriteResponse("not_found", normalized.mode!!, normalized.symbol!!, redisSetClient.size(key).toInt())
            } else {
                redisSetClient.remove(key, normalized.symbol!!)
                StrategySymbolsWriteResponse("removed", normalized.mode, normalized.symbol, redisSetClient.size(key).toInt())
            }
        } catch (_: Exception) {
            StrategySymbolsWriteResponse("internal_error", normalized.mode!!, normalized.symbol!!, 0)
        }
    }

    fun getRoutingStatus(mode: String?): RoutingStatusResponse {
        val normalized = normalizeMode(mode)
        if (normalized.error != null) {
            return RoutingStatusResponse(
                generatedAt = Instant.now().toString(),
                mode = normalized.mode ?: "",
                ws = RoutingSymbols(slotUsed = 0, slotMax = kisProperties.maxRealtimeRegistrations, symbols = emptyList()),
                rest = RoutingSymbols(slotUsed = 0, slotMax = 0, symbols = emptyList()),
                sources = RoutingSources(emptyList(), emptyList(), emptyList()),
                status = normalized.error,
            )
        }
        return try {
            val modeValue = normalized.mode!!
            val wsSymbols = wsSubscriptionService.listSymbols(modeValue)
            val restSymbols = restWatchlistService.listSymbols(modeValue)
            val manualSymbols = (
                redisSetClient.members(kisManualKey(modeValue, "ws")) +
                    redisSetClient.members(kisManualKey(modeValue, "rest"))
                ).distinct().sorted()
            val favorites = (redisSetClient.members(favoritesKey(modeValue, "ws")) + redisSetClient.members(favoritesKey(modeValue, "rest")))
                .distinct()
                .sorted()
            val strategy = redisSetClient.members(strategyKey(modeValue))
            RoutingStatusResponse(
                generatedAt = Instant.now().toString(),
                mode = modeValue,
                ws = RoutingSymbols(slotUsed = wsSymbols.size, slotMax = kisProperties.maxRealtimeRegistrations, symbols = wsSymbols.sorted()),
                rest = RoutingSymbols(slotUsed = 0, slotMax = 0, symbols = restSymbols.sorted()),
                sources = RoutingSources(
                    manual = manualSymbols,
                    favorites = favorites,
                    strategyPriority = strategy,
                ),
                status = "ok",
            )
        } catch (_: Exception) {
            RoutingStatusResponse(
                generatedAt = Instant.now().toString(),
                mode = normalized.mode ?: "",
                ws = RoutingSymbols(slotUsed = 0, slotMax = kisProperties.maxRealtimeRegistrations, symbols = emptyList()),
                rest = RoutingSymbols(slotUsed = 0, slotMax = 0, symbols = emptyList()),
                sources = RoutingSources(emptyList(), emptyList(), emptyList()),
                status = "internal_error",
            )
        }
    }

    private fun favoritesKey(mode: String, channel: String) = "subscriptions:favorites:$mode:$channel"
    private fun strategyKey(mode: String) = "subscriptions:strategy-symbols:$mode"
    private fun kisManualKey(mode: String, channel: String) = "kis:$channel:$mode"

    private fun normalizeMode(mode: String?): Normalized = when {
        mode == null -> Normalized(error = "invalid_input")
        else -> {
            val normalizedMode = mode.trim().lowercase()
            if (normalizedMode != "paper" && normalizedMode != "live") {
                Normalized(mode = normalizedMode, error = "invalid_mode")
            } else {
                Normalized(mode = normalizedMode)
            }
        }
    }

    private fun normalizeModeAndChannel(mode: String?, channel: String?): Normalized {
        if (mode == null || channel == null) return Normalized(error = "invalid_input")
        val normalizedMode = mode.trim().lowercase()
        val normalizedChannel = channel.trim().lowercase()
        if (normalizedMode != "paper" && normalizedMode != "live") return Normalized(mode = normalizedMode, channel = normalizedChannel, error = "invalid_mode")
        if (normalizedChannel != "ws" && normalizedChannel != "rest") return Normalized(mode = normalizedMode, channel = normalizedChannel, error = "invalid_channel")
        return Normalized(mode = normalizedMode, channel = normalizedChannel)
    }

    private fun normalizeModeAndSymbol(mode: String?, symbol: String?): Normalized {
        if (mode == null || symbol == null) return Normalized(error = "invalid_input")
        val normalizedMode = mode.trim().lowercase()
        val normalizedSymbol = symbol.trim().uppercase()
        if (normalizedMode != "paper" && normalizedMode != "live") return Normalized(mode = normalizedMode, symbol = normalizedSymbol, error = "invalid_mode")
        if (!isValidSymbol(normalizedSymbol)) return Normalized(mode = normalizedMode, symbol = normalizedSymbol, error = "invalid_symbol")
        return Normalized(mode = normalizedMode, symbol = normalizedSymbol)
    }

    private fun normalizeModeAndChannelAndSymbol(mode: String?, channel: String?, symbol: String?): Normalized {
        if (mode == null || channel == null || symbol == null) return Normalized(error = "invalid_input")
        val normalizedMode = mode.trim().lowercase()
        val normalizedChannel = channel.trim().lowercase()
        val normalizedSymbol = symbol.trim().uppercase()
        if (normalizedMode != "paper" && normalizedMode != "live") return Normalized(mode = normalizedMode, channel = normalizedChannel, symbol = normalizedSymbol, error = "invalid_mode")
        if (normalizedChannel != "ws" && normalizedChannel != "rest") return Normalized(mode = normalizedMode, channel = normalizedChannel, symbol = normalizedSymbol, error = "invalid_channel")
        if (!isValidSymbol(normalizedSymbol)) return Normalized(mode = normalizedMode, channel = normalizedChannel, symbol = normalizedSymbol, error = "invalid_symbol")
        return Normalized(mode = normalizedMode, channel = normalizedChannel, symbol = normalizedSymbol)
    }

    private fun isValidSymbol(symbol: String): Boolean {
        return symbol.isNotBlank() && symbol.length <= 20 && SYMBOL_PATTERN.matches(symbol)
    }

    private data class Normalized(
        val mode: String? = null,
        val channel: String? = null,
        val symbol: String? = null,
        val error: String? = null,
    )

    companion object {
        private val SYMBOL_PATTERN = Regex("^[0-9A-Z._-]{1,20}$")
    }
}

data class FavoritesListResponse(
    val mode: String,
    val channel: String,
    val items: List<String>,
    val returnedCount: Int,
    val status: String,
)

data class FavoritesWriteResponse(
    val status: String,
    val mode: String,
    val channel: String,
    val symbol: String,
    val totalSelected: Int,
)

data class StrategySymbolsListResponse(
    val mode: String,
    val items: List<String>,
    val returnedCount: Int,
    val status: String,
)

data class StrategySymbolsWriteResponse(
    val status: String,
    val mode: String,
    val symbol: String,
    val totalSelected: Int,
)

data class RoutingStatusResponse(
    val generatedAt: String,
    val mode: String,
    val ws: RoutingSymbols,
    val rest: RoutingSymbols,
    val sources: RoutingSources,
    val status: String,
)

data class RoutingSymbols(
    val slotUsed: Int,
    val slotMax: Int,
    val symbols: List<String>,
)

data class RoutingSources(
    val manual: List<String>,
    val favorites: List<String>,
    val strategyPriority: List<String>,
)


