package com.example.payagg.adapters.idempotency

import com.example.payagg.ports.IdempotencyPort
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service
import java.time.Duration
import java.util.concurrent.TimeUnit

@Service
class RedisIdempotencyStore(
    private val redisTemplate: RedisTemplate<String, Any>,
    private val objectMapper: ObjectMapper
) : IdempotencyPort {
    
    private val logger = LoggerFactory.getLogger(RedisIdempotencyStore::class.java)
    
    override fun store(key: String, value: Any, ttl: Duration): Boolean {
        return try {
            val serializedValue = objectMapper.writeValueAsString(value)
            val operations = redisTemplate.opsForValue()
            
            // Use SET with NX (only if not exists) and EX (expiration)
            val result = operations.setIfAbsent(key, serializedValue, ttl.toSeconds(), TimeUnit.SECONDS)
            
            logger.debug("Stored idempotency key: $key, success: $result")
            result ?: false
        } catch (e: Exception) {
            logger.error("Failed to store idempotency key: $key", e)
            false
        }
    }
    
    override fun retrieve(key: String): Any? {
        return try {
            val serializedValue = redisTemplate.opsForValue().get(key) as? String
            if (serializedValue != null) {
                logger.debug("Retrieved idempotency key: $key")
                objectMapper.readValue(serializedValue, Any::class.java)
            } else {
                logger.debug("Idempotency key not found: $key")
                null
            }
        } catch (e: Exception) {
            logger.error("Failed to retrieve idempotency key: $key", e)
            null
        }
    }
    
    override fun exists(key: String): Boolean {
        return try {
            val exists = redisTemplate.hasKey(key)
            logger.debug("Idempotency key exists check: $key = $exists")
            exists
        } catch (e: Exception) {
            logger.error("Failed to check existence of idempotency key: $key", e)
            false
        }
    }
    
    override fun delete(key: String): Boolean {
        return try {
            val deleted = redisTemplate.delete(key)
            logger.debug("Deleted idempotency key: $key, success: $deleted")
            deleted
        } catch (e: Exception) {
            logger.error("Failed to delete idempotency key: $key", e)
            false
        }
    }
}
