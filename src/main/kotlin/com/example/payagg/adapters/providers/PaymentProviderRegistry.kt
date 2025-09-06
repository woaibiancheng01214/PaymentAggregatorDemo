package com.example.payagg.adapters.providers

import com.example.payagg.domain.CardNetwork
import com.example.payagg.ports.PaymentProvider
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.*

@Component
class PaymentProviderRegistry(
    private val providers: List<PaymentProvider>
) {
    
    private val logger = LoggerFactory.getLogger(PaymentProviderRegistry::class.java)
    
    init {
        logger.info("Registered payment providers: ${providers.map { it.name }}")
    }
    
    fun getProvider(name: String): PaymentProvider? {
        return providers.find { it.name == name }
    }
    
    fun getAllProviders(): List<PaymentProvider> {
        return providers.toList()
    }
    
    fun getEligibleProviders(
        network: CardNetwork,
        currency: Currency,
        country: String
    ): List<PaymentProvider> {
        return providers.filter { provider ->
            provider.supports(network, currency, country)
        }.also { eligible ->
            logger.debug("Found ${eligible.size} eligible providers for $network, $currency, $country: ${eligible.map { it.name }}")
        }
    }
    
    fun getHealthyProviders(): List<PaymentProvider> {
        return providers.filter { provider ->
            try {
                provider.health().healthy
            } catch (e: Exception) {
                logger.warn("Failed to check health for provider ${provider.name}", e)
                false
            }
        }
    }
    
    fun getProvidersByNames(names: List<String>): List<PaymentProvider> {
        return names.mapNotNull { name ->
            getProvider(name).also { provider ->
                if (provider == null) {
                    logger.warn("Provider not found: $name")
                }
            }
        }
    }
}
