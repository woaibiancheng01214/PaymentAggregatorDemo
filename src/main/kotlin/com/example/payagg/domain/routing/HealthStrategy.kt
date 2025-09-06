package com.example.payagg.domain.routing

import com.example.payagg.ports.PaymentProvider
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class HealthStrategy : RoutingStrategy {
    
    private val logger = LoggerFactory.getLogger(HealthStrategy::class.java)
    override val name = "HEALTH"
    
    override fun route(context: RoutingContext, candidates: List<PaymentProvider>): RoutingResult {
        if (candidates.isEmpty()) {
            return RoutingResult(
                providers = emptyList(),
                strategy = name,
                reason = "No candidates provided"
            )
        }
        
        // Check health of each provider
        val healthyProviders = mutableListOf<PaymentProvider>()
        val unhealthyProviders = mutableListOf<String>()
        
        candidates.forEach { provider ->
            try {
                val health = provider.health()
                if (health.healthy && health.successRate > 0.8) { // Minimum 80% success rate
                    healthyProviders.add(provider)
                } else {
                    unhealthyProviders.add("${provider.name} (healthy=${health.healthy}, successRate=${health.successRate})")
                    logger.warn("Provider ${provider.name} filtered out due to poor health: $health")
                }
            } catch (e: Exception) {
                unhealthyProviders.add("${provider.name} (health check failed)")
                logger.error("Health check failed for provider ${provider.name}", e)
            }
        }
        
        // Sort healthy providers by success rate and latency
        val sortedHealthyProviders = healthyProviders.sortedWith(
            compareByDescending<PaymentProvider> { 
                try { it.health().successRate } catch (e: Exception) { 0.0 }
            }.thenBy { 
                try { it.health().latency } catch (e: Exception) { Long.MAX_VALUE }
            }
        )
        
        logger.info("Health filter: ${sortedHealthyProviders.size}/${candidates.size} providers healthy")
        if (unhealthyProviders.isNotEmpty()) {
            logger.info("Unhealthy providers filtered out: $unhealthyProviders")
        }
        
        return RoutingResult(
            providers = sortedHealthyProviders,
            strategy = name,
            reason = "Filtered and sorted providers by health metrics",
            metadata = mapOf(
                "healthy_count" to sortedHealthyProviders.size,
                "unhealthy_count" to unhealthyProviders.size,
                "unhealthy_providers" to unhealthyProviders,
                "health_metrics" to sortedHealthyProviders.associate { provider ->
                    provider.name to try {
                        val health = provider.health()
                        mapOf(
                            "healthy" to health.healthy,
                            "success_rate" to health.successRate,
                            "latency" to health.latency
                        )
                    } catch (e: Exception) {
                        mapOf("error" to e.message)
                    }
                }
            )
        )
    }
}
