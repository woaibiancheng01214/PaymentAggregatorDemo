package com.example.payagg.infra

import com.example.payagg.domain.IdempotencyService
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerInterceptor

@Component
class IdempotencyInterceptor(
    private val idempotencyService: IdempotencyService,
    private val objectMapper: ObjectMapper
) : HandlerInterceptor {
    
    private val logger = LoggerFactory.getLogger(IdempotencyInterceptor::class.java)
    
    companion object {
        const val IDEMPOTENCY_KEY_HEADER = "Idempotency-Key"
        const val REQUEST_ID_HEADER = "X-Request-Id"
    }
    
    override fun preHandle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any
    ): Boolean {
        val idempotencyKey = request.getHeader(IDEMPOTENCY_KEY_HEADER)
        val requestId = request.getHeader(REQUEST_ID_HEADER)

        // Only apply idempotency to POST requests with idempotency key and request ID
        if (request.method != "POST" || idempotencyKey.isNullOrBlank() || requestId.isNullOrBlank()) {
            return true
        }
        
        val endpoint = request.requestURI
        
        // Check if operation was already executed
        if (idempotencyService.isOperationExecuted(requestId, idempotencyKey, endpoint)) {
            logger.info("Idempotent operation detected for requestId: $requestId, key: $idempotencyKey, endpoint: $endpoint")

            // Try to retrieve cached response
            val cachedResult = idempotencyService.idempotencyPort.retrieve(
                com.example.payagg.ports.IdempotencyKey(requestId, idempotencyKey, endpoint).toRedisKey()
            )
            
            if (cachedResult != null) {
                response.status = HttpServletResponse.SC_OK
                response.contentType = "application/json"
                response.writer.write(objectMapper.writeValueAsString(cachedResult))
                return false // Stop further processing
            }
        }
        
        return true
    }
}
