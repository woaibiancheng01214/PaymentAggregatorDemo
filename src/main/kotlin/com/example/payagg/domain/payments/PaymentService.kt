package com.example.payagg.domain.payments

import com.example.payagg.domain.*
import com.example.payagg.domain.routing.RoutingContext
import com.example.payagg.domain.routing.RoutingEngine
import com.example.payagg.ports.*
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.Instant
import java.util.*

@Service
@Transactional
class PaymentService(
    private val paymentRepository: PaymentRepository,
    private val paymentAttemptRepository: PaymentAttemptRepository,
    private val merchantRepository: MerchantRepository,
    private val customerRepository: CustomerRepository,
    private val routingEngine: RoutingEngine,
) {
    
    private val logger = LoggerFactory.getLogger(PaymentService::class.java)
    
    fun createPayment(
        requestId: UUID?,
        amount: BigDecimal,
        currency: String,
        merchantId: UUID,
        customerId: UUID?
    ): Payment {
        // Validate merchant exists
        val merchant = merchantRepository.findById(merchantId)
            .orElseThrow { IllegalArgumentException("Merchant not found: $merchantId") }
        
        // Validate customer exists if provided
        if (customerId != null) {
            customerRepository.findById(customerId)
                .orElseThrow { IllegalArgumentException("Customer not found: $customerId") }
        }
        
        // Check for duplicate request ID
        if (requestId != null) {
            paymentRepository.findByRequestId(requestId)?.let {
                throw IllegalArgumentException("Payment with request ID $requestId already exists")
            }
        }
        
        val payment = Payment(
            requestId = requestId,
            amount = amount,
            currency = currency,
            status = PaymentStatus.INIT,
            merchantId = merchantId,
            customerId = customerId
        )
        
        val savedPayment = paymentRepository.save(payment)
        logger.info("Created payment ${savedPayment.id} for merchant $merchantId")
        
        return savedPayment
    }
    
    fun getPayment(paymentId: UUID): Payment? {
        return paymentRepository.findById(paymentId).orElse(null)
    }
    
    fun updatePayment(
        paymentId: UUID,
        amount: BigDecimal?,
        currency: String?,
        customerId: UUID?
    ): Payment {
        val payment = paymentRepository.findById(paymentId)
            .orElseThrow { IllegalArgumentException("Payment not found: $paymentId") }
        
        if (!payment.canBeConfirmed()) {
            throw IllegalStateException("Payment ${payment.id} cannot be updated in status ${payment.status}")
        }
        
        val updatedPayment = payment.copy(
            amount = amount ?: payment.amount,
            currency = currency ?: payment.currency,
            customerId = customerId ?: payment.customerId,
            lastModifiedAt = Instant.now()
        )
        
        return paymentRepository.save(updatedPayment)
    }
    
    fun confirmPayment(paymentId: UUID, paymentMethod: Map<String, Any>): PaymentAttempt {
        val payment = paymentRepository.findById(paymentId)
            .orElseThrow { IllegalArgumentException("Payment not found: $paymentId") }
        
        if (!payment.canBeConfirmed()) {
            throw IllegalStateException("Payment ${payment.id} cannot be confirmed in status ${payment.status}")
        }
        
        // Extract card network from payment method for routing
        val cardNetwork = extractCardNetwork(paymentMethod)
        val currency = Currency.getInstance(payment.currency)
        val merchant = merchantRepository.findById(payment.merchantId).orElseThrow()
        
        // Create routing context
        val routingContext = RoutingContext(
            amount = payment.amount, // Use BigDecimal directly for precision
            currency = currency,
            country = merchant.country,
            cardNetwork = cardNetwork,
            binRange = extractBinRange(paymentMethod),
            merchantId = payment.merchantId.toString()
        )
        
        // Get routing decision
        val routeDecision = routingEngine.route(routingContext)
        
        if (routeDecision.selectedProvider == null) {
            throw IllegalStateException("No payment provider available for this payment")
        }
        
        // Create payment attempt
        val paymentAttempt = PaymentAttempt(
            paymentId = payment.id,
            amount = payment.amount,
            currency = payment.currency,
            status = PaymentAttemptStatus.RECEIVED,
            merchantId = payment.merchantId,
            paymentMethod = paymentMethod,
            failureDetails = null,
            routingMode = RoutingMode.SMART,
            routeDecision = routeDecision,
            providerName = null,
            providerTransactionId = null
        )
        
        val savedAttempt: PaymentAttempt = paymentAttemptRepository.save(paymentAttempt)

        // Update payment status
        val updatedPayment = payment.updateStatus(PaymentStatus.PENDING)
        paymentRepository.save(updatedPayment)

        logger.info("Created payment attempt ${savedAttempt.id} for payment ${payment.id}")
        
        return savedAttempt
    }
    
    fun cancelPayment(paymentId: UUID): Payment {
        val payment = paymentRepository.findById(paymentId)
            .orElseThrow { IllegalArgumentException("Payment not found: $paymentId") }
        
        if (!payment.canBeCancelled()) {
            throw IllegalStateException("Payment ${payment.id} cannot be cancelled in status ${payment.status}")
        }
        
        val cancelledPayment = payment.updateStatus(PaymentStatus.CANCELLED)
        return paymentRepository.save(cancelledPayment)
    }
    
    fun getPaymentsByMerchant(merchantId: UUID): List<Payment> {
        return paymentRepository.findByMerchantId(merchantId)
    }
    
    private fun extractCardNetwork(paymentMethod: Map<String, Any>): CardNetwork {
        val card = paymentMethod["card"] as? Map<String, Any>
        val number = card?.get("number") as? String ?: ""
        
        // Simple BIN-based network detection (simplified for demo)
        return when {
            number.startsWith("4") -> CardNetwork.VISA
            number.startsWith("5") || number.startsWith("2") -> CardNetwork.MASTERCARD
            number.startsWith("3") -> CardNetwork.AMEX
            else -> CardNetwork.VISA // Default
        }
    }
    
    private fun extractBinRange(paymentMethod: Map<String, Any>): String? {
        val card = paymentMethod["card"] as? Map<String, Any>
        val number = card?.get("number") as? String
        return number?.take(6) // First 6 digits
    }
}
