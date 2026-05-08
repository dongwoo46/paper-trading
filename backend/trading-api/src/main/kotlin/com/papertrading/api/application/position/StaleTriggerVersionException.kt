package com.papertrading.api.application.position

import com.papertrading.api.common.exception.ApiDomainException
import org.springframework.http.HttpStatus

class StaleTriggerVersionException(message: String) :
    ApiDomainException("STALE_TRIGGER_VERSION", HttpStatus.CONFLICT, message)

