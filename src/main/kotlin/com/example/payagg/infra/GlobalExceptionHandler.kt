package com.example.payagg.infra

import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.context.request.WebRequest
import java.time.Instant

@RestControllerAdvice
class GlobalExceptionHandler {

    private val logger = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgumentException(
        ex: IllegalArgumentException,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        logger.warn("Bad request: ${ex.message}", ex)
        return ResponseEntity.badRequest().body(
            ErrorResponse(
                error = "BAD_REQUEST",
                message = ex.message ?: "Invalid request",
                timestamp = Instant.now()
            )
        )
    }

    @ExceptionHandler(Exception::class)
    fun handleGenericException(
        ex: Exception,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        logger.error("Internal server error: ${ex.message}", ex)
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
            ErrorResponse(
                error = "INTERNAL_SERVER_ERROR",
                message = "An unexpected error occurred",
                timestamp = Instant.now()
            )
        )
    }
}

data class ErrorResponse(
    val error: String,
    val message: String,
    val timestamp: Instant
)
