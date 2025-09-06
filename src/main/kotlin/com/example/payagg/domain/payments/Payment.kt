package com.example.payagg.domain.payments

import com.example.payagg.domain.PaymentStatus
import jakarta.persistence.*
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.math.BigDecimal
import java.time.Instant
import java.util.*

@Entity
@Table(name = "payments")
@EntityListeners(AuditingEntityListener::class)
data class Payment(
    @Id
    val id: UUID = UUID.randomUUID(),
    
    @Column(name = "request_id")
    val requestId: UUID?,
    
    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    val amount: BigDecimal,
    
    @Column(name = "currency", nullable = false, length = 3)
    val currency: String,
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    val status: PaymentStatus,
    
    @Column(name = "merchant_id", nullable = false)
    val merchantId: UUID,
    
    @Column(name = "customer_id")
    val customerId: UUID?,
    
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
        requestId = null,
        amount = BigDecimal.ZERO,
        currency = "",
        status = PaymentStatus.INIT,
        merchantId = UUID.randomUUID(),
        customerId = null
    )
    
    fun updateStatus(newStatus: PaymentStatus): Payment {
        return this.copy(status = newStatus, lastModifiedAt = Instant.now())
    }
    
    fun canBeConfirmed(): Boolean {
        return status in listOf(PaymentStatus.INIT, PaymentStatus.REQUIRES_AUTHORISATION)
    }
    
    fun canBeCancelled(): Boolean {
        return status !in listOf(PaymentStatus.SUCCEEDED, PaymentStatus.CANCELLED)
    }
}
