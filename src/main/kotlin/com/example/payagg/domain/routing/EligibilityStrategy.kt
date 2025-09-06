package com.example.payagg.domain.routing

import com.example.payagg.ports.PaymentProvider
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class EligibilityStrategy : RoutingStrategy {
    
    private val logger = LoggerFactory.getLogger(EligibilityStrategy::class.java)
    override val name = "ELIGIBILITY"
    
    override fun route(context: RoutingContext, candidates: List<PaymentProvider>): RoutingResult {
        val eligibleProviders = candidates.filter { provider ->
            val isSupported = provider.supports(context.cardNetwork, context.currency, context.country)
            
            if (!isSupported) {
                logger.debug("Provider ${provider.name} does not support ${context.cardNetwork}, ${context.currency}, ${context.country}")
            }
            
            isSupported
        }
        
        logger.info("Eligibility filter: ${eligibleProviders.size}/${candidates.size} providers eligible")
        
        return RoutingResult(
            providers = eligibleProviders,
            strategy = name,
            reason = "Filtered providers based on network, currency, and country support",
            metadata = mapOf(
                "total_candidates" to candidates.size,
                "eligible_count" to eligibleProviders.size,
                "filtered_out" to (candidates - eligibleProviders.toSet()).map { it.name }
            )
        )
    }
}
