package com.example.payagg.adapters.providers

import com.example.payagg.domain.CardNetwork
import com.example.payagg.ports.*
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.*

@Component
class StripeMockAdapter : PaymentProvider {
    
    private val logger = LoggerFactory.getLogger(StripeMockAdapter::class.java)
    override val name = "StripeMock"
    
    private val supportedCurrencies = setOf(
        Currency.getInstance("USD"),
        Currency.getInstance("EUR")
    )
    
    private val supportedCountries = setOf("US", "GB", "DE")
    private val supportedNetworks = setOf(CardNetwork.VISA, CardNetwork.MASTERCARD)
    
    override fun authorize(request: ProviderPaymentRequest): ProviderPaymentResponse {
        logger.info("StripeMock: Processing authorization for amount ${request.amount} ${request.currency}")
        
        // Simulate soft decline for amounts ending with 37
        if (request.amount % 100 == 37L) {
            return ProviderPaymentResponse(
                success = false,
                transactionId = null,
                status = ProviderTransactionStatus.DECLINED,
                errorCode = "SOFT_DECLINE",
                errorMessage = "Insufficient funds - please try again"
            )
        }
        
        // Simulate timeout for amounts ending with 99
        if (request.amount % 100 == 99L) {
            Thread.sleep(5000) // Simulate timeout
            return ProviderPaymentResponse(
                success = false,
                transactionId = null,
                status = ProviderTransactionStatus.TIMEOUT,
                errorCode = "TIMEOUT",
                errorMessage = "Request timeout"
            )
        }
        
        val transactionId = "stripe_${UUID.randomUUID()}"
        return ProviderPaymentResponse(
            success = true,
            transactionId = transactionId,
            status = ProviderTransactionStatus.AUTHORIZED,
            metadata = mapOf("provider" to name)
        )
    }
    
    override fun capture(request: ProviderPaymentRequest): ProviderPaymentResponse {
        logger.info("StripeMock: Processing capture for amount ${request.amount} ${request.currency}")
        
        val transactionId = "stripe_capture_${UUID.randomUUID()}"
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
        // 3.0% + 30 cents
        val percentageFee = (amount * 300) / 10000 // 3.0% in basis points
        val fixedFee = if (currency.currencyCode == "USD") 30 else 25 // cents
        
        return Fee(
            amount = percentageFee + fixedFee,
            currency = currency,
            type = FeeType.COMBINED
        )
    }
    
    override fun health(): ProviderHealth {
        return ProviderHealth(
            healthy = true,
            latency = 150,
            successRate = 0.95,
            lastChecked = Instant.now()
        )
    }
}
