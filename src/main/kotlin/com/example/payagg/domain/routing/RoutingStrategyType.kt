package com.example.payagg.domain.routing

/**
 * Enumeration of available routing strategies.
 * 
 * Each strategy evaluates providers based on different criteria and assigns scores
 * that can be combined in composite routing decisions.
 */
enum class RoutingStrategyType(
    val displayName: String,
    val description: String,
    val defaultWeight: Double
) {
    /**
     * Rules-based routing strategy.
     * Evaluates providers based on business rules and preferences.
     * Score: 1.0 = strict rule match, 0.8 = preferred, 0.5 = neutral, 0.0 = excluded
     */
    RULES(
        displayName = "Rules",
        description = "Business rules and provider preferences",
        defaultWeight = 0.4
    ),
    
    /**
     * Cost-based routing strategy.
     * Evaluates providers based on transaction fees and costs.
     * Score: 1.0 = cheapest, 0.0 = most expensive (normalized across all providers)
     */
    COST(
        displayName = "Cost",
        description = "Transaction fees and cost optimization",
        defaultWeight = 0.3
    ),
    
    /**
     * Success rate routing strategy.
     * Evaluates providers based on historical success rates and reliability.
     * Score: 1.0 = highest success rate, 0.0 = lowest success rate
     */
    SUCCESS_RATE(
        displayName = "Success Rate",
        description = "Provider reliability and success rates",
        defaultWeight = 0.2
    ),
    
    /**
     * Load balancing routing strategy.
     * Evaluates providers based on configured weights for traffic distribution.
     * Score: 1.0 = highest weight, 0.0 = lowest weight (normalized)
     */
    LOAD_BALANCING(
        displayName = "Load Balancing",
        description = "Traffic distribution and capacity management",
        defaultWeight = 0.1
    );
    
    companion object {
        /**
         * Get all strategy types as a list
         */
        fun getAllStrategies(): List<RoutingStrategyType> = values().toList()
        
        /**
         * Get default strategy weights as a map
         */
        fun getDefaultWeights(): Map<RoutingStrategyType, Double> {
            return values().associate { it to it.defaultWeight }
        }
        
        /**
         * Validate that strategy weights sum to 1.0
         */
        fun validateWeights(weights: Map<RoutingStrategyType, Double>): Boolean {
            val total = weights.values.sum()
            return kotlin.math.abs(total - 1.0) < 0.001 // Allow small floating point errors
        }
        
        /**
         * Normalize weights to sum to 1.0
         */
        fun normalizeWeights(weights: Map<RoutingStrategyType, Double>): Map<RoutingStrategyType, Double> {
            val total = weights.values.sum()
            return if (total > 0) {
                weights.mapValues { it.value / total }
            } else {
                getDefaultWeights()
            }
        }
    }
}
