package com.papertrading.api.application.portfolio

import com.papertrading.api.common.exception.ApiDomainException
import org.springframework.http.HttpStatus

open class PortfolioSnapshotDomainException(
    errorCode: String,
    status: HttpStatus,
    message: String,
    cause: Throwable? = null,
) : ApiDomainException(errorCode, status, message, cause)

class InvalidDateRangeException(message: String) :
    PortfolioSnapshotDomainException("INVALID_DATE_RANGE", HttpStatus.BAD_REQUEST, message)

class InvalidBusinessDateException(message: String) :
    PortfolioSnapshotDomainException("INVALID_BUSINESS_DATE", HttpStatus.BAD_REQUEST, message)

class SnapshotAlreadyRunningException(message: String) :
    PortfolioSnapshotDomainException("SNAPSHOT_ALREADY_RUNNING", HttpStatus.CONFLICT, message)

class SnapshotComputeFailedException(message: String, cause: Throwable? = null) :
    PortfolioSnapshotDomainException("SNAPSHOT_COMPUTE_FAILED", HttpStatus.INTERNAL_SERVER_ERROR, message, cause)
