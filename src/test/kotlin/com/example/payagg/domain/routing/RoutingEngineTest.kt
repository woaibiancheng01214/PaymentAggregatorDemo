package com.example.payagg.domain.routing

import com.example.payagg.adapters.providers.PaymentProviderRegistry
import com.example.payagg.domain.CardNetwork
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
    private val rulesStrategy = mockk<RulesStrategy>()
    private val costStrategy = mockk<CostStrategy>()
    private val weightStrategy = mockk<WeightStrategy>()
    private val healthStrategy = mockk<HealthStrategy>()
    
    private lateinit var routingEngine: RoutingEngine
    
    private val mockProvider1 = mockk<PaymentProvider>()
    private val mockProvider2 = mockk<PaymentProvider>()
    private val mockProvider3 = mockk<PaymentProvider>()
    
    @BeforeEach
    fun setUp() {
        clearAllMocks()
        
        every { mockProvider1.name } returns "Provider1"
        every { mockProvider2.name } returns "Provider2"
        every { mockProvider3.name } returns "Provider3"
        
        routingEngine = RoutingEngine(
            providerRegistry,
            eligibilityStrategy,
            rulesStrategy,
            costStrategy,
            weightStrategy,
            healthStrategy
        )
    }
    
    @Test
    fun `should route successfully through all strategies`() {
        // Given
        val context = RoutingContext(
            amount = BigDecimal("100.00"), // $100.00
            currency = Currency.getInstance("USD"),
            country = "US",
            cardNetwork = CardNetwork.VISA,
            merchantId = "merchant-123"
        )
        
        val allProviders = listOf(mockProvider1, mockProvider2, mockProvider3)
        val eligibleProviders = listOf(mockProvider1, mockProvider2)
        val rulesFilteredProviders = listOf(mockProvider1, mockProvider2)
        val healthyProviders = listOf(mockProvider1, mockProvider2)
        val costSortedProviders = listOf(mockProvider2, mockProvider1) // Provider2 cheaper
        val finalProviders = listOf(mockProvider2, mockProvider1)
        
        every { providerRegistry.getAllProviders() } returns allProviders
        
        every { eligibilityStrategy.route(context, allProviders) } returns RoutingResult(
            providers = eligibleProviders,
            strategy = "ELIGIBILITY",
            reason = "Filtered by eligibility",
            metadata = mapOf("eligible_count" to 2)
        )
        
        every { rulesStrategy.route(context, eligibleProviders) } returns RoutingResult(
            providers = rulesFilteredProviders,
            strategy = "RULES",
            reason = "No rules applied",
            metadata = emptyMap()
        )
        
        every { healthStrategy.route(context, rulesFilteredProviders) } returns RoutingResult(
            providers = healthyProviders,
            strategy = "HEALTH",
            reason = "All providers healthy",
            metadata = mapOf("healthy_count" to 2)
        )
        
        every { costStrategy.route(context, healthyProviders) } returns RoutingResult(
            providers = costSortedProviders,
            strategy = "COST",
            reason = "Sorted by cost",
            metadata = mapOf("lowest_fee_provider" to "Provider2")
        )
        
        every { weightStrategy.route(context, costSortedProviders) } returns RoutingResult(
            providers = finalProviders,
            strategy = "WEIGHT",
            reason = "Applied weights",
            metadata = emptyMap()
        )
        
        // When
        val result = routingEngine.route(context)
        
        // Then
        assertThat(result.candidates).containsExactly("Provider2", "Provider1")
        assertThat(result.selectedProvider).isEqualTo("Provider2")
        assertThat(result.strategyUsed).containsExactly("ELIGIBILITY", "RULES", "HEALTH", "COST", "WEIGHT")
        assertThat(result.reason).contains("Routing completed successfully")
        
        verify { eligibilityStrategy.route(context, allProviders) }
        verify { rulesStrategy.route(context, eligibleProviders) }
        verify { healthStrategy.route(context, rulesFilteredProviders) }
        verify { costStrategy.route(context, healthyProviders) }
        verify { weightStrategy.route(context, costSortedProviders) }
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
        assertThat(result.strategyUsed).containsExactly("ELIGIBILITY")
        assertThat(result.reason).isEqualTo("No eligible providers found")
        
        verify { eligibilityStrategy.route(context, allProviders) }
        verify(exactly = 0) { rulesStrategy.route(any(), any()) }
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
        val rulesFilteredProviders = listOf(mockProvider1, mockProvider2)
        
        every { providerRegistry.getAllProviders() } returns allProviders
        
        every { eligibilityStrategy.route(context, allProviders) } returns RoutingResult(
            providers = eligibleProviders,
            strategy = "ELIGIBILITY",
            reason = "Filtered by eligibility",
            metadata = emptyMap()
        )
        
        every { rulesStrategy.route(context, eligibleProviders) } returns RoutingResult(
            providers = rulesFilteredProviders,
            strategy = "RULES",
            reason = "No rules applied",
            metadata = emptyMap()
        )
        
        every { healthStrategy.route(context, rulesFilteredProviders) } returns RoutingResult(
            providers = emptyList(),
            strategy = "HEALTH",
            reason = "No healthy providers",
            metadata = mapOf("healthy_count" to 0)
        )
        
        // When
        val result = routingEngine.route(context)
        
        // Then
        assertThat(result.candidates).isEmpty()
        assertThat(result.selectedProvider).isNull()
        assertThat(result.strategyUsed).containsExactly("ELIGIBILITY", "RULES", "HEALTH")
        assertThat(result.reason).isEqualTo("No healthy providers available")
        
        verify { healthStrategy.route(context, rulesFilteredProviders) }
        verify(exactly = 0) { costStrategy.route(any(), any()) }
    }
}
