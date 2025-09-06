package com.example.payagg.domain.routing

import com.example.payagg.ports.PaymentProvider

/**
 * Interface for scoring strategies that evaluate providers and assign scores.
 * 
 * Unlike the legacy RoutingStrategy interface that filtered providers,
 * ScoringStrategy assigns numerical scores (0.0 to 1.0) that can be
 * combined in composite routing decisions.
 */
interface ScoringStrategy {
    
    /**
     * The type of this scoring strategy
     */
    val strategyType: RoutingStrategyType
    
    /**
     * Calculate a score for a provider in the given context.
     * 
     * @param context The routing context containing payment details
     * @param provider The provider to evaluate
     * @param allProviders All available providers (for relative scoring)
     * @return Score between 0.0 (worst) and 1.0 (best)
     */
    fun calculateScore(
        context: RoutingContext,
        provider: PaymentProvider,
        allProviders: List<PaymentProvider>
    ): Double
    
    /**
     * Get metadata about the scoring calculation for debugging/auditing.
     * 
     * @param context The routing context
     * @param provider The provider that was scored
     * @param score The calculated score
     * @return Metadata map with scoring details
     */
    fun getScoreMetadata(
        context: RoutingContext,
        provider: PaymentProvider,
        score: Double
    ): Map<String, Any> = emptyMap()
}

/**
 * Result of a scoring strategy evaluation
 */
data class ScoringResult(
    val provider: PaymentProvider,
    val score: Double,
    val metadata: Map<String, Any> = emptyMap()
) {
    init {
        require(score in 0.0..1.0) { "Score must be between 0.0 and 1.0, got $score" }
    }
}
