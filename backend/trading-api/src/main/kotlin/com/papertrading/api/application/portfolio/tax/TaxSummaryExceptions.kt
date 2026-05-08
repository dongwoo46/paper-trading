package com.papertrading.api.application.portfolio.tax

import com.papertrading.api.common.exception.ApiDomainException
import org.springframework.http.HttpStatus

open class TaxSummaryDomainException(
    errorCode: String,
    status: HttpStatus,
    message: String,
    cause: Throwable? = null,
) : ApiDomainException(errorCode, status, message, cause)

class InvalidTaxYearRangeException(message: String) :
    TaxSummaryDomainException("INVALID_TAX_YEAR_RANGE", HttpStatus.BAD_REQUEST, message)
class TaxYearNotClosedException(message: String) :
    TaxSummaryDomainException("TAX_YEAR_NOT_CLOSED", HttpStatus.BAD_REQUEST, message)
class TaxSummaryNotFoundException(message: String) :
    TaxSummaryDomainException("TAX_SUMMARY_NOT_FOUND", HttpStatus.NOT_FOUND, message)
class TaxSummaryAlreadyRunningException(message: String) :
    TaxSummaryDomainException("TAX_SUMMARY_ALREADY_RUNNING", HttpStatus.CONFLICT, message)
class UnsupportedCurrencyException(message: String) :
    TaxSummaryDomainException("UNSUPPORTED_CURRENCY", HttpStatus.UNPROCESSABLE_ENTITY, message)
class TaxSummaryComputeFailedException(message: String, cause: Throwable? = null) :
    TaxSummaryDomainException("TAX_SUMMARY_COMPUTE_FAILED", HttpStatus.INTERNAL_SERVER_ERROR, message, cause)
