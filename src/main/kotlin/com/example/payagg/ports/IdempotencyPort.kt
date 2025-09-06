package com.example.payagg.ports

import java.time.Duration

interface IdempotencyPort {
    fun store(key: String, value: Any, ttl: Duration): Boolean
    fun retrieve(key: String): Any?
    fun exists(key: String): Boolean
    fun delete(key: String): Boolean
}

data class IdempotencyKey(
    val requestId: String,
    val idempotencyKey: String,
    val endpoint: String
) {
    fun toRedisKey(): String = "idempotency:$requestId:$endpoint:$idempotencyKey"
}
