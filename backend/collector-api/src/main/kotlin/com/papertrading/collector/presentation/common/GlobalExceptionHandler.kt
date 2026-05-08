package com.papertrading.collector.presentation.common

import com.papertrading.collector.application.market.service.InsufficientBarsForRequestedRangeException
import com.papertrading.collector.application.market.service.InsufficientDataForRsException
import com.papertrading.collector.application.market.service.SymbolNotFoundOrNoDataException
import com.papertrading.collector.application.market.service.SymbolNotFoundOrNoBarsException
import jakarta.servlet.http.HttpServletRequest
import mu.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import java.time.Instant

@RestControllerAdvice
class GlobalExceptionHandler {
	private val log = KotlinLogging.logger {}

	@ExceptionHandler(
		IllegalArgumentException::class,
		MethodArgumentTypeMismatchException::class,
		MissingServletRequestParameterException::class,
		HttpMessageNotReadableException::class,
		MethodArgumentNotValidException::class,
	)
	fun handleBadRequest(
		ex: Exception,
		request: HttpServletRequest,
	): ResponseEntity<ErrorResponse> {
		log.warn(ex) { "Bad request: ${request.method} ${request.requestURI}" }
		val code = resolveErrorCode(ex)
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
			ErrorResponse(
				status = HttpStatus.BAD_REQUEST.value(),
				error = "bad_request",
				code = code,
				message = ex.message ?: "Invalid request",
				path = request.requestURI,
				timestamp = Instant.now().toString(),
			),
		)
	}

	@ExceptionHandler(SymbolNotFoundOrNoBarsException::class)
	fun handleNotFound(
		ex: SymbolNotFoundOrNoBarsException,
		request: HttpServletRequest,
	): ResponseEntity<ErrorResponse> {
		log.warn(ex) { "Not found: ${request.method} ${request.requestURI}" }
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
			ErrorResponse(
				status = HttpStatus.NOT_FOUND.value(),
				error = "not_found",
				code = "SYMBOL_NOT_FOUND_OR_NO_BARS",
				message = ex.message ?: "Not found",
				path = request.requestURI,
				timestamp = Instant.now().toString(),
			),
		)
	}

	@ExceptionHandler(SymbolNotFoundOrNoDataException::class)
	fun handleNotFoundNoData(
		ex: SymbolNotFoundOrNoDataException,
		request: HttpServletRequest,
	): ResponseEntity<ErrorResponse> {
		log.warn(ex) { "Not found: ${request.method} ${request.requestURI}" }
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
			ErrorResponse(
				status = HttpStatus.NOT_FOUND.value(),
				error = "not_found",
				code = "SYMBOL_NOT_FOUND_OR_NO_DATA",
				message = ex.message ?: "Not found",
				path = request.requestURI,
				timestamp = Instant.now().toString(),
			),
		)
	}

	@ExceptionHandler(InsufficientBarsForRequestedRangeException::class)
	fun handleUnprocessable(
		ex: InsufficientBarsForRequestedRangeException,
		request: HttpServletRequest,
	): ResponseEntity<ErrorResponse> {
		log.warn(ex) { "Unprocessable request: ${request.method} ${request.requestURI}" }
		return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(
			ErrorResponse(
				status = HttpStatus.UNPROCESSABLE_ENTITY.value(),
				error = "unprocessable_entity",
				code = "INSUFFICIENT_BARS_FOR_REQUESTED_RANGE",
				message = ex.message ?: "Unprocessable request",
				path = request.requestURI,
				timestamp = Instant.now().toString(),
			),
		)
	}

	@ExceptionHandler(InsufficientDataForRsException::class)
	fun handleUnprocessableRs(
		ex: InsufficientDataForRsException,
		request: HttpServletRequest,
	): ResponseEntity<ErrorResponse> {
		log.warn(ex) { "Unprocessable request: ${request.method} ${request.requestURI}" }
		return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(
			ErrorResponse(
				status = HttpStatus.UNPROCESSABLE_ENTITY.value(),
				error = "unprocessable_entity",
				code = "INSUFFICIENT_DATA_FOR_RS",
				message = ex.message ?: "Unprocessable request",
				path = request.requestURI,
				timestamp = Instant.now().toString(),
			),
		)
	}

	@ExceptionHandler(Exception::class)
	fun handleUnhandled(
		ex: Exception,
		request: HttpServletRequest,
	): ResponseEntity<ErrorResponse> {
		log.error(ex) { "Unhandled error: ${request.method} ${request.requestURI}" }
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
			ErrorResponse(
				status = HttpStatus.INTERNAL_SERVER_ERROR.value(),
				error = "internal_server_error",
				code = "INTERNAL_SERVER_ERROR",
				message = "Unexpected server error",
				path = request.requestURI,
				timestamp = Instant.now().toString(),
			),
		)
	}

	private fun resolveErrorCode(ex: Exception): String {
		val message = ex.message ?: return "INVALID_REQUEST"
		return when {
			message.startsWith("INVALID_INTERVAL") -> "INVALID_INTERVAL"
			message.startsWith("INVALID_PERIOD_QUERY") -> "INVALID_PERIOD_QUERY"
			message.startsWith("INVALID_INDICATOR_PARAM") -> "INVALID_INDICATOR_PARAM"
			message.startsWith("INVALID_SESSION") -> "INVALID_SESSION"
			else -> "INVALID_REQUEST"
		}
	}
}

data class ErrorResponse(
	val status: Int,
	val error: String,
	val code: String,
	val message: String,
	val path: String,
	val timestamp: String,
)



