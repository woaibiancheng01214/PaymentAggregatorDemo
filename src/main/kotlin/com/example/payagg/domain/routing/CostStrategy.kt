package com.example.payagg.domain.routing

import com.example.payagg.ports.PaymentProvider
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class CostStrategy : RoutingStrategy {
    
    private val logger = LoggerFactory.getLogger(CostStrategy::class.java)
    override val name = "COST"
    
    override fun route(context: RoutingContext, candidates: List<PaymentProvider>): RoutingResult {
        if (candidates.isEmpty()) {
            return RoutingResult(
                providers = emptyList(),
                strategy = name,
                reason = "No candidates provided"
            )
        }
        
        // Calculate fees for each provider
        val providerFees = candidates.map { provider ->
            try {
                val fee = provider.feeFor(context.currency, context.amount)
                provider to fee.amount
            } catch (e: Exception) {
                logger.warn("Failed to calculate fee for provider ${provider.name}", e)
                provider to Long.MAX_VALUE // Treat as most expensive if calculation fails
            }
        }
        
        // Sort by fee (lowest first)
        val sortedByFee = providerFees.sortedBy { it.second }
        val sortedProviders = sortedByFee.map { it.first }
        
        logger.info("Cost-based routing: ${sortedProviders.map { it.name }} (fees: ${sortedByFee.map { "${it.first.name}:${it.second}" }})")
        
        return RoutingResult(
            providers = sortedProviders,
            strategy = name,
            reason = "Sorted providers by lowest fees",
            metadata = mapOf<String, Any>(
                "provider_fees" to sortedByFee.associate { it.first.name to it.second },
                "lowest_fee_provider" to (sortedProviders.firstOrNull()?.name ?: ""),
                "lowest_fee_amount" to (sortedByFee.firstOrNull()?.second ?: 0L)
            )
        )
    }
}
