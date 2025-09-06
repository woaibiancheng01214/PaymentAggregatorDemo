package com.example.payagg.ports

import com.example.payagg.domain.CardNetwork
import java.util.*

interface PaymentProvider {
    val name: String
    
    fun authorize(request: ProviderPaymentRequest): ProviderPaymentResponse
    fun capture(request: ProviderPaymentRequest): ProviderPaymentResponse
    fun supports(network: CardNetwork, currency: Currency, country: String): Boolean
    fun feeFor(currency: Currency, amount: Long): Fee
    fun health(): ProviderHealth
}

data class ProviderPaymentRequest(
    val amount: Long, // in minor units (cents)
    val currency: Currency,
    val paymentMethod: PaymentMethod,
    val merchantId: String,
    val idempotencyKey: String,
    val metadata: Map<String, String> = emptyMap()
)

data class ProviderPaymentResponse(
    val success: Boolean,
    val transactionId: String?,
    val status: ProviderTransactionStatus,
    val errorCode: String? = null,
    val errorMessage: String? = null,
    val metadata: Map<String, String> = emptyMap()
)

data class PaymentMethod(
    val type: PaymentMethodType,
    val card: CardDetails? = null
)

data class CardDetails(
    val number: String,
    val expiryMonth: Int,
    val expiryYear: Int,
    val cvv: String,
    val holderName: String,
    val network: CardNetwork
)

enum class PaymentMethodType {
    CARD,
    BANK_TRANSFER,
    WALLET
}

enum class ProviderTransactionStatus {
    AUTHORIZED,
    CAPTURED,
    FAILED,
    DECLINED,
    TIMEOUT
}

data class Fee(
    val amount: Long, // in minor units
    val currency: Currency,
    val type: FeeType
)

enum class FeeType {
    FIXED,
    PERCENTAGE,
    COMBINED
}

data class ProviderHealth(
    val healthy: Boolean,
    val latency: Long, // in milliseconds
    val successRate: Double, // 0.0 to 1.0
    val lastChecked: java.time.Instant
)
