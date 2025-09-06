package com.example.payagg.domain.routing

import com.example.payagg.config.ConfigKeys
import com.example.payagg.ports.ConfigPort
import com.example.payagg.ports.PaymentProvider
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

/**
 * Rules-based scoring strategy.
 * 
 * Evaluates providers based on business rules and preferences configured
 * in the routing rules. Providers that match strict rules get the highest
 * scores, preferred providers get high scores, and excluded providers get zero.
 */
@Component
class RulesScoringStrategy(
    private val configPort: ConfigPort
) : ScoringStrategy {

    private val logger = LoggerFactory.getLogger(RulesScoringStrategy::class.java)
    
    override val strategyType = RoutingStrategyType.RULES
    
    override fun calculateScore(
        context: RoutingContext,
        provider: PaymentProvider,
        allProviders: List<PaymentProvider>
    ): Double {
        return try {
            val rules = getRoutingRules()
            val matchingRule = findMatchingRule(context, provider, rules)
            
            val score = when {
                matchingRule == null -> 0.5 // No specific rule = neutral score
                matchingRule.mode == RoutingMode.STRICT && provider.name in matchingRule.providerNames -> 1.0
                matchingRule.mode == RoutingMode.PREFERRED && provider.name in matchingRule.providerNames -> 0.8
                matchingRule.mode == RoutingMode.STRICT && provider.name !in matchingRule.providerNames -> 0.0
                else -> 0.5
            }
            
            logger.debug("Rules score for ${provider.name}: $score (rule: ${matchingRule?.description ?: "none"})")
            score
            
        } catch (e: Exception) {
            logger.warn("Failed to calculate rules score for provider ${provider.name}", e)
            0.5 // Default neutral score on error
        }
    }
    
    override fun getScoreMetadata(
        context: RoutingContext,
        provider: PaymentProvider,
        score: Double
    ): Map<String, Any> {
        return try {
            val rules = getRoutingRules()
            val matchingRule = findMatchingRule(context, provider, rules)
            
            mapOf(
                "matching_rule" to (matchingRule?.description ?: "No matching rule"),
                "rule_mode" to (matchingRule?.mode?.name ?: "NONE"),
                "is_preferred" to (matchingRule?.providerNames?.contains(provider.name) == true),
                "total_rules_evaluated" to rules.size
            )
        } catch (e: Exception) {
            mapOf("error" to "Failed to get rules metadata: ${e.message}")
        }
    }
    
    /**
     * Find the first matching rule for the given context and provider
     */
    private fun findMatchingRule(
        context: RoutingContext,
        provider: PaymentProvider,
        rules: List<RoutingRule>
    ): RoutingRule? {
        return rules.firstOrNull { rule ->
            try {
                rule.condition(context)
            } catch (e: Exception) {
                logger.warn("Error evaluating rule condition: ${rule.description}", e)
                false
            }
        }
    }
    
    /**
     * Get routing rules from configuration
     */
    private fun getRoutingRules(): List<RoutingRule> {
        return try {
            val rulesConfig = configPort.getConfig(ConfigKeys.ROUTING_RULES, List::class.java)
            if (rulesConfig != null) {
                parseRoutingRules(rulesConfig)
            } else {
                logger.warn("No ${ConfigKeys.ROUTING_RULES} configuration found, using empty rules")
                emptyList()
            }
        } catch (e: Exception) {
            logger.error("Failed to load routing rules from configuration", e)
            getDefaultRoutingRules()
        }
    }
    
    /**
     * Parse routing rules from configuration
     */
    private fun parseRoutingRules(rulesConfig: List<*>): List<RoutingRule> {
        return rulesConfig.mapNotNull { ruleData ->
            try {
                val ruleMap = ruleData as Map<String, Any>
                val condition = ruleMap["condition"] as Map<String, Any>
                val action = ruleMap["action"] as Map<String, Any>
                
                val mode = RoutingMode.valueOf((action["mode"] as String).uppercase())
                val providerNames = when (val prefer = action["prefer"]) {
                    is List<*> -> prefer.map { it.toString() }
                    else -> emptyList()
                }
                
                RoutingRule(
                    description = buildDescription(condition, action),
                    condition = buildConditionFunction(condition),
                    providerNames = providerNames,
                    mode = mode
                )
            } catch (e: Exception) {
                logger.error("Failed to parse routing rule: $ruleData", e)
                null
            }
        }
    }
    
    /**
     * Build human-readable description for a rule
     */
    private fun buildDescription(condition: Map<String, Any>, action: Map<String, Any>): String {
        val conditionStr = condition.entries.joinToString(" AND ") { (key, value) ->
            when (key) {
                "country" -> "country=$value"
                "network" -> "network=$value"
                "binRange" -> "binRange=$value"
                else -> "$key=$value"
            }
        }
        val actionStr = "${action["mode"]} ${action["prefer"]}"
        return "If $conditionStr then $actionStr"
    }
    
    /**
     * Build condition function from configuration
     */
    private fun buildConditionFunction(condition: Map<String, Any>): (RoutingContext) -> Boolean {
        return { ctx ->
            condition.all { (key, value) ->
                when (key) {
                    "country" -> ctx.country == value.toString()
                    "network" -> ctx.cardNetwork.name == value.toString()
                    "binRange" -> {
                        val binRange = value.toString()
                        if (binRange.contains("-")) {
                            val (start, end) = binRange.split("-")
                            ctx.binRange?.let { bin ->
                                bin >= start && bin <= end
                            } ?: false
                        } else {
                            ctx.binRange?.startsWith(binRange) == true
                        }
                    }
                    else -> {
                        logger.warn("Unknown condition key: $key")
                        true // Unknown conditions are ignored
                    }
                }
            }
        }
    }
    
    /**
     * Get default routing rules as fallback
     */
    private fun getDefaultRoutingRules(): List<RoutingRule> {
        logger.info("Using default hardcoded routing rules as fallback")
        return listOf(
            RoutingRule(
                description = "AMEX in US prefers Adyen",
                condition = { ctx -> ctx.country == "US" && ctx.cardNetwork.name == "AMEX" },
                providerNames = listOf("AdyenMock"),
                mode = RoutingMode.PREFERRED
            ),
            RoutingRule(
                description = "Domestic BIN range uses LocalBank strictly",
                condition = { ctx -> ctx.binRange?.startsWith("411111") == true },
                providerNames = listOf("LocalBankMock"),
                mode = RoutingMode.STRICT
            )
        )
    }
}



data class RoutingRule(
    val description: String,
    val condition: (RoutingContext) -> Boolean,
    val providerNames: List<String>,
    val mode: RoutingMode
) {
    fun matches(context: RoutingContext): Boolean = condition(context)
}
