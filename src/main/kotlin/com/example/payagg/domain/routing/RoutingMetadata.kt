package com.example.payagg.domain.routing

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import java.math.BigDecimal

/**
 * Base class for routing metadata with JSON subtype support.
 * 
 * This provides type-safe metadata handling with automatic camelCase serialization
 * instead of using random string keys in maps.
 */
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type"
)
@JsonSubTypes(
    JsonSubTypes.Type(value = EligibilityMetadata::class, name = "eligibility"),
    JsonSubTypes.Type(value = HealthCheckMetadata::class, name = "healthCheck"),
    JsonSubTypes.Type(value = RulesMetadata::class, name = "rules"),
    JsonSubTypes.Type(value = CostMetadata::class, name = "cost"),
    JsonSubTypes.Type(value = SuccessRateMetadata::class, name = "successRate"),
    JsonSubTypes.Type(value = LoadBalancingMetadata::class, name = "loadBalancing"),
    JsonSubTypes.Type(value = RoutingProfileMetadata::class, name = "routingProfile"),
    JsonSubTypes.Type(value = CompositeScoresMetadata::class, name = "compositeScores"),
    JsonSubTypes.Type(value = StrategyWeightsMetadata::class, name = "strategyWeights"),
    JsonSubTypes.Type(value = IndividualScoresMetadata::class, name = "individualScores")
)
abstract class RoutingMetadata

/**
 * Eligibility check metadata
 */
data class EligibilityMetadata(
    val totalCandidates: Int,
    val eligibleCount: Int,
    val filteredOut: List<String>,
    val eligibilityReasons: Map<String, String> = emptyMap()
) : RoutingMetadata()

/**
 * Health check metadata
 */
data class HealthCheckMetadata(
    val totalProviders: Int,
    val healthyProviders: Int,
    val unhealthyProviders: List<String>,
    val healthMetrics: Map<String, ProviderHealthMetrics> = emptyMap()
) : RoutingMetadata()

/**
 * Provider health metrics
 */
data class ProviderHealthMetrics(
    val isHealthy: Boolean,
    val successRate: Double,
    val latencyMs: Int,
    val totalTransactions: Int,
    val lastChecked: String
)

/**
 * Rules strategy metadata
 */
data class RulesMetadata(
    val matchingRule: String?,
    val ruleMode: String?,
    val isPreferred: Boolean,
    val totalRulesEvaluated: Int,
    val ruleDetails: Map<String, Any> = emptyMap()
) : RoutingMetadata()

/**
 * Cost strategy metadata
 */
data class CostMetadata(
    val providerFee: BigDecimal,
    val feeCurrency: String,
    val feeType: String,
    val amountProcessed: BigDecimal,
    val currencyProcessed: String,
    val costComparison: CostComparisonMetadata? = null
) : RoutingMetadata()

/**
 * Cost comparison metadata
 */
data class CostComparisonMetadata(
    val minFee: BigDecimal,
    val maxFee: BigDecimal,
    val avgFee: BigDecimal,
    val providerRanking: List<ProviderCostRanking>
)

/**
 * Provider cost ranking
 */
data class ProviderCostRanking(
    val providerName: String,
    val fee: BigDecimal,
    val rank: Int,
    val savingsVsMax: BigDecimal
)

/**
 * Success rate strategy metadata
 */
data class SuccessRateMetadata(
    val successRate: Double,
    val latencyMs: Int,
    val totalTransactions: Int,
    val successfulTransactions: Int,
    val isHealthy: Boolean,
    val lastUpdated: String,
    val performanceRanking: Int? = null
) : RoutingMetadata()

/**
 * Load balancing strategy metadata
 */
data class LoadBalancingMetadata(
    val providerWeight: Int,
    val totalWeight: Int,
    val weightPercentage: String,
    val allWeights: Map<String, Int>,
    val loadDistribution: LoadDistributionMetadata? = null
) : RoutingMetadata()

/**
 * Load distribution metadata
 */
data class LoadDistributionMetadata(
    val isBalanced: Boolean,
    val imbalanceRatio: Double,
    val highestLoadProvider: String,
    val lowestLoadProvider: String
)

/**
 * Routing profile metadata
 */
data class RoutingProfileMetadata(
    val name: String,
    val description: String,
    val isSingleStrategy: Boolean,
    val enabledStrategies: List<String>,
    val profileSource: String = "built-in" // "built-in" or "custom"
) : RoutingMetadata()

/**
 * Composite scores metadata
 */
data class CompositeScoresMetadata(
    val scores: Map<String, Double>,
    val winner: String,
    val winnerScore: Double,
    val scoreRange: ScoreRange
) : RoutingMetadata()

/**
 * Score range information
 */
data class ScoreRange(
    val min: Double,
    val max: Double,
    val avg: Double,
    val spread: Double
)

/**
 * Strategy weights metadata
 */
data class StrategyWeightsMetadata(
    val weights: Map<String, Double>,
    val isNormalized: Boolean,
    val totalWeight: Double,
    val source: String = "profile" // "profile", "config", or "default"
) : RoutingMetadata()

/**
 * Individual scores metadata
 */
data class IndividualScoresMetadata(
    val providerScores: Map<String, Map<String, Double>>,
    val strategyRankings: Map<String, List<String>>,
    val scoreStatistics: Map<String, ScoreStatistics>
) : RoutingMetadata()

/**
 * Score statistics for a strategy
 */
data class ScoreStatistics(
    val min: Double,
    val max: Double,
    val avg: Double,
    val standardDeviation: Double
)

/**
 * Metadata container for routing decisions
 */
data class RoutingMetadataContainer(
    val metadata: List<RoutingMetadata>
) {
    /**
     * Get metadata of a specific type
     */
    inline fun <reified T : RoutingMetadata> getMetadata(): T? {
        return metadata.filterIsInstance<T>().firstOrNull()
    }
    
    /**
     * Get all metadata of a specific type
     */
    inline fun <reified T : RoutingMetadata> getAllMetadata(): List<T> {
        return metadata.filterIsInstance<T>()
    }

}
