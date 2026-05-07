package com.papertrading.api.common.exception

open class PortfolioSnapshotDomainException(
    val errorCode: String,
    message: String,
) : RuntimeException(message)

class InvalidDateRangeException(message: String) :
    PortfolioSnapshotDomainException("INVALID_DATE_RANGE", message)

class InvalidBusinessDateException(message: String) :
    PortfolioSnapshotDomainException("INVALID_BUSINESS_DATE", message)

class SnapshotAlreadyRunningException(message: String) :
    PortfolioSnapshotDomainException("SNAPSHOT_ALREADY_RUNNING", message)

class SnapshotComputeFailedException(message: String, cause: Throwable? = null) :
    PortfolioSnapshotDomainException("SNAPSHOT_COMPUTE_FAILED", message) {
    init {
        if (cause != null) {
            initCause(cause)
        }
    }
}
