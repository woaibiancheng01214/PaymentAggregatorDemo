package com.example.payagg.domain.routing

import com.example.payagg.adapters.providers.PaymentProviderRegistry
import com.example.payagg.config.ConfigKeys
import com.example.payagg.domain.CardNetwork
import com.example.payagg.ports.Fee
import com.example.payagg.ports.FeeType
import com.example.payagg.ports.ConfigPort
import com.example.payagg.ports.PaymentProvider
import io.mockk.*
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.util.*

class RoutingEngineTest {

    private val providerRegistry = mockk<PaymentProviderRegistry>()
    private val eligibilityStrategy = mockk<EligibilityStrategy>()
    private val rulesScoringStrategy = mockk<RulesScoringStrategy>()
    private val costScoringStrategy = mockk<CostScoringStrategy>()
    private val successRateStrategy = mockk<SuccessRateStrategy>()
    private val loadBalancingStrategy = mockk<LoadBalancingScoringStrategy>()
    private val configPort = mockk<ConfigPort>()

    private lateinit var routingEngine: RoutingEngine

    private val mockProvider1 = mockk<PaymentProvider>()
    private val mockProvider2 = mockk<PaymentProvider>()
    private val mockProvider3 = mockk<PaymentProvider>()
    
    @BeforeEach
    fun setUp() {
        clearAllMocks()

        every { mockProvider1.name } returns "StripeMock"
        every { mockProvider2.name } returns "AdyenMock"
        every { mockProvider3.name } returns "LocalBankMock"

        // Setup default fee responses
        every { mockProvider1.feeFor(any(), any()) } returns Fee(BigDecimal("3.00"), Currency.getInstance("USD"), FeeType.FIXED)
        every { mockProvider2.feeFor(any(), any()) } returns Fee(BigDecimal("2.50"), Currency.getInstance("USD"), FeeType.FIXED)
        every { mockProvider3.feeFor(any(), any()) } returns Fee(BigDecimal("2.00"), Currency.getInstance("USD"), FeeType.FIXED)

        // Setup default config responses
        every { configPort.getConfig(ConfigKeys.ROUTING_PROFILE, Map::class.java) } returns mapOf("profile" to "balanced")
        every { configPort.getConfig(ConfigKeys.ROUTING_PROFILES, Map::class.java) } returns null
        every { configPort.getConfig(ConfigKeys.ROUTING_WEIGHTS, Map::class.java) } returns mapOf(
            "StripeMock" to 60,
            "AdyenMock" to 30,
            "LocalBankMock" to 10
        )

        // Setup strategy type properties
        every { rulesScoringStrategy.strategyType } returns RoutingStrategyType.RULES
        every { costScoringStrategy.strategyType } returns RoutingStrategyType.COST
        every { successRateStrategy.strategyType } returns RoutingStrategyType.SUCCESS_RATE
        every { loadBalancingStrategy.strategyType } returns RoutingStrategyType.LOAD_BALANCING

        routingEngine = RoutingEngine(
            providerRegistry,
            eligibilityStrategy,
            rulesScoringStrategy,
            costScoringStrategy,
            successRateStrategy,
            loadBalancingStrategy,
            configPort
        )
    }
    
    @Test
    fun `should route successfully using balanced profile with all strategies`() {
        // Given
        val context = RoutingContext(
            amount = BigDecimal("100.00"),
            currency = Currency.getInstance("USD"),
            country = "US",
            cardNetwork = CardNetwork.VISA,
            merchantId = "merchant-123"
        )

        val allProviders = listOf(mockProvider1, mockProvider2)
        val eligibleProviders = listOf(mockProvider1, mockProvider2)

        every { providerRegistry.getAllProviders() } returns allProviders

        // Mock eligibility strategy
        every { eligibilityStrategy.route(context, allProviders) } returns RoutingResult(
            providers = eligibleProviders,
            strategy = "ELIGIBILITY",
            reason = "Filtered by eligibility",
            metadata = mapOf("eligible_count" to 2)
        )

        // Mock health checks
        every { successRateStrategy.isProviderHealthy(mockProvider1) } returns true
        every { successRateStrategy.isProviderHealthy(mockProvider2) } returns true
        every { successRateStrategy.getScoreMetadata(any(), any(), any()) } returns mapOf(
            "success_rate" to 0.95,
            "latency_ms" to 200,
            "total_transactions" to 1000,
            "last_updated" to "2023-01-01"
        )

        // Mock scoring strategies
        every { rulesScoringStrategy.calculateScore(context, mockProvider1, eligibleProviders) } returns 0.5
        every { rulesScoringStrategy.calculateScore(context, mockProvider2, eligibleProviders) } returns 0.8
        every { rulesScoringStrategy.getScoreMetadata(any(), any(), any()) } returns mapOf("rule" to "neutral")

        every { costScoringStrategy.calculateScore(context, mockProvider1, eligibleProviders) } returns 0.6
        every { costScoringStrategy.calculateScore(context, mockProvider2, eligibleProviders) } returns 1.0
        every { costScoringStrategy.getScoreMetadata(any(), any(), any()) } returns mapOf("fee" to "3.00")

        every { successRateStrategy.calculateScore(context, mockProvider1, eligibleProviders) } returns 0.95
        every { successRateStrategy.calculateScore(context, mockProvider2, eligibleProviders) } returns 0.97

        every { loadBalancingStrategy.calculateScore(context, mockProvider1, eligibleProviders) } returns 0.6
        every { loadBalancingStrategy.calculateScore(context, mockProvider2, eligibleProviders) } returns 0.3
        every { loadBalancingStrategy.getScoreMetadata(any(), any(), any()) } returns mapOf("weight" to 60)

        // When
        val result = routingEngine.route(context)

        // Then
        assertThat(result.candidates).isNotEmpty()
        assertThat(result.selectedProvider).isNotNull()
        assertThat(result.strategyUsed).containsExactly("Rules", "Success Rate", "Cost", "Load Balancing")
        assertThat(result.reason).contains("profile 'balanced'")

        // Verify metadata structure
        val metadata = result.metadata
        assertThat(metadata.getMetadata<RoutingProfileMetadata>()).isNotNull()
        assertThat(metadata.getMetadata<CompositeScoresMetadata>()).isNotNull()
        assertThat(metadata.getMetadata<StrategyWeightsMetadata>()).isNotNull()
        assertThat(metadata.getMetadata<IndividualScoresMetadata>()).isNotNull()

        verify { eligibilityStrategy.route(context, allProviders) }
        verify { rulesScoringStrategy.calculateScore(context, any(), any()) }
        verify { costScoringStrategy.calculateScore(context, any(), any()) }
        verify { successRateStrategy.calculateScore(context, any(), any()) }
        verify { loadBalancingStrategy.calculateScore(context, any(), any()) }
    }
    
    @Test
    fun `should return empty result when no eligible providers`() {
        // Given
        val context = RoutingContext(
            amount = BigDecimal("100.00"),
            currency = Currency.getInstance("USD"),
            country = "US",
            cardNetwork = CardNetwork.VISA,
            merchantId = "merchant-123"
        )

        val allProviders = listOf(mockProvider1, mockProvider2)

        every { providerRegistry.getAllProviders() } returns allProviders
        every { eligibilityStrategy.route(context, allProviders) } returns RoutingResult(
            providers = emptyList(),
            strategy = "ELIGIBILITY",
            reason = "No eligible providers",
            metadata = emptyMap()
        )

        // When
        val result = routingEngine.route(context)

        // Then
        assertThat(result.candidates).isEmpty()
        assertThat(result.selectedProvider).isNull()
        assertThat(result.strategyUsed).containsExactly("Rules")
        assertThat(result.reason).isEqualTo("No eligible providers found")

        verify { eligibilityStrategy.route(context, allProviders) }
        verify(exactly = 0) { rulesScoringStrategy.calculateScore(any(), any(), any()) }
    }

    @Test
    fun `should return empty result when no healthy providers`() {
        // Given
        val context = RoutingContext(
            amount = BigDecimal("100.00"),
            currency = Currency.getInstance("USD"),
            country = "US",
            cardNetwork = CardNetwork.VISA,
            merchantId = "merchant-123"
        )

        val allProviders = listOf(mockProvider1, mockProvider2)
        val eligibleProviders = listOf(mockProvider1, mockProvider2)

        every { providerRegistry.getAllProviders() } returns allProviders
        every { eligibilityStrategy.route(context, allProviders) } returns RoutingResult(
            providers = eligibleProviders,
            strategy = "ELIGIBILITY",
            reason = "Filtered by eligibility",
            metadata = emptyMap()
        )

        // Mock all providers as unhealthy
        every { successRateStrategy.isProviderHealthy(any()) } returns false
        every { successRateStrategy.getScoreMetadata(any(), any(), any()) } returns mapOf(
            "success_rate" to 0.5,
            "latency_ms" to 5000,
            "total_transactions" to 10,
            "last_updated" to "2023-01-01"
        )

        // When
        val result = routingEngine.route(context)

        // Then
        assertThat(result.candidates).isEmpty()
        assertThat(result.selectedProvider).isNull()
        assertThat(result.strategyUsed).containsExactly("Rules", "Success Rate")
        assertThat(result.reason).isEqualTo("No healthy providers found")

        verify { successRateStrategy.isProviderHealthy(any()) }
        verify(exactly = 0) { rulesScoringStrategy.calculateScore(any(), any(), any()) }
    }

    @Test
    fun `should use cost optimized profile for single strategy routing`() {
        // Given
        every { configPort.getConfig(ConfigKeys.ROUTING_PROFILE, Map::class.java) } returns mapOf("profile" to "cost_optimized")

        val context = RoutingContext(
            amount = BigDecimal("100.00"),
            currency = Currency.getInstance("USD"),
            country = "US",
            cardNetwork = CardNetwork.VISA,
            merchantId = "merchant-123"
        )

        val allProviders = listOf(mockProvider1, mockProvider2)
        val eligibleProviders = listOf(mockProvider1, mockProvider2)

        every { providerRegistry.getAllProviders() } returns allProviders
        every { eligibilityStrategy.route(context, allProviders) } returns RoutingResult(
            providers = eligibleProviders,
            strategy = "ELIGIBILITY",
            reason = "Filtered by eligibility",
            metadata = emptyMap()
        )

        every { successRateStrategy.isProviderHealthy(any()) } returns true
        every { successRateStrategy.getScoreMetadata(any(), any(), any()) } returns mapOf("success_rate" to 0.95)

        every { costScoringStrategy.calculateScore(context, mockProvider1, eligibleProviders) } returns 0.6
        every { costScoringStrategy.calculateScore(context, mockProvider2, eligibleProviders) } returns 1.0
        every { costScoringStrategy.getScoreMetadata(any(), any(), any()) } returns mapOf("fee" to "2.50")

        // When
        val result = routingEngine.route(context)

        // Then
        assertThat(result.candidates).isNotEmpty()
        assertThat(result.selectedProvider).isEqualTo("AdyenMock") // Provider2 has higher cost score
        assertThat(result.strategyUsed).containsExactly("Rules", "Success Rate", "Cost")
        assertThat(result.reason).contains("single strategy (Cost)")

        // Verify only cost strategy was used for scoring
        verify { costScoringStrategy.calculateScore(context, any(), any()) }
        verify(exactly = 0) { rulesScoringStrategy.calculateScore(any(), any(), any()) }
        verify(exactly = 0) { loadBalancingStrategy.calculateScore(any(), any(), any()) }
    }
}
