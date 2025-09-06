package com.example.payagg.domain

import com.example.payagg.ports.IdempotencyKey
import com.example.payagg.ports.IdempotencyPort
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.Duration

@Service
class IdempotencyService(
    internal val idempotencyPort: IdempotencyPort,
    @Value("\${payagg.idempotency.ttl:PT1H}")
    private val defaultTtl: Duration
) {
    
    private val logger = LoggerFactory.getLogger(IdempotencyService::class.java)
    
    fun <T> executeIdempotent(
        requestId: String,
        idempotencyKey: String,
        endpoint: String,
        operation: () -> T
    ): T {
        val key = IdempotencyKey(requestId, idempotencyKey, endpoint)
        val redisKey = key.toRedisKey()
        
        // Check if operation was already executed
        val existingResult = idempotencyPort.retrieve(redisKey)
        if (existingResult != null) {
            logger.info("Returning cached result for idempotency key: $redisKey")
            @Suppress("UNCHECKED_CAST")
            return existingResult as T
        }
        
        // Execute operation
        val result = operation()
        
        // Store result with TTL
        val stored = idempotencyPort.store(redisKey, result as Any, defaultTtl)
        if (!stored) {
            logger.warn("Failed to store idempotency result for key: $redisKey")
        }
        
        return result
    }
    
    fun isOperationExecuted(requestId: String, idempotencyKey: String, endpoint: String): Boolean {
        val key = IdempotencyKey(requestId, idempotencyKey, endpoint)
        return idempotencyPort.exists(key.toRedisKey())
    }

    fun invalidateIdempotencyKey(requestId: String, idempotencyKey: String, endpoint: String): Boolean {
        val key = IdempotencyKey(requestId, idempotencyKey, endpoint)
        return idempotencyPort.delete(key.toRedisKey())
    }
}
