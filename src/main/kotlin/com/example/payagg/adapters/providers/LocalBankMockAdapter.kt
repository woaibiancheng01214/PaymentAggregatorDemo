package com.example.payagg.adapters.providers

import com.example.payagg.domain.CardNetwork
import com.example.payagg.ports.*
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.util.*

@Component
class LocalBankMockAdapter : PaymentProvider {
    
    private val logger = LoggerFactory.getLogger(LocalBankMockAdapter::class.java)
    override val name = "LocalBankMock"
    
    @Value("\${payagg.providers.localbank.healthy:true}")
    private var isHealthy: Boolean = true
    
    private val supportedCurrencies = setOf(Currency.getInstance("USD"))
    private val supportedCountries = setOf("US")
    private val supportedNetworks = setOf(CardNetwork.VISA, CardNetwork.MASTERCARD)
    
    // Domestic BIN ranges (simplified for demo)
    private val domesticBinRanges = setOf(
        "411111", "424242", "400000", "401288"
    )
    
    override fun authorize(request: ProviderPaymentRequest): ProviderPaymentResponse {
        logger.info("LocalBankMock: Processing authorization for amount ${request.amount} ${request.currency}")
        
        // Check if provider is healthy
        if (!isHealthy) {
            return ProviderPaymentResponse(
                success = false,
                transactionId = null,
                status = ProviderTransactionStatus.TIMEOUT,
                errorCode = "SERVICE_UNAVAILABLE",
                errorMessage = "LocalBank service is currently unavailable"
            )
        }
        
        // Check if card is domestic
        val cardNumber = request.paymentMethod.card?.number ?: ""
        val isDomesticCard = domesticBinRanges.any { cardNumber.startsWith(it) }
        
        if (!isDomesticCard) {
            return ProviderPaymentResponse(
                success = false,
                transactionId = null,
                status = ProviderTransactionStatus.DECLINED,
                errorCode = "FOREIGN_CARD_NOT_SUPPORTED",
                errorMessage = "Only domestic cards are supported"
            )
        }
        
        // Simulate occasional network issues (for amounts divisible by 17)
        val amountCents = request.amount.multiply(BigDecimal("100")).toLong()
        if (amountCents % 17 == 0L) {
            return ProviderPaymentResponse(
                success = false,
                transactionId = null,
                status = ProviderTransactionStatus.TIMEOUT,
                errorCode = "NETWORK_ERROR",
                errorMessage = "Network timeout occurred"
            )
        }
        
        val transactionId = "localbank_${UUID.randomUUID()}"
        return ProviderPaymentResponse(
            success = true,
            transactionId = transactionId,
            status = ProviderTransactionStatus.AUTHORIZED,
            metadata = mapOf(
                "provider" to name,
                "domestic_card" to "true"
            )
        )
    }
    
    override fun capture(request: ProviderPaymentRequest): ProviderPaymentResponse {
        logger.info("LocalBankMock: Processing capture for amount ${request.amount} ${request.currency}")
        
        if (!isHealthy) {
            return ProviderPaymentResponse(
                success = false,
                transactionId = null,
                status = ProviderTransactionStatus.TIMEOUT,
                errorCode = "SERVICE_UNAVAILABLE",
                errorMessage = "LocalBank service is currently unavailable"
            )
        }
        
        val transactionId = "localbank_capture_${UUID.randomUUID()}"
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
        // Great fees for domestic cards: 2.2% + 20 cents
        val percentageRate = BigDecimal("0.022") // 2.2%
        val percentageFee = amount.multiply(percentageRate).setScale(2, RoundingMode.HALF_UP)
        val fixedFee = BigDecimal("0.20") // 20 cents

        return Fee(
            amount = percentageFee.add(fixedFee),
            currency = currency,
            type = FeeType.COMBINED
        )
    }
    
    override fun health(): ProviderHealth {
        return ProviderHealth(
            healthy = isHealthy,
            latency = if (isHealthy) 100 else 5000,
            successRate = if (isHealthy) 0.98 else 0.60,
            lastChecked = Instant.now()
        )
    }
    
    // Admin method to toggle health for testing
    fun setHealthy(healthy: Boolean) {
        this.isHealthy = healthy
        logger.info("LocalBankMock health status changed to: $healthy")
    }
}
