package com.papertrading.api.application.position

import com.papertrading.api.common.exception.ApiDomainException
import org.springframework.http.HttpStatus

class PositionNotEligibleException(message: String) :
    ApiDomainException("POSITION_NOT_ELIGIBLE", HttpStatus.UNPROCESSABLE_ENTITY, message)

