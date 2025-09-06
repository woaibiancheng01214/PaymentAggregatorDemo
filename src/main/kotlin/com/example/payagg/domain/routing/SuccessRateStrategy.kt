package com.example.payagg.domain.routing

import com.example.payagg.config.ConfigKeys
import com.example.payagg.ports.ConfigPort
import com.example.payagg.ports.PaymentProvider
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import kotlin.random.Random

/**
 * Success rate-based scoring strategy.
 * 
 * Evaluates providers based on their historical success rates, latency,
 * and overall reliability metrics. Providers with higher success rates
 * and lower latency receive higher scores.
 */
@Component
class SuccessRateStrategy(
    private val configPort: ConfigPort
) : ScoringStrategy {
    
    private val logger = LoggerFactory.getLogger(SuccessRateStrategy::class.java)
    
    override val strategyType = RoutingStrategyType.SUCCESS_RATE
    
    override fun calculateScore(
        context: RoutingContext,
        provider: PaymentProvider,
        allProviders: List<PaymentProvider>
    ): Double {
        return try {
            val metrics = getProviderMetrics(provider)
            val successRate = metrics.successRate
            val latency = metrics.latencyMs
            
            // Calculate score based on success rate (70%) and latency (30%)
            val successRateScore = successRate
            val latencyScore = calculateLatencyScore(latency, allProviders)
            
            val finalScore = (successRateScore * 0.7) + (latencyScore * 0.3)
            
            logger.debug("Success rate score for ${provider.name}: $finalScore (success: $successRate, latency: ${latency}ms)")
            finalScore.coerceIn(0.0, 1.0)
            
        } catch (e: Exception) {
            logger.warn("Failed to calculate success rate score for provider ${provider.name}", e)
            0.5 // Default neutral score on error
        }
    }
    
    override fun getScoreMetadata(
        context: RoutingContext,
        provider: PaymentProvider,
        score: Double
    ): Map<String, Any> {
        return try {
            val metrics = getProviderMetrics(provider)
            mapOf(
                "success_rate" to metrics.successRate,
                "latency_ms" to metrics.latencyMs,
                "total_transactions" to metrics.totalTransactions,
                "successful_transactions" to metrics.successfulTransactions,
                "is_healthy" to metrics.isHealthy,
                "last_updated" to metrics.lastUpdated
            )
        } catch (e: Exception) {
            mapOf("error" to "Failed to get metrics: ${e.message}")
        }
    }
    
    /**
     * Get provider metrics (success rate, latency, etc.)
     */
    private fun getProviderMetrics(provider: PaymentProvider): ProviderMetrics {
        // In a real implementation, this would come from monitoring/metrics system
        // For demo purposes, we'll simulate realistic metrics
        return when (provider.name) {
            "StripeMock" -> ProviderMetrics(
                successRate = 0.98,
                latencyMs = 150,
                totalTransactions = 10000,
                successfulTransactions = 9800
            )
            "AdyenMock" -> ProviderMetrics(
                successRate = 0.97,
                latencyMs = 200,
                totalTransactions = 8000,
                successfulTransactions = 7760
            )
            "LocalBankMock" -> ProviderMetrics(
                successRate = 0.95,
                latencyMs = 100,
                totalTransactions = 5000,
                successfulTransactions = 4750
            )
            else -> {
                // Generate realistic random metrics for unknown providers
                val successRate = 0.90 + (Random.nextDouble() * 0.08) // 90-98%
                val latency = 100 + Random.nextInt(200) // 100-300ms
                val total = 1000 + Random.nextInt(9000) // 1k-10k transactions
                ProviderMetrics(
                    successRate = successRate,
                    latencyMs = latency,
                    totalTransactions = total,
                    successfulTransactions = (total * successRate).toInt()
                )
            }
        }
    }
    
    /**
     * Calculate latency score relative to all providers
     */
    private fun calculateLatencyScore(latency: Int, allProviders: List<PaymentProvider>): Double {
        val allLatencies = allProviders.map { getProviderMetrics(it).latencyMs }
        val minLatency = allLatencies.minOrNull() ?: latency
        val maxLatency = allLatencies.maxOrNull() ?: latency
        
        return if (maxLatency == minLatency) {
            1.0 // All latencies are the same
        } else {
            // Invert: lower latency = higher score
            1.0 - ((latency - minLatency).toDouble() / (maxLatency - minLatency).toDouble())
        }
    }
    
    /**
     * Check if provider is healthy based on success rate and latency thresholds
     */
    fun isProviderHealthy(provider: PaymentProvider): Boolean {
        return try {
            val metrics = getProviderMetrics(provider)
            val config = getHealthConfig()
            
            metrics.successRate >= config.minSuccessRate &&
            metrics.latencyMs <= config.maxLatencyMs &&
            metrics.totalTransactions >= config.minTransactions
            
        } catch (e: Exception) {
            logger.warn("Failed to check health for provider ${provider.name}", e)
            false // Assume unhealthy on error
        }
    }
    
    /**
     * Get health check configuration
     */
    private fun getHealthConfig(): HealthConfig {
        return try {
            val config = configPort.getConfig(ConfigKeys.HEALTH_CHECK_CONFIG, Map::class.java)
            if (config != null) {
                @Suppress("UNCHECKED_CAST")
                val configMap = config as Map<String, Any>
                HealthConfig(
                    minSuccessRate = (configMap["min_success_rate"] as? Number)?.toDouble() ?: 0.90,
                    maxLatencyMs = (configMap["max_latency_ms"] as? Number)?.toInt() ?: 5000,
                    minTransactions = (configMap["min_transactions"] as? Number)?.toInt() ?: 100
                )
            } else {
                HealthConfig() // Use defaults
            }
        } catch (e: Exception) {
            logger.warn("Failed to load health check configuration, using defaults", e)
            HealthConfig()
        }
    }
}

/**
 * Provider metrics data
 */
data class ProviderMetrics(
    val successRate: Double,
    val latencyMs: Int,
    val totalTransactions: Int,
    val successfulTransactions: Int,
    val lastUpdated: String = java.time.Instant.now().toString()
) {
    val isHealthy: Boolean
        get() = successRate >= 0.90 && latencyMs <= 5000 && totalTransactions >= 100
}

/**
 * Health check configuration
 */
data class HealthConfig(
    val minSuccessRate: Double = 0.90,      // 90% minimum success rate
    val maxLatencyMs: Int = 5000,           // 5 second maximum latency
    val minTransactions: Int = 100          // Minimum transactions for reliability
)
