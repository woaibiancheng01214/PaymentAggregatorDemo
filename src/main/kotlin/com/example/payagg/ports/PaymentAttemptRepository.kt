package com.example.payagg.ports

import com.example.payagg.domain.PaymentAttemptStatus
import com.example.payagg.domain.payments.PaymentAttempt
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface PaymentAttemptRepository : JpaRepository<PaymentAttempt, UUID> {
    fun findByPaymentId(paymentId: UUID): List<PaymentAttempt>
    fun findByStatus(status: PaymentAttemptStatus): List<PaymentAttempt>
    fun findByProviderName(providerName: String): List<PaymentAttempt>
    
    @Query("SELECT pa FROM PaymentAttempt pa WHERE pa.paymentId = :paymentId ORDER BY pa.createdAt DESC")
    fun findByPaymentIdOrderByCreatedAtDesc(paymentId: UUID): List<PaymentAttempt>
    
    @Query("SELECT pa FROM PaymentAttempt pa WHERE pa.paymentId = :paymentId AND pa.status = :status")
    fun findByPaymentIdAndStatus(paymentId: UUID, status: PaymentAttemptStatus): List<PaymentAttempt>
}
