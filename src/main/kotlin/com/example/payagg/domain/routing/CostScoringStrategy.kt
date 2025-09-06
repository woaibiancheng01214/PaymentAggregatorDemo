package com.example.payagg.domain.routing

import com.example.payagg.ports.PaymentProvider
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.math.BigDecimal

/**
 * Cost-based scoring strategy.
 * 
 * Evaluates providers based on their transaction fees and costs.
 * Providers with lower fees receive higher scores. Scores are normalized
 * across all available providers to ensure fair comparison.
 */
@Component
class CostScoringStrategy : ScoringStrategy {
    
    private val logger = LoggerFactory.getLogger(CostScoringStrategy::class.java)
    
    override val strategyType = RoutingStrategyType.COST
    
    override fun calculateScore(
        context: RoutingContext,
        provider: PaymentProvider,
        allProviders: List<PaymentProvider>
    ): Double {
        return try {
            val fee = provider.feeFor(context.currency, context.amount)
            val allFees = calculateAllFees(context, allProviders)
            
            if (allFees.isEmpty()) {
                logger.warn("No fees calculated for any provider")
                return 0.5 // Default neutral score
            }
            
            val score = calculateNormalizedCostScore(fee.amount, allFees)
            
            logger.debug("Cost score for ${provider.name}: $score (fee: ${fee.amount})")
            score
            
        } catch (e: Exception) {
            logger.warn("Failed to calculate cost score for provider ${provider.name}", e)
            0.0 // Assume most expensive on error
        }
    }
    
    override fun getScoreMetadata(
        context: RoutingContext,
        provider: PaymentProvider,
        score: Double
    ): Map<String, Any> {
        return try {
            val fee = provider.feeFor(context.currency, context.amount)
            val allFees = calculateAllFees(context, allProviders = listOf(provider))
            
            mapOf(
                "provider_fee" to fee.amount,
                "fee_currency" to fee.currency.currencyCode,
                "fee_type" to fee.type.name,
                "amount_processed" to context.amount,
                "currency_processed" to context.currency.currencyCode
            )
        } catch (e: Exception) {
            mapOf("error" to "Failed to get cost metadata: ${e.message}")
        }
    }
    
    /**
     * Calculate fees for all providers
     */
    private fun calculateAllFees(context: RoutingContext, allProviders: List<PaymentProvider>): List<BigDecimal> {
        return allProviders.mapNotNull { provider ->
            try {
                provider.feeFor(context.currency, context.amount).amount
            } catch (e: Exception) {
                logger.warn("Failed to calculate fee for provider ${provider.name}", e)
                null
            }
        }
    }
    
    /**
     * Calculate normalized cost score (1.0 = cheapest, 0.0 = most expensive)
     */
    private fun calculateNormalizedCostScore(fee: BigDecimal, allFees: List<BigDecimal>): Double {
        if (allFees.size <= 1) {
            return 1.0 // Only one provider or no comparison possible
        }
        
        val minFee = allFees.minOrNull() ?: return 0.5
        val maxFee = allFees.maxOrNull() ?: return 0.5
        
        return if (maxFee == minFee) {
            1.0 // All fees are the same, everyone gets max score
        } else {
            // Invert the score: lower fee = higher score
            val range = maxFee.subtract(minFee)
            val position = fee.subtract(minFee)
            val normalizedPosition = position.divide(range, 4, java.math.RoundingMode.HALF_UP)
            
            // Invert: 0.0 becomes 1.0, 1.0 becomes 0.0
            1.0 - normalizedPosition.toDouble()
        }
    }
    
    /**
     * Get cost comparison data for all providers
     */
    fun getCostComparison(context: RoutingContext, allProviders: List<PaymentProvider>): CostComparison {
        val providerFees = allProviders.mapNotNull { provider ->
            try {
                val fee = provider.feeFor(context.currency, context.amount)
                ProviderFee(
                    providerName = provider.name,
                    fee = fee.amount,
                    feeType = fee.type.name,
                    currency = fee.currency.currencyCode
                )
            } catch (e: Exception) {
                logger.warn("Failed to get fee for provider ${provider.name}", e)
                null
            }
        }
        
        val fees = providerFees.map { it.fee }
        val minFee = fees.minOrNull() ?: BigDecimal.ZERO
        val maxFee = fees.maxOrNull() ?: BigDecimal.ZERO
        val avgFee = if (fees.isNotEmpty()) {
            fees.reduce { acc, fee -> acc.add(fee) }
                .divide(BigDecimal(fees.size), 2, java.math.RoundingMode.HALF_UP)
        } else {
            BigDecimal.ZERO
        }
        
        return CostComparison(
            providerFees = providerFees,
            minFee = minFee,
            maxFee = maxFee,
            avgFee = avgFee,
            currency = context.currency.currencyCode
        )
    }
}

/**
 * Provider fee information
 */
data class ProviderFee(
    val providerName: String,
    val fee: BigDecimal,
    val feeType: String,
    val currency: String
)

/**
 * Cost comparison across all providers
 */
data class CostComparison(
    val providerFees: List<ProviderFee>,
    val minFee: BigDecimal,
    val maxFee: BigDecimal,
    val avgFee: BigDecimal,
    val currency: String
) {
    /**
     * Get the cheapest provider
     */
    fun getCheapestProvider(): ProviderFee? = providerFees.minByOrNull { it.fee }
    
    /**
     * Get the most expensive provider
     */
    fun getMostExpensiveProvider(): ProviderFee? = providerFees.maxByOrNull { it.fee }
    
    /**
     * Calculate savings compared to most expensive provider
     */
    fun calculateSavings(providerName: String): BigDecimal? {
        val providerFee = providerFees.find { it.providerName == providerName }?.fee
        return if (providerFee != null && maxFee > BigDecimal.ZERO) {
            maxFee.subtract(providerFee)
        } else {
            null
        }
    }
}
