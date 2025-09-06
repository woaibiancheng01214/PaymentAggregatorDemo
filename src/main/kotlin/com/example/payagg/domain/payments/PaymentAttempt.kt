package com.example.payagg.domain.payments

import com.example.payagg.domain.FailureDetails
import com.example.payagg.domain.PaymentAttemptStatus
import com.example.payagg.domain.RouteDecision
import com.example.payagg.domain.RoutingMode
import io.hypersistence.utils.hibernate.type.json.JsonType
import jakarta.persistence.*
import org.hibernate.annotations.Type
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.math.BigDecimal
import java.time.Instant
import java.util.*

@Entity
@Table(name = "payment_attempts")
@EntityListeners(AuditingEntityListener::class)
data class PaymentAttempt(
    @Id
    val id: UUID = UUID.randomUUID(),
    
    @Column(name = "payment_id", nullable = false)
    val paymentId: UUID,
    
    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    val amount: BigDecimal,
    
    @Column(name = "captured_amount", precision = 19, scale = 2)
    val capturedAmount: BigDecimal = BigDecimal.ZERO,
    
    @Column(name = "currency", nullable = false, length = 3)
    val currency: String,
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    val status: PaymentAttemptStatus,
    
    @Column(name = "merchant_id", nullable = false)
    val merchantId: UUID,
    
    @Type(JsonType::class)
    @Column(name = "payment_method", columnDefinition = "jsonb")
    val paymentMethod: Map<String, Any>?,
    
    @Type(JsonType::class)
    @Column(name = "failure_details", columnDefinition = "jsonb")
    val failureDetails: FailureDetails?,
    
    @Enumerated(EnumType.STRING)
    @Column(name = "routing_mode")
    val routingMode: RoutingMode?,
    
    @Type(JsonType::class)
    @Column(name = "route_decision", columnDefinition = "jsonb")
    val routeDecision: RouteDecision?,
    
    @Column(name = "provider_name")
    val providerName: String?,
    
    @Column(name = "provider_transaction_id")
    val providerTransactionId: String?,
    
    @Version
    val version: Long = 0,
    
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),
    
    @LastModifiedDate
    @Column(name = "last_modified_at", nullable = false)
    val lastModifiedAt: Instant = Instant.now()
) {
    // JPA requires no-arg constructor
    constructor() : this(
        id = UUID.randomUUID(),
        paymentId = UUID.randomUUID(),
        amount = BigDecimal.ZERO,
        currency = "",
        status = PaymentAttemptStatus.RECEIVED,
        merchantId = UUID.randomUUID(),
        paymentMethod = null,
        failureDetails = null,
        routingMode = null,
        routeDecision = null,
        providerName = null,
        providerTransactionId = null
    )
    
    fun updateStatus(newStatus: PaymentAttemptStatus): PaymentAttempt {
        return this.copy(status = newStatus, lastModifiedAt = Instant.now())
    }
    
    fun markAsAuthorised(providerTransactionId: String): PaymentAttempt {
        return this.copy(
            status = PaymentAttemptStatus.AUTHORISED,
            providerTransactionId = providerTransactionId,
            lastModifiedAt = Instant.now()
        )
    }
    
    fun markAsCaptured(capturedAmount: BigDecimal): PaymentAttempt {
        return this.copy(
            status = PaymentAttemptStatus.CAPTURED,
            capturedAmount = capturedAmount,
            lastModifiedAt = Instant.now()
        )
    }
    
    fun markAsFailed(failureDetails: FailureDetails): PaymentAttempt {
        return this.copy(
            status = PaymentAttemptStatus.FAILED,
            failureDetails = failureDetails,
            lastModifiedAt = Instant.now()
        )
    }
}
