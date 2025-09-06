package com.example.payagg.domain.routing

import com.example.payagg.config.ConfigKeys
import com.example.payagg.ports.ConfigPort
import com.example.payagg.ports.PaymentProvider
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

/**
 * Load balancing scoring strategy.
 * 
 * Evaluates providers based on configured weights for traffic distribution
 * and capacity management. Providers with higher weights receive higher scores,
 * enabling controlled load distribution across the provider network.
 */
@Component
class LoadBalancingScoringStrategy(
    private val configPort: ConfigPort
) : ScoringStrategy {
    
    private val logger = LoggerFactory.getLogger(LoadBalancingScoringStrategy::class.java)
    
    override val strategyType = RoutingStrategyType.LOAD_BALANCING
    
    override fun calculateScore(
        context: RoutingContext,
        provider: PaymentProvider,
        allProviders: List<PaymentProvider>
    ): Double {
        return try {
            val weights = getProviderWeights()
            val providerWeight = weights[provider.name] ?: 0
            val maxWeight = weights.values.maxOrNull() ?: 1
            
            val score = if (maxWeight > 0) {
                providerWeight.toDouble() / maxWeight.toDouble()
            } else {
                0.5 // Default neutral score if no weights configured
            }
            
            logger.debug("Load balancing score for ${provider.name}: $score (weight: $providerWeight/$maxWeight)")
            score.coerceIn(0.0, 1.0)
            
        } catch (e: Exception) {
            logger.warn("Failed to calculate load balancing score for provider ${provider.name}", e)
            0.5 // Default neutral score on error
        }
    }
    
    override fun getScoreMetadata(
        context: RoutingContext,
        provider: PaymentProvider,
        score: Double
    ): Map<String, Any> {
        return try {
            val weights = getProviderWeights()
            val providerWeight = weights[provider.name] ?: 0
            val totalWeight = weights.values.sum()
            val percentage = if (totalWeight > 0) {
                (providerWeight.toDouble() / totalWeight.toDouble()) * 100
            } else {
                0.0
            }
            
            mapOf(
                "provider_weight" to providerWeight,
                "total_weight" to totalWeight,
                "weight_percentage" to String.format("%.1f%%", percentage),
                "all_weights" to weights
            )
        } catch (e: Exception) {
            mapOf("error" to "Failed to get load balancing metadata: ${e.message}")
        }
    }
    
    /**
     * Get provider weights from configuration
     */
    private fun getProviderWeights(): Map<String, Int> {
        return try {
            val weightsConfig = configPort.getConfig(ConfigKeys.ROUTING_WEIGHTS, Map::class.java)
            if (weightsConfig != null) {
                @Suppress("UNCHECKED_CAST")
                val configMap = weightsConfig as Map<String, Any>
                configMap.mapValues { (_, value) ->
                    when (value) {
                        is Number -> value.toInt()
                        is String -> value.toIntOrNull() ?: 0
                        else -> 0
                    }
                }
            } else {
                logger.warn("No ${ConfigKeys.ROUTING_WEIGHTS} configuration found, using default weights")
                getDefaultWeights()
            }
        } catch (e: Exception) {
            logger.error("Failed to load routing weights from configuration", e)
            getDefaultWeights()
        }
    }
    
    /**
     * Get default provider weights
     */
    private fun getDefaultWeights(): Map<String, Int> {
        logger.info("Using default hardcoded routing weights as fallback")
        return mapOf(
            "StripeMock" to 60,
            "AdyenMock" to 30,
            "LocalBankMock" to 10
        )
    }
    
    /**
     * Calculate load distribution across all providers
     */
    fun getLoadDistribution(allProviders: List<PaymentProvider>): LoadDistribution {
        val weights = getProviderWeights()
        val totalWeight = weights.values.sum()
        
        val distributions = allProviders.map { provider ->
            val weight = weights[provider.name] ?: 0
            val percentage = if (totalWeight > 0) {
                (weight.toDouble() / totalWeight.toDouble()) * 100
            } else {
                0.0
            }
            
            ProviderLoadInfo(
                providerName = provider.name,
                weight = weight,
                percentage = percentage
            )
        }
        
        return LoadDistribution(
            providers = distributions,
            totalWeight = totalWeight,
            isBalanced = isLoadBalanced(distributions)
        )
    }
    
    /**
     * Check if load is reasonably balanced across providers
     */
    private fun isLoadBalanced(distributions: List<ProviderLoadInfo>): Boolean {
        if (distributions.isEmpty()) return true
        
        val percentages = distributions.map { it.percentage }
        val maxPercentage = percentages.maxOrNull() ?: 0.0
        val minPercentage = percentages.minOrNull() ?: 0.0
        
        // Consider balanced if no single provider has more than 70% of traffic
        // and the difference between max and min is less than 60%
        return maxPercentage <= 70.0 && (maxPercentage - minPercentage) <= 60.0
    }
    
    /**
     * Get recommended weight adjustments for better load balancing
     */
    fun getRecommendedWeights(allProviders: List<PaymentProvider>): Map<String, Int> {
        val currentDistribution = getLoadDistribution(allProviders)
        
        return if (currentDistribution.isBalanced) {
            // Already balanced, return current weights
            getProviderWeights()
        } else {
            // Suggest more balanced weights
            val providerCount = allProviders.size
            val baseWeight = 100 / providerCount
            
            allProviders.associate { provider ->
                provider.name to when (provider.name) {
                    "StripeMock" -> baseWeight + 10  // Slightly favor Stripe
                    "AdyenMock" -> baseWeight
                    "LocalBankMock" -> baseWeight - 5  // Slightly reduce LocalBank
                    else -> baseWeight
                }
            }
        }
    }
}

/**
 * Load information for a single provider
 */
data class ProviderLoadInfo(
    val providerName: String,
    val weight: Int,
    val percentage: Double
)

/**
 * Load distribution across all providers
 */
data class LoadDistribution(
    val providers: List<ProviderLoadInfo>,
    val totalWeight: Int,
    val isBalanced: Boolean
) {
    /**
     * Get the provider with the highest load
     */
    fun getHighestLoadProvider(): ProviderLoadInfo? = providers.maxByOrNull { it.percentage }
    
    /**
     * Get the provider with the lowest load
     */
    fun getLowestLoadProvider(): ProviderLoadInfo? = providers.minByOrNull { it.percentage }
    
    /**
     * Calculate load imbalance ratio (higher = more imbalanced)
     */
    fun getImbalanceRatio(): Double {
        val percentages = providers.map { it.percentage }
        val max = percentages.maxOrNull() ?: 0.0
        val min = percentages.minOrNull() ?: 0.0
        
        return if (min > 0) max / min else Double.MAX_VALUE
    }
}
