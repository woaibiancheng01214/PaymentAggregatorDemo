package com.example.payagg.adapters.providers

import com.example.payagg.domain.CardNetwork
import com.example.payagg.ports.*
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.math.RoundingMode
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
        if (request.paymentMethod.card?.network == CardNetwork.AMEX && request.amount > BigDecimal("1000.00")) {
            return ProviderPaymentResponse(
                success = false,
                transactionId = null,
                status = ProviderTransactionStatus.DECLINED,
                errorCode = "AMEX_LIMIT_EXCEEDED",
                errorMessage = "AMEX transactions over $1000 not supported"
            )
        }
        
        // Simulate random failures for testing (for amounts divisible by 13)
        val amountCents = request.amount.multiply(BigDecimal("100")).toLong()
        if (amountCents % 13 == 0L) {
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
    
    override fun feeFor(currency: Currency, amount: BigDecimal): Fee {
        // 2.9% base fee
        val percentageRate = BigDecimal("0.029") // 2.9%
        val percentageFee = amount.multiply(percentageRate).setScale(2, RoundingMode.HALF_UP)

        // Add surcharge for AMEX (higher fees for premium cards)
        val surcharge = if (currency.currencyCode == "USD") BigDecimal("0.50") else BigDecimal("0.40")

        return Fee(
            amount = percentageFee.add(surcharge),
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
