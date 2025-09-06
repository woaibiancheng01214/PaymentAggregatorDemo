package com.example.payagg.adapters.providers

import com.example.payagg.domain.CardNetwork
import com.example.payagg.ports.*
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.*

@Component
class AdyenMockAdapter : PaymentProvider {
    
    private val logger = LoggerFactory.getLogger(AdyenMockAdapter::class.java)
    override val name = "AdyenMock"
    
    private val supportedCurrencies = setOf(
        Currency.getInstance("USD"),
        Currency.getInstance("EUR"),
        Currency.getInstance("GBP")
    )
    
    private val supportedCountries = setOf("US", "NL", "GB", "DE")
    private val supportedNetworks = setOf(CardNetwork.VISA, CardNetwork.MASTERCARD, CardNetwork.AMEX)
    
    override fun authorize(request: ProviderPaymentRequest): ProviderPaymentResponse {
        logger.info("AdyenMock: Processing authorization for amount ${request.amount} ${request.currency}")
        
        // Reject AMEX for amounts over $1000
        if (request.paymentMethod.card?.network == CardNetwork.AMEX && request.amount > 100000) {
            return ProviderPaymentResponse(
                success = false,
                transactionId = null,
                status = ProviderTransactionStatus.DECLINED,
                errorCode = "AMEX_LIMIT_EXCEEDED",
                errorMessage = "AMEX transactions over $1000 not supported"
            )
        }
        
        // Simulate random failures for testing
        if (request.amount % 13 == 0L) {
            return ProviderPaymentResponse(
                success = false,
                transactionId = null,
                status = ProviderTransactionStatus.FAILED,
                errorCode = "PROCESSING_ERROR",
                errorMessage = "Payment processing failed"
            )
        }
        
        val transactionId = "adyen_${UUID.randomUUID()}"
        return ProviderPaymentResponse(
            success = true,
            transactionId = transactionId,
            status = ProviderTransactionStatus.AUTHORIZED,
            metadata = mapOf("provider" to name)
        )
    }
    
    override fun capture(request: ProviderPaymentRequest): ProviderPaymentResponse {
        logger.info("AdyenMock: Processing capture for amount ${request.amount} ${request.currency}")
        
        val transactionId = "adyen_capture_${UUID.randomUUID()}"
        return ProviderPaymentResponse(
            success = true,
            transactionId = transactionId,
            status = ProviderTransactionStatus.CAPTURED,
            metadata = mapOf("provider" to name)
        )
    }
    
    override fun supports(network: CardNetwork, currency: Currency, country: String): Boolean {
        return network in supportedNetworks && 
               currency in supportedCurrencies && 
               country in supportedCountries
    }
    
    override fun feeFor(currency: Currency, amount: Long): Fee {
        // 2.9% base fee
        val percentageFee = (amount * 290) / 10000 // 2.9% in basis points
        
        // Add surcharge for AMEX
        val surcharge = if (currency.currencyCode == "USD") 50 else 40 // Additional fee for AMEX
        
        return Fee(
            amount = percentageFee + surcharge,
            currency = currency,
            type = FeeType.COMBINED
        )
    }
    
    override fun health(): ProviderHealth {
        return ProviderHealth(
            healthy = true,
            latency = 200,
            successRate = 0.97,
            lastChecked = Instant.now()
        )
    }
}
