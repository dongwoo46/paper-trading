package com.papertrading.api.presentation.controller

import com.papertrading.api.application.position.PositionQueryService
import com.papertrading.api.presentation.dto.position.PositionResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/accounts/{accountId}/positions")
class PositionController(
    private val positionQueryService: PositionQueryService,
) {
    @GetMapping
    fun listPositions(@PathVariable accountId: Long): List<PositionResponse> =
        positionQueryService.listPositionsWithCurrentPrice(accountId)
            .map { PositionResponse.from(it) }

    @GetMapping("/{ticker}")
    fun getPosition(
        @PathVariable accountId: Long,
        @PathVariable ticker: String,
    ): PositionResponse =
        PositionResponse.from(
            positionQueryService.getPositionWithCurrentPrice(accountId, ticker.trim().uppercase())
        )
}
