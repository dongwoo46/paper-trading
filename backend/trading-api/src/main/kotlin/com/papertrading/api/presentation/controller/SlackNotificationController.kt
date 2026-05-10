package com.papertrading.api.presentation.controller

import com.papertrading.api.application.notification.SlackNotificationService
import com.papertrading.api.presentation.dto.notification.SlackNotificationConfigResponse
import com.papertrading.api.presentation.dto.notification.SlackNotificationTestRequest
import com.papertrading.api.presentation.dto.notification.SlackNotificationTestResponse
import com.papertrading.api.presentation.dto.notification.UpdateSlackNotificationConfigRequest
import jakarta.validation.Valid
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
    private val slackNotificationService: SlackNotificationService,
) {
    @GetMapping
    fun getConfig(): SlackNotificationConfigResponse =
        SlackNotificationConfigResponse.from(slackNotificationService.getConfig())

    @PutMapping
    fun updateConfig(
        @Valid @RequestBody request: UpdateSlackNotificationConfigRequest,
    ): SlackNotificationConfigResponse =
        SlackNotificationConfigResponse.from(slackNotificationService.updateConfig(request.toCommand()))

    @PostMapping("/test")
    @ResponseStatus(HttpStatus.ACCEPTED)
    fun sendTest(
        @Valid @RequestBody request: SlackNotificationTestRequest,
    ): SlackNotificationTestResponse =
        SlackNotificationTestResponse(
            accepted = true,
            requestId = slackNotificationService.sendTestMessage(request.message),
        )
}
