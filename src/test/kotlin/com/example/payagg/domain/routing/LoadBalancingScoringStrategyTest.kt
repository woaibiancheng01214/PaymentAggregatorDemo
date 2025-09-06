package com.example.payagg.domain.routing

import com.example.payagg.config.ConfigKeys
import com.example.payagg.domain.CardNetwork
import com.example.payagg.ports.ConfigPort
import com.example.payagg.ports.PaymentProvider
import io.mockk.*
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.util.*

class LoadBalancingScoringStrategyTest {
    
    private val configPort = mockk<ConfigPort>()
    private lateinit var loadBalancingStrategy: LoadBalancingScoringStrategy
    
    private val mockProvider1 = mockk<PaymentProvider>()
    private val mockProvider2 = mockk<PaymentProvider>()
    private val mockProvider3 = mockk<PaymentProvider>()
    
    private val context = RoutingContext(
        amount = BigDecimal("100.00"),
        currency = Currency.getInstance("USD"),
        country = "US",
        cardNetwork = CardNetwork.VISA,
        merchantId = "merchant-123"
    )
    
    @BeforeEach
    fun setUp() {
        clearAllMocks()

        // Use the hardcoded provider names from LoadBalancingScoringStrategy defaults
        every { mockProvider1.name } returns "StripeMock"      // Default weight: 60
        every { mockProvider2.name } returns "AdyenMock"       // Default weight: 30
        every { mockProvider3.name } returns "LocalBankMock"   // Default weight: 10

        // Setup default weight configuration
        every { configPort.getConfig(ConfigKeys.ROUTING_WEIGHTS, Map::class.java) } returns mapOf(
            "StripeMock" to 60,
            "AdyenMock" to 30,
            "LocalBankMock" to 10
        )

        loadBalancingStrategy = LoadBalancingScoringStrategy(configPort)
    }
    
    @Test
    fun `should return highest score for provider with highest weight`() {
        // Given
        val providers = listOf(mockProvider1, mockProvider2, mockProvider3)

        // When
        val score1 = loadBalancingStrategy.calculateScore(context, mockProvider1, providers) // StripeMock: Weight 60
        val score2 = loadBalancingStrategy.calculateScore(context, mockProvider2, providers) // AdyenMock: Weight 30
        val score3 = loadBalancingStrategy.calculateScore(context, mockProvider3, providers) // LocalBankMock: Weight 10

        // Then
        assertThat(score1).isEqualTo(1.0) // Highest weight gets max score
        assertThat(score2).isEqualTo(0.5) // 30/60 = 0.5
        assertThat(score3).isCloseTo(0.167, within(0.001)) // 10/60 ≈ 0.167
        assertThat(score1).isGreaterThan(score2)
        assertThat(score2).isGreaterThan(score3)
    }
    
    @Test
    fun `should return equal scores when all providers have same weight`() {
        // Given
        every { configPort.getConfig(ConfigKeys.ROUTING_WEIGHTS, Map::class.java) } returns mapOf(
            "StripeMock" to 30,
            "AdyenMock" to 30,
            "LocalBankMock" to 30
        )

        val providers = listOf(mockProvider1, mockProvider2, mockProvider3)

        // When
        val score1 = loadBalancingStrategy.calculateScore(context, mockProvider1, providers)
        val score2 = loadBalancingStrategy.calculateScore(context, mockProvider2, providers)
        val score3 = loadBalancingStrategy.calculateScore(context, mockProvider3, providers)

        // Then
        assertThat(score1).isEqualTo(1.0)
        assertThat(score2).isEqualTo(1.0)
        assertThat(score3).isEqualTo(1.0)
    }
    
    @Test
    fun `should handle missing provider weight gracefully`() {
        // Given
        val unknownProvider = mockk<PaymentProvider>()
        every { unknownProvider.name } returns "UnknownProvider"
        
        val providers = listOf(mockProvider1, unknownProvider)
        
        // When
        val score1 = loadBalancingStrategy.calculateScore(context, mockProvider1, providers) // Has weight: 60
        val scoreUnknown = loadBalancingStrategy.calculateScore(context, unknownProvider, providers) // No weight: 0
        
        // Then
        assertThat(score1).isEqualTo(1.0) // Max weight
        assertThat(scoreUnknown).isEqualTo(0.0) // No weight = 0 score
    }
    
    @Test
    fun `should return correct metadata for load balancing scoring`() {
        // When
        val metadata = loadBalancingStrategy.getScoreMetadata(context, mockProvider1, 1.0)
        
        // Then
        assertThat(metadata).containsEntry("provider_weight", 60)
        assertThat(metadata).containsEntry("total_weight", 100) // 60 + 30 + 10
        assertThat(metadata).containsEntry("weight_percentage", "60.0%")
        assertThat(metadata).containsKey("all_weights")
        
        @Suppress("UNCHECKED_CAST")
        val allWeights = metadata["all_weights"] as Map<String, Int>
        assertThat(allWeights).containsEntry("StripeMock", 60)
        assertThat(allWeights).containsEntry("AdyenMock", 30)
        assertThat(allWeights).containsEntry("LocalBankMock", 10)
    }
    
    @Test
    fun `should use default weights when no configuration provided`() {
        // Given
        every { configPort.getConfig(ConfigKeys.ROUTING_WEIGHTS, Map::class.java) } returns null

        // When
        val score = loadBalancingStrategy.calculateScore(context, mockProvider1, listOf(mockProvider1))

        // Then
        assertThat(score).isEqualTo(1.0) // Single provider with default weight gets max score
    }
    
    @Test
    fun `should handle configuration errors gracefully`() {
        // Given
        every { configPort.getConfig(ConfigKeys.ROUTING_WEIGHTS, Map::class.java) } throws RuntimeException("Config error")

        // When
        val score = loadBalancingStrategy.calculateScore(context, mockProvider1, listOf(mockProvider1))

        // Then
        assertThat(score).isEqualTo(1.0) // Should use default weights, single provider gets max score
    }
    
    @Test
    fun `should verify strategy type is LOAD_BALANCING`() {
        // Then
        assertThat(loadBalancingStrategy.strategyType).isEqualTo(RoutingStrategyType.LOAD_BALANCING)
    }
    
    @Test
    fun `should calculate load distribution correctly`() {
        // Given
        val providers = listOf(mockProvider1, mockProvider2, mockProvider3)
        
        // When
        val distribution = loadBalancingStrategy.getLoadDistribution(providers)
        
        // Then
        assertThat(distribution.totalWeight).isEqualTo(100) // 60 + 30 + 10
        assertThat(distribution.providers).hasSize(3)
        
        val highCapacityInfo = distribution.providers.find { it.providerName == "StripeMock" }
        assertThat(highCapacityInfo?.weight).isEqualTo(60)
        assertThat(highCapacityInfo?.percentage).isEqualTo(60.0)

        val mediumCapacityInfo = distribution.providers.find { it.providerName == "AdyenMock" }
        assertThat(mediumCapacityInfo?.weight).isEqualTo(30)
        assertThat(mediumCapacityInfo?.percentage).isEqualTo(30.0)

        val lowCapacityInfo = distribution.providers.find { it.providerName == "LocalBankMock" }
        assertThat(lowCapacityInfo?.weight).isEqualTo(10)
        assertThat(lowCapacityInfo?.percentage).isEqualTo(10.0)

        assertThat(distribution.getHighestLoadProvider()?.providerName).isEqualTo("StripeMock")
        assertThat(distribution.getLowestLoadProvider()?.providerName).isEqualTo("LocalBankMock")
    }
    
    @Test
    fun `should identify balanced load distribution`() {
        // Given
        every { configPort.getConfig(ConfigKeys.ROUTING_WEIGHTS, Map::class.java) } returns mapOf(
            "StripeMock" to 35,
            "AdyenMock" to 35,
            "LocalBankMock" to 30
        )

        val providers = listOf(mockProvider1, mockProvider2, mockProvider3)

        // When
        val distribution = loadBalancingStrategy.getLoadDistribution(providers)

        // Then
        assertThat(distribution.isBalanced).isTrue() // No provider has >70% and spread is <60%
        assertThat(distribution.getImbalanceRatio()).isLessThan(2.0) // Low imbalance ratio
    }
    
    @Test
    fun `should identify imbalanced load distribution`() {
        // Given
        every { configPort.getConfig(ConfigKeys.ROUTING_WEIGHTS, Map::class.java) } returns mapOf(
            "StripeMock" to 80,
            "AdyenMock" to 15,
            "LocalBankMock" to 5
        )

        val providers = listOf(mockProvider1, mockProvider2, mockProvider3)

        // When
        val distribution = loadBalancingStrategy.getLoadDistribution(providers)

        // Then
        assertThat(distribution.isBalanced).isFalse() // One provider has >70%
        assertThat(distribution.getImbalanceRatio()).isGreaterThan(2.0) // High imbalance ratio
    }
    
    @Test
    fun `should provide recommended weights for better balance`() {
        // Given
        every { configPort.getConfig(ConfigKeys.ROUTING_WEIGHTS, Map::class.java) } returns mapOf(
            "StripeMock" to 80,
            "AdyenMock" to 15,
            "LocalBankMock" to 5
        )

        val providers = listOf(mockProvider1, mockProvider2, mockProvider3)

        // When
        val recommendedWeights = loadBalancingStrategy.getRecommendedWeights(providers)

        // Then
        assertThat(recommendedWeights).isNotEmpty()
        assertThat(recommendedWeights.values.sum()).isGreaterThan(0) // Should have positive weights

        // Recommended weights should be more balanced than current
        val maxRecommended = recommendedWeights.values.maxOrNull() ?: 0
        val minRecommended = recommendedWeights.values.minOrNull() ?: 0
        val recommendedSpread = maxRecommended - minRecommended

        assertThat(recommendedSpread).isLessThan(75) // Should be more balanced than 80-5=75
    }
    
    @Test
    fun `should handle string and numeric weight values`() {
        // Given
        every { configPort.getConfig(ConfigKeys.ROUTING_WEIGHTS, Map::class.java) } returns mapOf(
            "StripeMock" to "60",  // String value
            "AdyenMock" to 30,  // Numeric value
            "LocalBankMock" to "invalid" // Invalid string
        )

        val providers = listOf(mockProvider1, mockProvider2, mockProvider3)

        // When
        val score1 = loadBalancingStrategy.calculateScore(context, mockProvider1, providers)
        val score2 = loadBalancingStrategy.calculateScore(context, mockProvider2, providers)
        val score3 = loadBalancingStrategy.calculateScore(context, mockProvider3, providers)

        // Then
        assertThat(score1).isEqualTo(1.0) // "60" parsed correctly
        assertThat(score2).isEqualTo(0.5) // 30/60 = 0.5
        assertThat(score3).isEqualTo(0.0) // Invalid string becomes 0
    }
}
