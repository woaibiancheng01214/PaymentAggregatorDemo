package com.example.payagg.api

import com.example.payagg.api.dto.*
import com.example.payagg.domain.payments.Payment
import com.example.payagg.domain.payments.PaymentAttempt
import com.example.payagg.domain.payments.PaymentService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/payments")
@Tag(name = "Payments", description = "Payment management operations")
class PaymentController(
    private val paymentService: PaymentService
) {
    
    @PostMapping
    @Operation(summary = "Create a new payment")
    fun createPayment(@Valid @RequestBody request: CreatePaymentRequest): ResponseEntity<PaymentResponse> {
        val payment = paymentService.createPayment(
            requestId = request.requestId,
            amount = request.amount,
            currency = request.currency,
            merchantId = request.merchantId,
            customerId = request.customerId
        )
        
        return ResponseEntity.status(HttpStatus.CREATED).body(payment.toResponse())
    }
    
    @GetMapping("/{id}")
    @Operation(summary = "Get payment by ID")
    fun getPayment(@PathVariable id: UUID): ResponseEntity<PaymentResponse> {
        val payment = paymentService.getPayment(id)
            ?: return ResponseEntity.notFound().build()
        
        return ResponseEntity.ok(payment.toResponse())
    }
    
    @PatchMapping("/{id}")
    @Operation(summary = "Update payment")
    fun updatePayment(
        @PathVariable id: UUID,
        @Valid @RequestBody request: UpdatePaymentRequest
    ): ResponseEntity<PaymentResponse> {
        val payment = paymentService.updatePayment(
            paymentId = id,
            amount = request.amount,
            currency = request.currency,
            customerId = request.customerId
        )
        
        return ResponseEntity.ok(payment.toResponse())
    }
    
    @PostMapping("/{id}/confirm")
    @Operation(summary = "Confirm payment with payment method")
    fun confirmPayment(
        @PathVariable id: UUID,
        @Valid @RequestBody request: ConfirmPaymentRequest
    ): ResponseEntity<PaymentAttemptResponse> {
        val paymentMethod = mapOf(
            "type" to request.paymentMethod.type,
            "card" to mapOf(
                "number" to (request.paymentMethod.card?.number ?: ""),
                "expiry_month" to (request.paymentMethod.card?.expiryMonth ?: 0),
                "expiry_year" to (request.paymentMethod.card?.expiryYear ?: 0),
                "cvv" to (request.paymentMethod.card?.cvv ?: ""),
                "holder_name" to (request.paymentMethod.card?.holderName ?: "")
            )
        )
        
        val paymentAttempt = paymentService.confirmPayment(id, paymentMethod)
        
        return ResponseEntity.ok(paymentAttempt.toResponse())
    }
    
    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel payment")
    fun cancelPayment(@PathVariable id: UUID): ResponseEntity<PaymentResponse> {
        val payment = paymentService.cancelPayment(id)
        return ResponseEntity.ok(payment.toResponse())
    }
    
    private fun Payment.toResponse(): PaymentResponse {
        return PaymentResponse(
            id = id,
            requestId = requestId,
            amount = amount,
            currency = currency,
            status = status,
            merchantId = merchantId,
            customerId = customerId,
            createdAt = createdAt,
            lastModifiedAt = lastModifiedAt
        )
    }
    
    private fun PaymentAttempt.toResponse(): PaymentAttemptResponse {
        return PaymentAttemptResponse(
            id = id,
            paymentId = paymentId,
            amount = amount,
            capturedAmount = capturedAmount,
            currency = currency,
            status = status,
            merchantId = merchantId,
            routingMode = routingMode,
            routeDecision = routeDecision,
            providerName = providerName,
            createdAt = createdAt,
            lastModifiedAt = lastModifiedAt
        )
    }
}
