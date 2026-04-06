package com.papertrading.collector.presentation.common

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
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
			ErrorResponse(
				status = HttpStatus.BAD_REQUEST.value(),
				error = "bad_request",
				message = ex.message ?: "Invalid request",
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
				message = "Unexpected server error",
				path = request.requestURI,
				timestamp = Instant.now().toString(),
			),
		)
	}
}

data class ErrorResponse(
	val status: Int,
	val error: String,
	val message: String,
	val path: String,
	val timestamp: String,
)

