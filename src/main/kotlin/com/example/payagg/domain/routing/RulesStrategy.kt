package com.example.payagg.domain.routing

import com.example.payagg.adapters.providers.PaymentProviderRegistry
import com.example.payagg.ports.PaymentProvider
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class RulesStrategy(
    private val providerRegistry: PaymentProviderRegistry
) : RoutingStrategy {
    
    private val logger = LoggerFactory.getLogger(RulesStrategy::class.java)
    override val name = "RULES"
    
    override fun route(context: RoutingContext, candidates: List<PaymentProvider>): RoutingResult {
        // Get routing rules from configuration (simplified for demo)
        val rules = getRoutingRules()
        
        for (rule in rules) {
            if (rule.matches(context)) {
                logger.info("Routing rule matched: ${rule.description}")
                
                val ruleProviders = when (rule.mode) {
                    RoutingMode.STRICT -> {
                        // Only use specified providers
                        providerRegistry.getProvidersByNames(rule.providerNames)
                            .filter { it in candidates }
                    }
                    RoutingMode.PREFERRED -> {
                        // Prefer specified providers, but include others as fallback
                        val preferred = providerRegistry.getProvidersByNames(rule.providerNames)
                            .filter { it in candidates }
                        val others = candidates.filter { it !in preferred }
                        preferred + others
                    }
                    RoutingMode.EXCLUDED -> {
                        // Exclude specified providers
                        val excluded = providerRegistry.getProvidersByNames(rule.providerNames).toSet()
                        candidates.filter { it !in excluded }
                    }
                }
                
                return RoutingResult(
                    providers = ruleProviders,
                    strategy = name,
                    reason = "Applied routing rule: ${rule.description}",
                    metadata = mapOf(
                        "rule_applied" to rule.description,
                        "rule_mode" to rule.mode.name,
                        "rule_providers" to rule.providerNames
                    )
                )
            }
        }
        
        // No rules matched, return candidates unchanged
        return RoutingResult(
            providers = candidates,
            strategy = name,
            reason = "No routing rules matched",
            metadata = mapOf("rules_evaluated" to rules.size)
        )
    }
    
    private fun getRoutingRules(): List<RoutingRule> {
        // In a real implementation, this would come from configuration
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
