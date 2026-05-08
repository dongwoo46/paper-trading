package com.papertrading.api.interfaces.rest.position

import com.papertrading.api.application.position.PositionExitTriggerCommandService
import com.papertrading.api.application.position.PositionExitTriggerQueryService
import com.papertrading.api.interfaces.rest.position.dto.UpsertPositionExitTriggerRequest
import com.papertrading.api.interfaces.rest.position.dto.toResponse
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/positions")
class PositionExitTriggerController(
    private val commandService: PositionExitTriggerCommandService,
    private val queryService: PositionExitTriggerQueryService,
) {
    @PutMapping("/{positionId}/exit-trigger")
    fun upsert(
        @PathVariable positionId: Long,
        @Valid @RequestBody request: UpsertPositionExitTriggerRequest,
    ) = commandService.upsertPositionTrigger(request.toCommand(positionId)).toResponse()

    @GetMapping("/{positionId}/exit-trigger")
    fun get(@PathVariable positionId: Long) = queryService.getEffectiveTrigger(positionId).toResponse()
}
