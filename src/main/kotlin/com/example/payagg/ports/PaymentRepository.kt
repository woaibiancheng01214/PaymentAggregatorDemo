package com.example.payagg.ports

import com.example.payagg.domain.PaymentStatus
import com.example.payagg.domain.payments.Payment
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface PaymentRepository : JpaRepository<Payment, UUID> {
    fun findByMerchantId(merchantId: UUID): List<Payment>
    fun findByStatus(status: PaymentStatus): List<Payment>
    fun findByRequestId(requestId: UUID): Payment?
    
    @Query("SELECT p FROM Payment p WHERE p.merchantId = :merchantId AND p.status = :status")
    fun findByMerchantIdAndStatus(merchantId: UUID, status: PaymentStatus): List<Payment>
}
