package com.example.payagg.api.dto

import com.example.payagg.domain.PaymentStatus
import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import java.math.BigDecimal
import java.time.Instant
import java.util.*

data class CreatePaymentRequest(
    @field:NotNull
    @JsonProperty("request_id")
    val requestId: UUID?,
    
    @field:NotNull
    @field:Positive
    val amount: BigDecimal,
    
    @field:NotBlank
    val currency: String,
    
    @field:NotNull
    @JsonProperty("merchant_id")
    val merchantId: UUID,
    
    @JsonProperty("customer_id")
    val customerId: UUID?
)

data class UpdatePaymentRequest(
    val amount: BigDecimal?,
    val currency: String?,
    @JsonProperty("customer_id")
    val customerId: UUID?
)

data class PaymentResponse(
    val id: UUID,
    @JsonProperty("request_id")
    val requestId: UUID?,
    val amount: BigDecimal,
    val currency: String,
    val status: PaymentStatus,
    @JsonProperty("merchant_id")
    val merchantId: UUID,
    @JsonProperty("customer_id")
    val customerId: UUID?,
    @JsonProperty("created_at")
    val createdAt: Instant,
    @JsonProperty("last_modified_at")
    val lastModifiedAt: Instant
)

data class ConfirmPaymentRequest(
    @field:NotNull
    @JsonProperty("payment_method")
    val paymentMethod: PaymentMethodDto
)

data class PaymentMethodDto(
    val type: String,
    val card: CardDetailsDto?
)

data class CardDetailsDto(
    val number: String,
    @JsonProperty("expiry_month")
    val expiryMonth: Int,
    @JsonProperty("expiry_year")
    val expiryYear: Int,
    val cvv: String,
    @JsonProperty("holder_name")
    val holderName: String
)

data class PaymentAttemptResponse(
    val id: UUID,
    @JsonProperty("payment_id")
    val paymentId: UUID,
    val amount: BigDecimal,
    @JsonProperty("captured_amount")
    val capturedAmount: BigDecimal,
    val currency: String,
    val status: com.example.payagg.domain.PaymentAttemptStatus,
    @JsonProperty("merchant_id")
    val merchantId: UUID,
    @JsonProperty("routing_mode")
    val routingMode: com.example.payagg.domain.RoutingMode?,
    @JsonProperty("route_decision")
    val routeDecision: com.example.payagg.domain.RouteDecision?,
    @JsonProperty("provider_name")
    val providerName: String?,
    @JsonProperty("created_at")
    val createdAt: Instant,
    @JsonProperty("last_modified_at")
    val lastModifiedAt: Instant
)
