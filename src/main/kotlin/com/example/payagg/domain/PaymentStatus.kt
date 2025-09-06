package com.example.payagg.domain

enum class PaymentStatus {
    INIT,
    REQUIRES_AUTHORISATION,
    REQUIRES_CAPTURE,
    PENDING,
    SUCCEEDED,
    CANCELLED
}
