package com.papertrading.api.application.portfolio.tax

open class TaxSummaryDomainException(
    val errorCode: String,
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)

class InvalidTaxYearRangeException(message: String) : TaxSummaryDomainException("INVALID_TAX_YEAR_RANGE", message)
class TaxYearNotClosedException(message: String) : TaxSummaryDomainException("TAX_YEAR_NOT_CLOSED", message)
class TaxSummaryNotFoundException(message: String) : TaxSummaryDomainException("TAX_SUMMARY_NOT_FOUND", message)
class TaxSummaryAlreadyRunningException(message: String) : TaxSummaryDomainException("TAX_SUMMARY_ALREADY_RUNNING", message)
class UnsupportedCurrencyException(message: String) : TaxSummaryDomainException("UNSUPPORTED_CURRENCY", message)
class TaxSummaryComputeFailedException(message: String, cause: Throwable? = null) :
    TaxSummaryDomainException("TAX_SUMMARY_COMPUTE_FAILED", message, cause)
