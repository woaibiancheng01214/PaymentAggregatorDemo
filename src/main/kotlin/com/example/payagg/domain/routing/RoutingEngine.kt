package com.example.payagg.domain.routing

import com.example.payagg.adapters.providers.PaymentProviderRegistry
import com.example.payagg.domain.RouteDecision
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class RoutingEngine(
    private val providerRegistry: PaymentProviderRegistry,
    private val eligibilityStrategy: EligibilityStrategy,
    private val rulesStrategy: RulesStrategy,
    private val costStrategy: CostStrategy,
    private val weightStrategy: WeightStrategy,
    private val healthStrategy: HealthStrategy
) {
    
    private val logger = LoggerFactory.getLogger(RoutingEngine::class.java)
    
    fun route(context: RoutingContext): RouteDecision {
        logger.info("Starting routing for ${context.amount} ${context.currency} in ${context.country} with ${context.cardNetwork}")
        
        // Start with all available providers
        var candidates = providerRegistry.getAllProviders()
        val strategiesUsed = mutableListOf<String>()
        val routingMetadata = mutableMapOf<String, Any>()
        
        // Apply strategies in order
        
        // 1. Eligibility filter (mandatory)
        val eligibilityResult = eligibilityStrategy.route(context, candidates)
        candidates = eligibilityResult.providers
        strategiesUsed.add(eligibilityResult.strategy)
        routingMetadata["eligibility"] = eligibilityResult.metadata
        
        if (candidates.isEmpty()) {
            logger.warn("No eligible providers found after eligibility filter")
            return RouteDecision(
                candidates = emptyList(),
                strategyUsed = strategiesUsed,
                selectedProvider = null,
                reason = "No eligible providers found"
            )
        }
        
        // 2. Rules strategy
        val rulesResult = rulesStrategy.route(context, candidates)
        candidates = rulesResult.providers
        strategiesUsed.add(rulesResult.strategy)
        routingMetadata["rules"] = rulesResult.metadata
        
        if (candidates.isEmpty()) {
            logger.warn("No providers available after rules filter")
            return RouteDecision(
                candidates = emptyList(),
                strategyUsed = strategiesUsed,
                selectedProvider = null,
                reason = "No providers available after rules filter"
            )
        }
        
        // 3. Health filter
        val healthResult = healthStrategy.route(context, candidates)
        candidates = healthResult.providers
        strategiesUsed.add(healthResult.strategy)
        routingMetadata["health"] = healthResult.metadata
        
        if (candidates.isEmpty()) {
            logger.warn("No healthy providers available")
            return RouteDecision(
                candidates = emptyList(),
                strategyUsed = strategiesUsed,
                selectedProvider = null,
                reason = "No healthy providers available"
            )
        }
        
        // 4. Cost strategy (if enabled)
        if (isCostStrategyEnabled()) {
            val costResult = costStrategy.route(context, candidates)
            candidates = costResult.providers
            strategiesUsed.add(costResult.strategy)
            routingMetadata["cost"] = costResult.metadata
        }
        
        // 5. Weight strategy (if enabled)
        if (isWeightStrategyEnabled()) {
            val weightResult = weightStrategy.route(context, candidates)
            candidates = weightResult.providers
            strategiesUsed.add(weightResult.strategy)
            routingMetadata["weight"] = weightResult.metadata
        }
        
        val selectedProvider = candidates.firstOrNull()
        val candidateNames = candidates.map { it.name }
        
        logger.info("Routing completed. Selected provider: ${selectedProvider?.name}, Candidates: $candidateNames")
        
        return RouteDecision(
            candidates = candidateNames,
            strategyUsed = strategiesUsed,
            selectedProvider = selectedProvider?.name,
            reason = "Routing completed successfully using strategies: ${strategiesUsed.joinToString(", ")}"
        )
    }
    
    private fun isCostStrategyEnabled(): Boolean {
        // In a real implementation, this would come from configuration
        return true
    }
    
    private fun isWeightStrategyEnabled(): Boolean {
        // In a real implementation, this would come from configuration
        return true
    }
}
