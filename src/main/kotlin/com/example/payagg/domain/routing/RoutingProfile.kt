package com.example.payagg.domain.routing

/**
 * Routing profile configuration for predefined strategy combinations.
 * 
 * Profiles allow easy switching between different routing behaviors without
 * manually configuring individual strategies and weights.
 */
data class RoutingProfile(
    val name: String,
    val description: String,
    val strategies: Set<RoutingStrategyType>,
    val weights: Map<RoutingStrategyType, Double>
) {
    init {
        // Validate that all strategies in weights are also in strategies set
        val extraWeights = weights.keys - strategies
        require(extraWeights.isEmpty()) {
            "Profile '$name' has weights for strategies not in strategy list: $extraWeights"
        }
        
        // Validate that weights sum to 1.0 (with small tolerance for floating point)
        val totalWeight = weights.values.sum()
        require(kotlin.math.abs(totalWeight - 1.0) < 0.001) {
            "Profile '$name' weights must sum to 1.0, got $totalWeight"
        }
    }
    
    /**
     * Get normalized weights for all strategies (missing strategies get 0.0)
     */
    fun getNormalizedWeights(): Map<RoutingStrategyType, Double> {
        return RoutingStrategyType.getAllStrategies().associateWith { strategy ->
            weights[strategy] ?: 0.0
        }
    }
    
    /**
     * Check if this profile uses only a single strategy
     */
    fun isSingleStrategy(): Boolean = strategies.size == 1
    
    /**
     * Get the single strategy if this is a single-strategy profile
     */
    fun getSingleStrategy(): RoutingStrategyType? = strategies.singleOrNull()
    
    companion object {
        /**
         * Predefined routing profiles for common use cases
         */
        fun getBuiltInProfiles(): Map<String, RoutingProfile> = mapOf(
            
            // Single strategy profiles
            "cost_optimized" to RoutingProfile(
                name = "cost_optimized",
                description = "Pure cost optimization - always choose the cheapest provider",
                strategies = setOf(RoutingStrategyType.COST),
                weights = mapOf(RoutingStrategyType.COST to 1.0)
            ),
            
            "rules_only" to RoutingProfile(
                name = "rules_only", 
                description = "Strict business rules compliance - follow configured rules exactly",
                strategies = setOf(RoutingStrategyType.RULES),
                weights = mapOf(RoutingStrategyType.RULES to 1.0)
            ),
            
            "reliability_focused" to RoutingProfile(
                name = "reliability_focused",
                description = "Prioritize provider reliability and success rates",
                strategies = setOf(RoutingStrategyType.SUCCESS_RATE),
                weights = mapOf(RoutingStrategyType.SUCCESS_RATE to 1.0)
            ),
            
            "load_balancing_only" to RoutingProfile(
                name = "load_balancing_only",
                description = "Pure load balancing based on configured weights",
                strategies = setOf(RoutingStrategyType.LOAD_BALANCING),
                weights = mapOf(RoutingStrategyType.LOAD_BALANCING to 1.0)
            ),
            
            // Multi-strategy profiles
            "cost_and_rules" to RoutingProfile(
                name = "cost_and_rules",
                description = "Balance cost optimization with business rules",
                strategies = setOf(RoutingStrategyType.COST, RoutingStrategyType.RULES),
                weights = mapOf(
                    RoutingStrategyType.RULES to 0.6,
                    RoutingStrategyType.COST to 0.4
                )
            ),
            
            "reliability_and_rules" to RoutingProfile(
                name = "reliability_and_rules",
                description = "Prioritize reliability while respecting business rules",
                strategies = setOf(RoutingStrategyType.SUCCESS_RATE, RoutingStrategyType.RULES),
                weights = mapOf(
                    RoutingStrategyType.RULES to 0.4,
                    RoutingStrategyType.SUCCESS_RATE to 0.6
                )
            ),
            
            "cost_and_reliability" to RoutingProfile(
                name = "cost_and_reliability",
                description = "Balance cost optimization with provider reliability",
                strategies = setOf(RoutingStrategyType.COST, RoutingStrategyType.SUCCESS_RATE),
                weights = mapOf(
                    RoutingStrategyType.COST to 0.6,
                    RoutingStrategyType.SUCCESS_RATE to 0.4
                )
            ),
            
            "balanced" to RoutingProfile(
                name = "balanced",
                description = "Balanced approach using all strategies with default weights",
                strategies = RoutingStrategyType.getAllStrategies().toSet(),
                weights = RoutingStrategyType.getDefaultWeights()
            ),
            
            "business_first" to RoutingProfile(
                name = "business_first",
                description = "Business rules first, then cost and reliability",
                strategies = setOf(
                    RoutingStrategyType.RULES,
                    RoutingStrategyType.COST,
                    RoutingStrategyType.SUCCESS_RATE
                ),
                weights = mapOf(
                    RoutingStrategyType.RULES to 0.6,
                    RoutingStrategyType.COST to 0.25,
                    RoutingStrategyType.SUCCESS_RATE to 0.15
                )
            ),
            
            "performance_optimized" to RoutingProfile(
                name = "performance_optimized",
                description = "Optimize for speed and reliability, minimal cost consideration",
                strategies = setOf(
                    RoutingStrategyType.SUCCESS_RATE,
                    RoutingStrategyType.LOAD_BALANCING,
                    RoutingStrategyType.COST
                ),
                weights = mapOf(
                    RoutingStrategyType.SUCCESS_RATE to 0.5,
                    RoutingStrategyType.LOAD_BALANCING to 0.3,
                    RoutingStrategyType.COST to 0.2
                )
            )
        )
        
        /**
         * Get profile by name, including built-in profiles
         */
        fun getProfile(name: String, customProfiles: Map<String, RoutingProfile> = emptyMap()): RoutingProfile? {
            return customProfiles[name] ?: getBuiltInProfiles()[name]
        }
        
        /**
         * Get all available profile names
         */
        fun getAllProfileNames(customProfiles: Map<String, RoutingProfile> = emptyMap()): Set<String> {
            return getBuiltInProfiles().keys + customProfiles.keys
        }
        
        /**
         * Validate a profile configuration
         */
        fun validateProfile(profile: RoutingProfile): List<String> {
            val errors = mutableListOf<String>()
            
            if (profile.strategies.isEmpty()) {
                errors.add("Profile '${profile.name}' must have at least one strategy")
            }
            
            if (profile.weights.isEmpty()) {
                errors.add("Profile '${profile.name}' must have at least one weight")
            }
            
            val missingWeights = profile.strategies - profile.weights.keys
            if (missingWeights.isNotEmpty()) {
                errors.add("Profile '${profile.name}' missing weights for strategies: $missingWeights")
            }
            
            return errors
        }
    }
}
