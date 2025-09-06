package com.example.payagg.domain

enum class PaymentAttemptStatus {
    RECEIVED,
    AUTHORISED,
    CAPTURED,
    SETTLED,
    FAILED,
    CANCELLED
}
