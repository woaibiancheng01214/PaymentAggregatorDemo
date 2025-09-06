package com.example.payagg.domain.routing

import com.example.payagg.adapters.providers.PaymentProviderRegistry
import com.example.payagg.config.ConfigKeys
import com.example.payagg.domain.RouteDecision
import com.example.payagg.domain.CardNetwork
import com.example.payagg.ports.ConfigPort
import com.example.payagg.ports.PaymentProvider
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.util.*

/**
 * Provider evaluation result containing all scoring criteria
 */
data class ProviderEvaluation(
    val provider: PaymentProvider,
    val isEligible: Boolean = true,
    val isHealthy: Boolean = true,
    val scores: Map<RoutingStrategyType, Double> = emptyMap(),
    val compositeScore: Double = 0.0,
    val metadata: Map<String, Any> = emptyMap()
) {
    /**
     * Calculate composite score based on strategy weights
     */
    fun calculateCompositeScore(weights: Map<RoutingStrategyType, Double>): ProviderEvaluation {
        if (!isEligible || !isHealthy) {
            return copy(compositeScore = 0.0)
        }

        val composite = weights.entries.sumOf { (strategy, weight) ->
            val score = scores[strategy] ?: 0.5 // Default neutral score
            score * weight
        }

        return copy(compositeScore = composite)
    }

    /**
     * Get score for a specific strategy
     */
    fun getScore(strategy: RoutingStrategyType): Double = scores[strategy] ?: 0.0
}

@Service
class RoutingEngine(
    private val providerRegistry: PaymentProviderRegistry,
    private val eligibilityStrategy: EligibilityStrategy,
    private val rulesScoringStrategy: RulesScoringStrategy,
    private val costScoringStrategy: CostScoringStrategy,
    private val successRateStrategy: SuccessRateStrategy,
    private val loadBalancingStrategy: LoadBalancingScoringStrategy,
    private val configPort: ConfigPort
) {
    
    private val logger = LoggerFactory.getLogger(RoutingEngine::class.java)
    
    fun route(context: RoutingContext): RouteDecision {
        logger.info("Starting routing for ${context.amount} ${context.currency} in ${context.country} with ${context.cardNetwork}")

        // Get all providers
        val allProviders = providerRegistry.getAllProviders()
        val strategiesUsed = mutableListOf<String>()
        val routingMetadata = mutableListOf<RoutingMetadata>()

        // Step 1: Mandatory eligibility check for all providers
        val eligibleProviders = performEligibilityCheck(context, allProviders, strategiesUsed, routingMetadata)

        if (eligibleProviders.isEmpty()) {
            logger.warn("No eligible providers found")
            return RouteDecision(
                candidates = emptyList(),
                strategyUsed = strategiesUsed,
                selectedProvider = null,
                reason = "No eligible providers found"
            )
        }

        // Step 2: Health check (success rate based)
        val healthyProviders = performHealthCheck(eligibleProviders, strategiesUsed, routingMetadata)

        if (healthyProviders.isEmpty()) {
            logger.warn("No healthy providers found")
            return RouteDecision(
                candidates = emptyList(),
                strategyUsed = strategiesUsed,
                selectedProvider = null,
                reason = "No healthy providers found"
            )
        }

        // Step 3: Get active routing profile and enabled strategies
        val routingProfile = getActiveRoutingProfile()
        val enabledStrategies = getEnabledScoringStrategies(routingProfile)

        if (enabledStrategies.isEmpty()) {
            logger.warn("No enabled strategies found, using default balanced profile")
            val balancedProfile = RoutingProfile.getBuiltInProfiles()["balanced"]!!
            val balancedStrategies = getEnabledScoringStrategies(balancedProfile)
            val evaluations = evaluateProvidersWithScoring(context, healthyProviders, balancedStrategies, strategiesUsed, routingMetadata)
            val weights = balancedProfile.getNormalizedWeights()
            val rankedProviders = evaluations
                .map { it.calculateCompositeScore(weights) }
                .sortedByDescending { it.compositeScore }
            return routeWithProfile(context, rankedProviders, balancedProfile, strategiesUsed, routingMetadata)
        }

        // Step 4: Score-based strategy evaluation (only enabled strategies)
        val evaluations = evaluateProvidersWithScoring(context, healthyProviders, enabledStrategies, strategiesUsed, routingMetadata)

        // Step 5: Calculate composite scores and rank providers
        val weights = routingProfile.getNormalizedWeights()
        val rankedProviders = evaluations
            .map { it.calculateCompositeScore(weights) }
            .sortedByDescending { it.compositeScore }

        return routeWithProfile(context, rankedProviders, routingProfile, strategiesUsed, routingMetadata)
    }

    /**
     * Complete routing with the selected profile and ranked providers
     */
    private fun routeWithProfile(
        context: RoutingContext,
        rankedProviders: List<ProviderEvaluation>,
        routingProfile: RoutingProfile,
        strategiesUsed: MutableList<String>,
        routingMetadata: MutableList<RoutingMetadata>
    ): RouteDecision {
        // Select the best provider
        val selectedEvaluation = rankedProviders.first()
        val candidateNames = rankedProviders.map { it.provider.name }

        val profileInfo = if (routingProfile.isSingleStrategy()) {
            "single strategy (${routingProfile.getSingleStrategy()?.displayName})"
        } else {
            "profile '${routingProfile.name}' with ${routingProfile.strategies.size} strategies"
        }

        logger.info("Routing completed using $profileInfo. Selected: ${selectedEvaluation.provider.name} (score: ${selectedEvaluation.compositeScore})")
        logger.debug("Provider rankings: ${rankedProviders.map { "${it.provider.name}=${it.compositeScore}" }}")

        return RouteDecision(
            candidates = candidateNames,
            strategyUsed = strategiesUsed,
            selectedProvider = selectedEvaluation.provider.name,
            reason = "Selected best provider using $profileInfo (score: ${selectedEvaluation.compositeScore})",
            metadata = RoutingMetadataContainer(buildRouteMetadata(rankedProviders, routingProfile, routingMetadata))
        )
    }
    
    /**
     * Perform mandatory eligibility check
     */
    private fun performEligibilityCheck(
        context: RoutingContext,
        providers: List<PaymentProvider>,
        strategiesUsed: MutableList<String>,
        routingMetadata: MutableList<RoutingMetadata>
    ): List<PaymentProvider> {
        val eligibilityResult = eligibilityStrategy.route(context, providers)
        strategiesUsed.add(RoutingStrategyType.RULES.displayName) // Eligibility is part of rules

        val filteredOut = providers.map { it.name } - eligibilityResult.providers.map { it.name }.toSet()
        routingMetadata.add(EligibilityMetadata(
            totalCandidates = providers.size,
            eligibleCount = eligibilityResult.providers.size,
            filteredOut = filteredOut,
            eligibilityReasons = eligibilityResult.metadata.mapValues { it.value.toString() }
        ))

        logger.info("Eligibility check: ${eligibilityResult.providers.size}/${providers.size} providers eligible")
        return eligibilityResult.providers
    }

    /**
     * Perform health check using success rate strategy
     */
    private fun performHealthCheck(
        providers: List<PaymentProvider>,
        strategiesUsed: MutableList<String>,
        routingMetadata: MutableList<RoutingMetadata>
    ): List<PaymentProvider> {
        val healthyProviders = providers.filter { provider ->
            successRateStrategy.isProviderHealthy(provider)
        }

        strategiesUsed.add(RoutingStrategyType.SUCCESS_RATE.displayName)

        val unhealthyProviders = providers.map { it.name } - healthyProviders.map { it.name }.toSet()
        val healthMetrics = providers.associate { provider ->
            provider.name to try {
                val metadata = successRateStrategy.getScoreMetadata(
                    RoutingContext(
                        amount = BigDecimal.ZERO,
                        currency = Currency.getInstance("USD"),
                        country = "US",
                        cardNetwork = CardNetwork.VISA,
                        merchantId = "health-check"
                    ),
                    provider,
                    0.0
                )
                ProviderHealthMetrics(
                    isHealthy = successRateStrategy.isProviderHealthy(provider),
                    successRate = metadata["success_rate"] as? Double ?: 0.0,
                    latencyMs = metadata["latency_ms"] as? Int ?: 0,
                    totalTransactions = metadata["total_transactions"] as? Int ?: 0,
                    lastChecked = metadata["last_updated"] as? String ?: ""
                )
            } catch (e: Exception) {
                ProviderHealthMetrics(false, 0.0, 0, 0, "")
            }
        }

        routingMetadata.add(HealthCheckMetadata(
            totalProviders = providers.size,
            healthyProviders = healthyProviders.size,
            unhealthyProviders = unhealthyProviders.toList(),
            healthMetrics = healthMetrics
        ))

        logger.info("Health check: ${healthyProviders.size}/${providers.size} providers healthy")
        return healthyProviders
    }

    /**
     * Get the active routing profile from configuration
     */
    private fun getActiveRoutingProfile(): RoutingProfile {
        return try {
            val profileConfig = configPort.getConfig(ConfigKeys.ROUTING_PROFILE, Map::class.java)
            val profileName = if (profileConfig != null) {
                @Suppress("UNCHECKED_CAST")
                val configMap = profileConfig as Map<String, Any>
                configMap["profile"]?.toString()
            } else {
                null
            }

            if (profileName != null) {
                val customProfiles = getCustomProfiles()
                RoutingProfile.getProfile(profileName, customProfiles)
                    ?: run {
                        logger.warn("Unknown routing profile '$profileName', using balanced profile")
                        RoutingProfile.getBuiltInProfiles()["balanced"]!!
                    }
            } else {
                logger.info("No routing profile configured, using balanced profile")
                RoutingProfile.getBuiltInProfiles()["balanced"]!!
            }
        } catch (e: Exception) {
            logger.error("Failed to load routing profile, using balanced profile", e)
            RoutingProfile.getBuiltInProfiles()["balanced"]!!
        }
    }

    /**
     * Get custom profiles from configuration
     */
    private fun getCustomProfiles(): Map<String, RoutingProfile> {
        return try {
            val profilesConfig = configPort.getConfig(ConfigKeys.ROUTING_PROFILES, Map::class.java)
            if (profilesConfig != null) {
                parseCustomProfiles(profilesConfig)
            } else {
                emptyMap()
            }
        } catch (e: Exception) {
            logger.warn("Failed to load custom routing profiles", e)
            emptyMap()
        }
    }

    /**
     * Parse custom profiles from configuration
     */
    private fun parseCustomProfiles(profilesConfig: Map<*, *>): Map<String, RoutingProfile> {
        return profilesConfig.mapNotNull { (name, config) ->
            try {
                @Suppress("UNCHECKED_CAST")
                val profileMap = config as Map<String, Any>
                val strategiesConfig = profileMap["strategies"] as? List<*> ?: emptyList<String>()
                val weightsConfig = profileMap["weights"] as? Map<*, *> ?: emptyMap<String, Any>()

                val strategies = strategiesConfig.mapNotNull { strategyName ->
                    try {
                        RoutingStrategyType.valueOf(strategyName.toString().uppercase())
                    } catch (e: Exception) {
                        logger.warn("Unknown strategy type: $strategyName")
                        null
                    }
                }.toSet()

                val weights = weightsConfig.mapNotNull { (strategyName, weight) ->
                    try {
                        val strategyType = RoutingStrategyType.valueOf(strategyName.toString().uppercase())
                        val weightValue = (weight as? Number)?.toDouble() ?: 0.0
                        strategyType to weightValue
                    } catch (e: Exception) {
                        logger.warn("Invalid weight configuration for strategy $strategyName: $weight")
                        null
                    }
                }.toMap()

                if (strategies.isNotEmpty() && weights.isNotEmpty()) {
                    name.toString() to RoutingProfile(
                        name = name.toString(),
                        description = "Custom profile: ${name}",
                        strategies = strategies,
                        weights = weights
                    )
                } else {
                    logger.warn("Invalid profile configuration for $name")
                    null
                }
            } catch (e: Exception) {
                logger.error("Failed to parse custom profile $name", e)
                null
            }
        }.toMap()
    }

    /**
     * Get enabled scoring strategies based on the routing profile
     */
    private fun getEnabledScoringStrategies(profile: RoutingProfile): List<ScoringStrategy> {
        val allStrategies = mapOf(
            RoutingStrategyType.RULES to rulesScoringStrategy,
            RoutingStrategyType.COST to costScoringStrategy,
            RoutingStrategyType.SUCCESS_RATE to successRateStrategy,
            RoutingStrategyType.LOAD_BALANCING to loadBalancingStrategy
        )

        return profile.strategies.mapNotNull { strategyType ->
            allStrategies[strategyType]
        }
    }

    /**
     * Evaluate providers using enabled scoring strategies
     */
    private fun evaluateProvidersWithScoring(
        context: RoutingContext,
        providers: List<PaymentProvider>,
        enabledStrategies: List<ScoringStrategy>,
        strategiesUsed: MutableList<String>,
        routingMetadata: MutableList<RoutingMetadata>
    ): List<ProviderEvaluation> {

        return providers.map { provider ->
            val scores = mutableMapOf<RoutingStrategyType, Double>()
            val metadata = mutableMapOf<String, Any>()

            // Calculate scores for each enabled strategy
            enabledStrategies.forEach { strategy ->
                try {
                    val score = strategy.calculateScore(context, provider, providers)
                    scores[strategy.strategyType] = score
                    metadata["${strategy.strategyType.name.lowercase()}_metadata"] =
                        strategy.getScoreMetadata(context, provider, score)

                    if (strategy.strategyType.displayName !in strategiesUsed) {
                        strategiesUsed.add(strategy.strategyType.displayName)
                    }
                } catch (e: Exception) {
                    logger.warn("Failed to calculate ${strategy.strategyType} score for ${provider.name}", e)
                    scores[strategy.strategyType] = 0.5 // Default neutral score
                }
            }

            ProviderEvaluation(
                provider = provider,
                isEligible = true, // Already filtered
                isHealthy = true,  // Already filtered
                scores = scores,
                metadata = metadata
            )
        }
    }



    /**
     * Build comprehensive route metadata
     */
    private fun buildRouteMetadata(
        rankedProviders: List<ProviderEvaluation>,
        routingProfile: RoutingProfile,
        routingMetadata: List<RoutingMetadata>
    ): List<RoutingMetadata> {
        val metadata = routingMetadata.toMutableList()

        // Add profile information
        metadata.add(RoutingProfileMetadata(
            name = routingProfile.name,
            description = routingProfile.description,
            isSingleStrategy = routingProfile.isSingleStrategy(),
            enabledStrategies = routingProfile.strategies.map { it.name.lowercase() },
            profileSource = if (routingProfile.name in RoutingProfile.getBuiltInProfiles()) "built-in" else "custom"
        ))

        // Add composite scores
        val scores = rankedProviders.associate { it.provider.name to it.compositeScore }
        val winner = rankedProviders.firstOrNull()
        if (winner != null && scores.isNotEmpty()) {
            val scoreValues = scores.values
            metadata.add(CompositeScoresMetadata(
                scores = scores,
                winner = winner.provider.name,
                winnerScore = winner.compositeScore,
                scoreRange = ScoreRange(
                    min = scoreValues.minOrNull() ?: 0.0,
                    max = scoreValues.maxOrNull() ?: 0.0,
                    avg = scoreValues.average(),
                    spread = (scoreValues.maxOrNull() ?: 0.0) - (scoreValues.minOrNull() ?: 0.0)
                )
            ))
        }

        // Add strategy weights
        metadata.add(StrategyWeightsMetadata(
            weights = routingProfile.weights.mapKeys { it.key.name.lowercase() },
            isNormalized = true,
            totalWeight = routingProfile.weights.values.sum(),
            source = "profile"
        ))

        // Add individual scores
        val providerScores = rankedProviders.associate { evaluation ->
            evaluation.provider.name to evaluation.scores.mapKeys { it.key.name.lowercase() }
        }

        val strategyRankings = routingProfile.strategies.associate { strategy ->
            val strategyName = strategy.name.lowercase()
            val ranking = rankedProviders.sortedByDescending {
                it.scores[strategy] ?: 0.0
            }.map { it.provider.name }
            strategyName to ranking
        }

        val scoreStatistics = routingProfile.strategies.associate { strategy ->
            val strategyName = strategy.name.lowercase()
            val scores = rankedProviders.mapNotNull { it.scores[strategy] }
            if (scores.isNotEmpty()) {
                val avg = scores.average()
                val variance = scores.map { (it - avg) * (it - avg) }.average()
                strategyName to ScoreStatistics(
                    min = scores.minOrNull() ?: 0.0,
                    max = scores.maxOrNull() ?: 0.0,
                    avg = avg,
                    standardDeviation = kotlin.math.sqrt(variance)
                )
            } else {
                strategyName to ScoreStatistics(0.0, 0.0, 0.0, 0.0)
            }
        }

        metadata.add(IndividualScoresMetadata(
            providerScores = providerScores,
            strategyRankings = strategyRankings,
            scoreStatistics = scoreStatistics
        ))

        return metadata
    }
}
