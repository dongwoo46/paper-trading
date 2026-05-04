package com.papertrading.api.presentation.controller

import com.papertrading.api.application.notification.SlackNotificationCommandService
import com.papertrading.api.application.notification.SlackNotificationQueryService
import com.papertrading.api.presentation.dto.notification.SlackNotificationConfigResponse
import com.papertrading.api.presentation.dto.notification.SlackNotificationTestRequest
import com.papertrading.api.presentation.dto.notification.SlackNotificationTestResponse
import com.papertrading.api.presentation.dto.notification.UpdateSlackNotificationConfigRequest
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/operations/notifications/slack")
class SlackNotificationController(
    private val queryService: SlackNotificationQueryService,
    private val commandService: SlackNotificationCommandService,
) {
    @GetMapping
    fun getConfig(): SlackNotificationConfigResponse =
        SlackNotificationConfigResponse.from(queryService.getConfig())

    @PutMapping
    fun updateConfig(
        @RequestBody request: UpdateSlackNotificationConfigRequest,
    ): SlackNotificationConfigResponse =
        SlackNotificationConfigResponse.from(commandService.updateConfig(request.toCommand()))

    @PostMapping("/test")
    @ResponseStatus(HttpStatus.ACCEPTED)
    fun sendTest(
        @RequestBody request: SlackNotificationTestRequest,
    ): SlackNotificationTestResponse =
        SlackNotificationTestResponse(
            accepted = true,
            requestId = commandService.sendTestMessage(request.message),
        )
}
