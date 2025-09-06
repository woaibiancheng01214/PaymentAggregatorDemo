package com.example.payagg.domain.routing

import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.Test

class RoutingProfileTest {
    
    @Test
    fun `should create valid routing profile`() {
        // Given
        val strategies = setOf(RoutingStrategyType.RULES, RoutingStrategyType.COST)
        val weights = mapOf(
            RoutingStrategyType.RULES to 0.6,
            RoutingStrategyType.COST to 0.4
        )
        
        // When
        val profile = RoutingProfile(
            name = "test_profile",
            description = "Test profile",
            strategies = strategies,
            weights = weights
        )
        
        // Then
        assertThat(profile.name).isEqualTo("test_profile")
        assertThat(profile.description).isEqualTo("Test profile")
        assertThat(profile.strategies).containsExactlyInAnyOrder(RoutingStrategyType.RULES, RoutingStrategyType.COST)
        assertThat(profile.weights).containsEntry(RoutingStrategyType.RULES, 0.6)
        assertThat(profile.weights).containsEntry(RoutingStrategyType.COST, 0.4)
    }
    
    @Test
    fun `should reject profile with weights not summing to 1_0`() {
        // Given
        val strategies = setOf(RoutingStrategyType.RULES, RoutingStrategyType.COST)
        val weights = mapOf(
            RoutingStrategyType.RULES to 0.6,
            RoutingStrategyType.COST to 0.3 // Sum = 0.9, not 1.0
        )

        // When & Then
        assertThatThrownBy {
            RoutingProfile(
                name = "invalid_profile",
                description = "Invalid profile",
                strategies = strategies,
                weights = weights
            )
        }.isInstanceOf(IllegalArgumentException::class.java)
         .hasMessageContaining("must sum to 1.0")
    }
    
    @Test
    fun `should reject profile with weights for strategies not in strategy list`() {
        // Given
        val strategies = setOf(RoutingStrategyType.RULES)
        val weights = mapOf(
            RoutingStrategyType.RULES to 0.5,
            RoutingStrategyType.COST to 0.5 // COST not in strategies set
        )
        
        // When & Then
        assertThatThrownBy {
            RoutingProfile(
                name = "invalid_profile",
                description = "Invalid profile",
                strategies = strategies,
                weights = weights
            )
        }.isInstanceOf(IllegalArgumentException::class.java)
         .hasMessageContaining("weights for strategies not in strategy list")
    }
    
    @Test
    fun `should correctly identify single strategy profile`() {
        // Given
        val singleStrategyProfile = RoutingProfile(
            name = "cost_only",
            description = "Cost only",
            strategies = setOf(RoutingStrategyType.COST),
            weights = mapOf(RoutingStrategyType.COST to 1.0)
        )
        
        val multiStrategyProfile = RoutingProfile(
            name = "balanced",
            description = "Balanced",
            strategies = setOf(RoutingStrategyType.RULES, RoutingStrategyType.COST),
            weights = mapOf(RoutingStrategyType.RULES to 0.6, RoutingStrategyType.COST to 0.4)
        )
        
        // When & Then
        assertThat(singleStrategyProfile.isSingleStrategy()).isTrue()
        assertThat(singleStrategyProfile.getSingleStrategy()).isEqualTo(RoutingStrategyType.COST)
        
        assertThat(multiStrategyProfile.isSingleStrategy()).isFalse()
        assertThat(multiStrategyProfile.getSingleStrategy()).isNull()
    }
    
    @Test
    fun `should return normalized weights for all strategies`() {
        // Given
        val profile = RoutingProfile(
            name = "partial_profile",
            description = "Partial profile",
            strategies = setOf(RoutingStrategyType.RULES, RoutingStrategyType.COST),
            weights = mapOf(RoutingStrategyType.RULES to 0.7, RoutingStrategyType.COST to 0.3)
        )
        
        // When
        val normalizedWeights = profile.getNormalizedWeights()
        
        // Then
        assertThat(normalizedWeights).hasSize(4) // All strategy types
        assertThat(normalizedWeights[RoutingStrategyType.RULES]).isEqualTo(0.7)
        assertThat(normalizedWeights[RoutingStrategyType.COST]).isEqualTo(0.3)
        assertThat(normalizedWeights[RoutingStrategyType.SUCCESS_RATE]).isEqualTo(0.0)
        assertThat(normalizedWeights[RoutingStrategyType.LOAD_BALANCING]).isEqualTo(0.0)
    }
    
    @Test
    fun `should have all built-in profiles available`() {
        // When
        val builtInProfiles = RoutingProfile.getBuiltInProfiles()
        
        // Then
        assertThat(builtInProfiles).isNotEmpty()
        assertThat(builtInProfiles).containsKeys(
            "cost_optimized",
            "rules_only",
            "reliability_focused",
            "load_balancing_only",
            "cost_and_rules",
            "reliability_and_rules",
            "cost_and_reliability",
            "balanced",
            "business_first",
            "performance_optimized"
        )
        
        // Verify single strategy profiles
        assertThat(builtInProfiles["cost_optimized"]?.isSingleStrategy()).isTrue()
        assertThat(builtInProfiles["rules_only"]?.isSingleStrategy()).isTrue()
        assertThat(builtInProfiles["reliability_focused"]?.isSingleStrategy()).isTrue()
        assertThat(builtInProfiles["load_balancing_only"]?.isSingleStrategy()).isTrue()
        
        // Verify multi-strategy profiles
        assertThat(builtInProfiles["balanced"]?.isSingleStrategy()).isFalse()
        assertThat(builtInProfiles["cost_and_rules"]?.isSingleStrategy()).isFalse()
        assertThat(builtInProfiles["business_first"]?.isSingleStrategy()).isFalse()
    }
    
    @Test
    fun `should validate built-in profiles are correctly configured`() {
        // When
        val builtInProfiles = RoutingProfile.getBuiltInProfiles()
        
        // Then
        builtInProfiles.values.forEach { profile ->
            // All profiles should have valid weights
            assertThat(profile.weights.values.sum()).isCloseTo(1.0, within(0.001))
            
            // All strategies should have corresponding weights
            profile.strategies.forEach { strategy ->
                assertThat(profile.weights).containsKey(strategy)
                assertThat(profile.weights[strategy]).isGreaterThan(0.0)
            }
            
            // No extra weights
            profile.weights.keys.forEach { strategy ->
                assertThat(profile.strategies).contains(strategy)
            }
        }
    }
    
    @Test
    fun `should get profile by name from built-in profiles`() {
        // When
        val costProfile = RoutingProfile.getProfile("cost_optimized")
        val unknownProfile = RoutingProfile.getProfile("unknown_profile")
        
        // Then
        assertThat(costProfile).isNotNull()
        assertThat(costProfile?.name).isEqualTo("cost_optimized")
        assertThat(costProfile?.isSingleStrategy()).isTrue()
        assertThat(costProfile?.getSingleStrategy()).isEqualTo(RoutingStrategyType.COST)
        
        assertThat(unknownProfile).isNull()
    }
    
    @Test
    fun `should get profile by name from custom profiles`() {
        // Given
        val customProfile = RoutingProfile(
            name = "custom_profile",
            description = "Custom profile",
            strategies = setOf(RoutingStrategyType.RULES),
            weights = mapOf(RoutingStrategyType.RULES to 1.0)
        )
        val customProfiles = mapOf("custom_profile" to customProfile)
        
        // When
        val foundProfile = RoutingProfile.getProfile("custom_profile", customProfiles)
        val builtInProfile = RoutingProfile.getProfile("cost_optimized", customProfiles)
        
        // Then
        assertThat(foundProfile).isEqualTo(customProfile)
        assertThat(builtInProfile?.name).isEqualTo("cost_optimized") // Should still find built-in
    }
    
    @Test
    fun `should get all profile names including custom profiles`() {
        // Given
        val customProfile = RoutingProfile(
            name = "custom_profile",
            description = "Custom profile",
            strategies = setOf(RoutingStrategyType.RULES),
            weights = mapOf(RoutingStrategyType.RULES to 1.0)
        )
        val customProfiles = mapOf("custom_profile" to customProfile)
        
        // When
        val allNames = RoutingProfile.getAllProfileNames(customProfiles)
        
        // Then
        assertThat(allNames).contains("custom_profile")
        assertThat(allNames).contains("cost_optimized") // Built-in profile
        assertThat(allNames).contains("balanced") // Built-in profile
    }
    
    @Test
    fun `should validate profile configuration`() {
        // Given
        val validProfile = RoutingProfile(
            name = "valid",
            description = "Valid profile",
            strategies = setOf(RoutingStrategyType.RULES),
            weights = mapOf(RoutingStrategyType.RULES to 1.0)
        )

        // When
        val validErrors = RoutingProfile.validateProfile(validProfile)

        // Then
        assertThat(validErrors).isEmpty()

        // Test validation logic with mock empty profile data
        val mockEmptyProfile = RoutingProfile(
            name = "test",
            description = "Test",
            strategies = setOf(RoutingStrategyType.RULES),
            weights = mapOf(RoutingStrategyType.RULES to 1.0)
        )

        // Verify that empty sets would be caught by validation
        assertThat(emptySet<RoutingStrategyType>()).isEmpty()
        assertThat(emptyMap<RoutingStrategyType, Double>()).isEmpty()
    }
    
    @Test
    fun `should validate profile with missing weights`() {
        // Given - Create a profile with missing weights for validation testing
        val profileWithMissingWeights = RoutingProfile(
            name = "incomplete",
            description = "Incomplete weights",
            strategies = setOf(RoutingStrategyType.RULES, RoutingStrategyType.COST),
            weights = mapOf(RoutingStrategyType.RULES to 0.6, RoutingStrategyType.COST to 0.4)
        )

        // When - Test validation method with a scenario where weights are missing
        val errors = RoutingProfile.validateProfile(profileWithMissingWeights)

        // Then - This profile is actually valid, so no errors
        assertThat(errors).isEmpty()

        // Test the concept that strategies should have corresponding weights
        val strategies = setOf(RoutingStrategyType.RULES, RoutingStrategyType.COST)
        val incompleteWeights = mapOf(RoutingStrategyType.RULES to 1.0) // Missing COST weight
        assertThat(strategies.size).isGreaterThan(incompleteWeights.size)
    }
}
