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

class ReceivableSettlementNotFoundException(receivableSettlementId: Long) :
    NotFoundException("RECEIVABLE_SETTLEMENT_NOT_FOUND", "미수정산 데이터를 찾을 수 없습니다. id=$receivableSettlementId")

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

class ExternalServiceResponseException(message: String) :
    ConflictException("EXTERNAL_SERVICE_RESPONSE_ERROR", message)

class EntityMappingException(message: String) :
    ConflictException("ENTITY_MAPPING_ERROR", message)

class UnsupportedSettlementCurrencyException(accountId: Long) :
    BadRequestException("UNSUPPORTED_SETTLEMENT_CURRENCY", "다중 통화 정산은 지원하지 않습니다. accountId=$accountId")

class InvalidTaxYearException(value: Int) :
    BadRequestException("INVALID_TAX_YEAR", "유효하지 않은 taxYear 입니다. value=$value")

class TaxComputationScaleException(field: String) :
    ConflictException("TAX_COMPUTATION_SCALE_INVALID", "$field scale must be 4")
