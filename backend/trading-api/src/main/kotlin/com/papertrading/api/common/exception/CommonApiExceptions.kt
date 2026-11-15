package com.papertrading.api.common.exception

import org.springframework.http.HttpStatus
import java.math.BigDecimal

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

// ---- 계좌·주문 에러 정렬(DEC-02): 기존 require/check 의 400 뭉침을 상황별 범주와 code 로 분리 ----

/** A-1 — 금액이 0 이하 (입금, 출금) */
class InvalidAmountException(message: String) : BadRequestException("INVALID_AMOUNT", message)

/** A-2 — 계좌명이 비어 있음 */
class InvalidAccountNameException : BadRequestException("INVALID_ACCOUNT_NAME", "계좌명은 비어 있을 수 없습니다.")

/** A-1 — 초기 예수금이 음수 */
class InvalidInitialDepositException : BadRequestException("INVALID_INITIAL_DEPOSIT", "초기 예수금은 0 이상이어야 합니다.")

/** A-1 — 기준 통화 코드가 3자리가 아님 */
class InvalidBaseCurrencyException : BadRequestException("INVALID_BASE_CURRENCY", "기준 통화는 3자리 코드여야 합니다.")

/** A-1 — 계좌 타입과 거래 모드의 잘못된 조합 */
class InvalidAccountModeCombinationException(
    accountType: String,
    tradingMode: String,
) : BadRequestException("INVALID_ACCOUNT_TYPE_TRADING_MODE", "계좌 타입과 거래 모드 조합이 올바르지 않습니다. $accountType, $tradingMode")

/** A-1 — 리스크 정책 값이 범위 밖 */
class InvalidRiskPolicyException(message: String) : BadRequestException("INVALID_RISK_POLICY", message)

/** A-2 — 지정가 주문에 가격이 없음 */
class LimitPriceRequiredException : BadRequestException("LIMIT_PRICE_REQUIRED", "지정가 주문은 limitPrice가 필요합니다.")

/** A-2 — GTD 주문에 만료 시각이 없음 */
class ExpireAtRequiredException : BadRequestException("EXPIRE_AT_REQUIRED", "GTD 주문은 expireAt이 필요합니다.")

/** F-5 — 가용 예수금 부족 (출금, 매수 주문 잠금) */
class InsufficientAvailableDepositException : ConflictException("INSUFFICIENT_AVAILABLE_DEPOSIT", "가용 예수금이 부족합니다.")

/** F-5 — 주문 가능 수량 부족 (매도 주문) */
class InsufficientOrderableQuantityException(
    available: BigDecimal,
    requested: BigDecimal,
) : ConflictException("INSUFFICIENT_ORDERABLE_QUANTITY", "주문 가능 수량 부족. available=$available, requested=$requested")

/** F-2 — 비활성화된 계좌로 주문 */
class AccountInactiveException : ConflictException("ACCOUNT_INACTIVE", "비활성화된 계좌입니다.")

/** F-2 — 이미 비활성화된 계좌를 다시 비활성화 */
class AccountAlreadyInactiveException : ConflictException("ACCOUNT_ALREADY_INACTIVE", "이미 비활성화된 계좌입니다.")

/** F-1 — 이미 활성화된 계좌를 다시 활성화 */
class AccountAlreadyActiveException : ConflictException("ACCOUNT_ALREADY_ACTIVE", "이미 활성화된 계좌입니다.")

/** F-2 — 취소할 수 없는 상태의 주문 (체결 완료, 취소 완료 등) */
class OrderNotCancelableException(
    status: String,
) : ConflictException("ORDER_NOT_CANCELABLE", "취소 불가 상태입니다. status=$status")

/** F-4 — 시세가 오래되어 아직 쓸 수 없음 */
class QuoteStaleException(ticker: String) : ConflictException("QUOTE_STALE", "시세가 오래되었습니다. (stale) ticker=$ticker")

/** K-2 — 잠금 예수금 장부가 어긋나 해제, 확정할 수 없음 */
class LockedDepositInsufficientException : InternalErrorException("LOCKED_DEPOSIT_INSUFFICIENT", "잠금 예수금이 부족합니다.")

/** K-3 — 내부에서 계산한 원장 금액이 0 이하 (잠금, 해제, 확정, 매도 대금) */
class LedgerAmountInvalidException(message: String) : InternalErrorException("LEDGER_AMOUNT_INVALID", message)

/** K-3 — 매도 체결의 포지션 원자 반영이 조건을 맞추지 못함 */
class SellExecutionApplyFailedException(
    positionId: Long,
    quantity: BigDecimal,
) : InternalErrorException("SELL_EXECUTION_APPLY_FAILED", "원자적 매도 반영 실패: positionId=$positionId, qty=$quantity")
