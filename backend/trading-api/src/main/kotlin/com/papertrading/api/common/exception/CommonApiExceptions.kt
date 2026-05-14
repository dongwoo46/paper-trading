package com.papertrading.api.common.exception

import org.springframework.http.HttpStatus

open class BadRequestException(
    errorCode: String,
    message: String,
    cause: Throwable? = null,
) : ApiDomainException(errorCode, HttpStatus.BAD_REQUEST, message, cause)

open class NotFoundException(
    errorCode: String,
    message: String,
    cause: Throwable? = null,
) : ApiDomainException(errorCode, HttpStatus.NOT_FOUND, message, cause)

open class ConflictException(
    errorCode: String,
    message: String,
    cause: Throwable? = null,
) : ApiDomainException(errorCode, HttpStatus.CONFLICT, message, cause)

class AccountNotFoundException(accountId: Long) :
    NotFoundException("ACCOUNT_NOT_FOUND", "계좌를 찾을 수 없습니다. id=$accountId")

class OrderNotFoundException(orderId: Long) :
    NotFoundException("ORDER_NOT_FOUND", "주문을 찾을 수 없습니다. orderId=$orderId")

class PositionNotFoundException(positionId: Long? = null, ticker: String? = null) :
    NotFoundException(
        "POSITION_NOT_FOUND",
        when {
            positionId != null -> "포지션을 찾을 수 없습니다. id=$positionId"
            !ticker.isNullOrBlank() -> "포지션을 찾을 수 없습니다. ticker=$ticker"
            else -> "포지션을 찾을 수 없습니다."
        }
    )

class TradingJournalNotFoundException(journalId: Long) :
    NotFoundException("TRADING_JOURNAL_NOT_FOUND", "거래 일지를 찾을 수 없습니다. id=$journalId")

class InvalidRetryPolicyException :
    BadRequestException("INVALID_RETRY_POLICY", "재시도 정책 값이 올바르지 않습니다.")

class WebhookNotConfiguredException :
    ConflictException("WEBHOOK_NOT_CONFIGURED", "Webhook이 설정되지 않았습니다.")

class InvalidPercentScaleException :
    BadRequestException("INVALID_PERCENT_SCALE", "percent scale must be <= 4")

class InvalidAccountIdException(accountId: Long) :
    BadRequestException("INVALID_ACCOUNT_ID", "accountId must be positive. accountId=$accountId")

class QuoteUnavailableException(ticker: String) :
    ConflictException("QUOTE_UNAVAILABLE", "시세 정보가 없습니다. ticker=$ticker")

class TradingJournalOwnershipMismatchException(journalId: Long, accountId: Long) :
    ConflictException(
        "TRADING_JOURNAL_ACCOUNT_MISMATCH",
        "해당 계좌의 거래 일지가 아닙니다. journalId=$journalId, accountId=$accountId"
    )

class InvalidPaginationException(message: String) :
    BadRequestException("INVALID_PAGINATION", message)

class KisResponseParseException(message: String) :
    BadRequestException("KIS_RESPONSE_PARSE_ERROR", message)

open class UnauthorizedException(
    errorCode: String,
    message: String,
    cause: Throwable? = null,
) : ApiDomainException(errorCode, HttpStatus.UNAUTHORIZED, message, cause)

open class ForbiddenException(
    errorCode: String,
    message: String,
    cause: Throwable? = null,
) : ApiDomainException(errorCode, HttpStatus.FORBIDDEN, message, cause)

open class BadGatewayException(
    errorCode: String,
    message: String,
    cause: Throwable? = null,
) : ApiDomainException(errorCode, HttpStatus.BAD_GATEWAY, message, cause)

open class GatewayTimeoutException(
    errorCode: String,
    message: String,
    cause: Throwable? = null,
) : ApiDomainException(errorCode, HttpStatus.GATEWAY_TIMEOUT, message, cause)

class KisAuthorizationException(message: String, cause: Throwable? = null) :
    UnauthorizedException("KIS_UNAUTHORIZED", message, cause)

class KisForbiddenException(message: String, cause: Throwable? = null) :
    ForbiddenException("KIS_FORBIDDEN", message, cause)

class KisRemoteCallException(message: String, cause: Throwable? = null) :
    BadGatewayException("KIS_BAD_GATEWAY", message, cause)

class KisTimeoutException(message: String, cause: Throwable? = null) :
    GatewayTimeoutException("KIS_GATEWAY_TIMEOUT", message, cause)

class ExternalServiceResponseException(message: String) :
    ConflictException("EXTERNAL_SERVICE_RESPONSE_ERROR", message)

class EntityMappingException(message: String) :
    ConflictException("ENTITY_MAPPING_ERROR", message)

class UnsupportedSettlementCurrencyException(accountId: Long) :
    BadRequestException("UNSUPPORTED_SETTLEMENT_CURRENCY", "다중 통화 정산은 지원하지 않습니다. accountId=$accountId")

class InvalidTaxYearException(value: Int) :
    BadRequestException("INVALID_TAX_YEAR", "유효하지 않은 taxYear 입니다. value=$value")

class TaxComputationScaleException(field: String) :
    ConflictException("TAX_COMPUTATION_SCALE_INVALID", "$field scale must be 2")

class SlackWebhookFailedException(message: String) :
    ApiDomainException("BAD_GATEWAY", HttpStatus.BAD_GATEWAY, message)

class StaleTriggerVersionException(message: String) :
    ApiDomainException("STALE_TRIGGER_VERSION", HttpStatus.CONFLICT, message)

class PositionNotEligibleException(message: String) :
    ApiDomainException("POSITION_NOT_ELIGIBLE", HttpStatus.UNPROCESSABLE_ENTITY, message)

class InvalidDateRangeException(message: String) :
    BadRequestException("INVALID_DATE_RANGE", message)

class InvalidBusinessDateException(message: String) :
    BadRequestException("INVALID_BUSINESS_DATE", message)

class SnapshotAlreadyRunningException(message: String) :
    ConflictException("SNAPSHOT_ALREADY_RUNNING", message)

class SnapshotComputeFailedException(message: String, cause: Throwable? = null) :
    ApiDomainException("SNAPSHOT_COMPUTE_FAILED", HttpStatus.INTERNAL_SERVER_ERROR, message, cause)

class InvalidTaxYearRangeException(message: String) :
    BadRequestException("INVALID_TAX_YEAR_RANGE", message)

class TaxYearNotClosedException(message: String) :
    BadRequestException("TAX_YEAR_NOT_CLOSED", message)

class TaxSummaryNotFoundException(message: String) :
    NotFoundException("TAX_SUMMARY_NOT_FOUND", message)

class TaxSummaryAlreadyRunningException(message: String) :
    ConflictException("TAX_SUMMARY_ALREADY_RUNNING", message)

class UnsupportedCurrencyException(message: String) :
    ApiDomainException("UNSUPPORTED_CURRENCY", HttpStatus.UNPROCESSABLE_ENTITY, message)

class TaxSummaryComputeFailedException(message: String, cause: Throwable? = null) :
    ApiDomainException("TAX_SUMMARY_COMPUTE_FAILED", HttpStatus.INTERNAL_SERVER_ERROR, message, cause)

class InvalidAccountModeForTaxSummaryException(message: String) :
    BadRequestException("INVALID_ACCOUNT_MODE_FOR_TAX_SUMMARY", message)
