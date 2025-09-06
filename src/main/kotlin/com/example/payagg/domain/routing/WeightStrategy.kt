package com.example.payagg.domain.routing

import com.example.payagg.ports.PaymentProvider
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import kotlin.random.Random

@Component
class WeightStrategy : RoutingStrategy {
    
    private val logger = LoggerFactory.getLogger(WeightStrategy::class.java)
    override val name = "WEIGHT"
    
    override fun route(context: RoutingContext, candidates: List<PaymentProvider>): RoutingResult {
        if (candidates.isEmpty()) {
            return RoutingResult(
                providers = emptyList(),
                strategy = name,
                reason = "No candidates provided"
            )
        }
        
        // Get weights from configuration (simplified for demo)
        val weights = getProviderWeights()
        
        // Calculate weighted distribution
        val weightedProviders = candidates.map { provider ->
            val weight = weights[provider.name] ?: 1 // Default weight of 1
            provider to weight
        }
        
        // Sort by weight (highest first) with some randomization for A/B testing
        val sortedProviders = weightedProviders
            .sortedWith(compareByDescending<Pair<PaymentProvider, Int>> { it.second }
                .thenBy { Random.nextDouble() }) // Add randomness for equal weights
            .map { it.first }
        
        logger.info("Weight-based routing: ${sortedProviders.map { it.name }} (weights: ${weightedProviders.associate { it.first.name to it.second }})")
        
        return RoutingResult(
            providers = sortedProviders,
            strategy = name,
            reason = "Sorted providers by configured weights with randomization",
            metadata = mapOf<String, Any>(
                "provider_weights" to weightedProviders.associate { it.first.name to it.second },
                "highest_weight_provider" to (sortedProviders.firstOrNull()?.name ?: "")
            )
        )
    }
    
    private fun getProviderWeights(): Map<String, Int> {
        // In a real implementation, this would come from configuration
        return mapOf(
            "StripeMock" to 60,
            "AdyenMock" to 30,
            "LocalBankMock" to 10
        )
    }
}
