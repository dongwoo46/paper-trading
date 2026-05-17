package com.papertrading.api.presentation.controller

import com.papertrading.api.application.position.PositionExitTriggerCommandService
import com.papertrading.api.application.position.PositionExitTriggerQueryService
import com.papertrading.api.application.position.command.CancelPositionExitTriggerCommand
import com.papertrading.api.presentation.dto.position.CreatePositionExitTriggerRequest
import com.papertrading.api.presentation.dto.position.UpdatePositionExitTriggerRequest
import com.papertrading.api.presentation.dto.position.toResponse
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/positions")
class PositionExitTriggerController(
    private val commandService: PositionExitTriggerCommandService,
    private val queryService: PositionExitTriggerQueryService,
) {
    @PostMapping("/{positionId}/exit-triggers")
    fun create(
        @PathVariable positionId: Long,
        @Valid @RequestBody request: CreatePositionExitTriggerRequest,
    ) = commandService.createPositionTrigger(request.toCommand(positionId)).toResponse()

    @GetMapping("/{positionId}/exit-triggers")
    fun list(@PathVariable positionId: Long) = queryService.listPositionTriggers(positionId).toResponse()

    @PatchMapping("/{positionId}/exit-triggers/{triggerId}")
    fun update(
        @PathVariable positionId: Long,
        @PathVariable triggerId: Long,
        @Valid @RequestBody request: UpdatePositionExitTriggerRequest,
    ) = commandService.updatePositionTrigger(request.toCommand(positionId, triggerId)).toResponse()

    @DeleteMapping("/{positionId}/exit-triggers/{triggerId}")
    fun cancel(
        @PathVariable positionId: Long,
        @PathVariable triggerId: Long,
        @RequestParam(required = false) expectedVersion: Long?,
    ) = commandService.cancelPositionTrigger(
        CancelPositionExitTriggerCommand(
            positionId = positionId,
            triggerId = triggerId,
            expectedVersion = expectedVersion,
        )
    ).toResponse()
}
