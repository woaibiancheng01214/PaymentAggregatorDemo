package com.example.payagg.domain.routing

import com.example.payagg.domain.CardNetwork
import com.example.payagg.ports.Fee
import com.example.payagg.ports.FeeType
import com.example.payagg.ports.PaymentProvider
import io.mockk.*
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.util.*

class CostScoringStrategyTest {
    
    private lateinit var costScoringStrategy: CostScoringStrategy
    
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
        
        every { mockProvider1.name } returns "ExpensiveProvider"
        every { mockProvider2.name } returns "CheapProvider"
        every { mockProvider3.name } returns "MediumProvider"
        
        costScoringStrategy = CostScoringStrategy()
    }
    
    @Test
    fun `should return highest score for cheapest provider`() {
        // Given
        val providers = listOf(mockProvider1, mockProvider2, mockProvider3)
        
        every { mockProvider1.feeFor(any(), any()) } returns Fee(BigDecimal("5.00"), Currency.getInstance("USD"), FeeType.FIXED)
        every { mockProvider2.feeFor(any(), any()) } returns Fee(BigDecimal("2.00"), Currency.getInstance("USD"), FeeType.FIXED)
        every { mockProvider3.feeFor(any(), any()) } returns Fee(BigDecimal("3.50"), Currency.getInstance("USD"), FeeType.FIXED)
        
        // When
        val score1 = costScoringStrategy.calculateScore(context, mockProvider1, providers)
        val score2 = costScoringStrategy.calculateScore(context, mockProvider2, providers)
        val score3 = costScoringStrategy.calculateScore(context, mockProvider3, providers)
        
        // Then
        assertThat(score2).isEqualTo(1.0) // Cheapest provider gets highest score
        assertThat(score1).isEqualTo(0.0) // Most expensive provider gets lowest score
        assertThat(score3).isBetween(0.0, 1.0) // Medium provider gets medium score
        assertThat(score2).isGreaterThan(score3)
        assertThat(score3).isGreaterThan(score1)
    }
    
    @Test
    fun `should return equal scores when all providers have same fee`() {
        // Given
        val providers = listOf(mockProvider1, mockProvider2, mockProvider3)
        val sameFee = Fee(BigDecimal("3.00"), Currency.getInstance("USD"), FeeType.FIXED)
        
        every { mockProvider1.feeFor(any(), any()) } returns sameFee
        every { mockProvider2.feeFor(any(), any()) } returns sameFee
        every { mockProvider3.feeFor(any(), any()) } returns sameFee
        
        // When
        val score1 = costScoringStrategy.calculateScore(context, mockProvider1, providers)
        val score2 = costScoringStrategy.calculateScore(context, mockProvider2, providers)
        val score3 = costScoringStrategy.calculateScore(context, mockProvider3, providers)
        
        // Then
        assertThat(score1).isEqualTo(1.0)
        assertThat(score2).isEqualTo(1.0)
        assertThat(score3).isEqualTo(1.0)
    }
    
    @Test
    fun `should handle fee calculation errors gracefully`() {
        // Given
        val providers = listOf(mockProvider1, mockProvider2)
        
        every { mockProvider1.feeFor(any(), any()) } throws RuntimeException("Fee calculation failed")
        every { mockProvider2.feeFor(any(), any()) } returns Fee(BigDecimal("2.00"), Currency.getInstance("USD"), FeeType.FIXED)
        
        // When
        val score1 = costScoringStrategy.calculateScore(context, mockProvider1, providers)
        val score2 = costScoringStrategy.calculateScore(context, mockProvider2, providers)
        
        // Then
        assertThat(score1).isEqualTo(0.0) // Error results in lowest score
        assertThat(score2).isGreaterThan(0.0) // Working provider gets positive score
    }
    
    @Test
    fun `should return correct metadata for cost scoring`() {
        // Given
        val providers = listOf(mockProvider1)
        val fee = Fee(BigDecimal("2.50"), Currency.getInstance("USD"), FeeType.PERCENTAGE)
        
        every { mockProvider1.feeFor(any(), any()) } returns fee
        
        // When
        val metadata = costScoringStrategy.getScoreMetadata(context, mockProvider1, 0.8)
        
        // Then
        assertThat(metadata).containsEntry("provider_fee", BigDecimal("2.50"))
        assertThat(metadata).containsEntry("fee_currency", "USD")
        assertThat(metadata).containsEntry("fee_type", "PERCENTAGE")
        assertThat(metadata).containsEntry("amount_processed", BigDecimal("100.00"))
        assertThat(metadata).containsEntry("currency_processed", "USD")
    }
    
    @Test
    fun `should handle single provider scenario`() {
        // Given
        val providers = listOf(mockProvider1)
        
        every { mockProvider1.feeFor(any(), any()) } returns Fee(BigDecimal("3.00"), Currency.getInstance("USD"), FeeType.FIXED)
        
        // When
        val score = costScoringStrategy.calculateScore(context, mockProvider1, providers)
        
        // Then
        assertThat(score).isEqualTo(1.0) // Single provider gets max score
    }
    
    @Test
    fun `should verify strategy type is COST`() {
        // Then
        assertThat(costScoringStrategy.strategyType).isEqualTo(RoutingStrategyType.COST)
    }
    
    @Test
    fun `should calculate cost comparison correctly`() {
        // Given
        val providers = listOf(mockProvider1, mockProvider2, mockProvider3)
        
        every { mockProvider1.feeFor(any(), any()) } returns Fee(BigDecimal("5.00"), Currency.getInstance("USD"), FeeType.FIXED)
        every { mockProvider2.feeFor(any(), any()) } returns Fee(BigDecimal("2.00"), Currency.getInstance("USD"), FeeType.FIXED)
        every { mockProvider3.feeFor(any(), any()) } returns Fee(BigDecimal("3.50"), Currency.getInstance("USD"), FeeType.FIXED)
        
        // When
        val comparison = costScoringStrategy.getCostComparison(context, providers)
        
        // Then
        assertThat(comparison.minFee).isEqualTo(BigDecimal("2.00"))
        assertThat(comparison.maxFee).isEqualTo(BigDecimal("5.00"))
        assertThat(comparison.avgFee).isEqualTo(BigDecimal("3.50"))
        assertThat(comparison.currency).isEqualTo("USD")
        assertThat(comparison.providerFees).hasSize(3)
        
        val cheapestProvider = comparison.getCheapestProvider()
        assertThat(cheapestProvider?.providerName).isEqualTo("CheapProvider")
        assertThat(cheapestProvider?.fee).isEqualTo(BigDecimal("2.00"))
        
        val mostExpensiveProvider = comparison.getMostExpensiveProvider()
        assertThat(mostExpensiveProvider?.providerName).isEqualTo("ExpensiveProvider")
        assertThat(mostExpensiveProvider?.fee).isEqualTo(BigDecimal("5.00"))
        
        val savings = comparison.calculateSavings("CheapProvider")
        assertThat(savings).isEqualTo(BigDecimal("3.00")) // 5.00 - 2.00
    }
}
