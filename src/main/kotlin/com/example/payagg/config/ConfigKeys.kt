package com.example.payagg.config

/**
 * Centralized configuration keys for the Payment Aggregator system.
 * 
 * This object contains all configuration keys used throughout the application
 * to avoid magic strings and ensure consistency across components.
 * 
 * Usage:
 * ```kotlin
 * configPort.getConfig(ConfigKeys.ROUTING_RULES, List::class.java)
 * configPort.setConfig(ConfigKeys.PROVIDERS, providersConfig)
 * ```
 */
object ConfigKeys {
    
    // ========================================
    // Provider Configuration
    // ========================================
    
    /**
     * Configuration for payment providers including their capabilities,
     * supported currencies, countries, and networks.
     * 
     * Expected format: List of provider configurations
     * ```json
     * [
     *   {
     *     "name": "StripeMock",
     *     "currencies": ["USD", "EUR"],
     *     "countries": ["US", "GB", "DE"],
     *     "networks": ["VISA", "MASTERCARD"],
     *     "baseFeeBps": 300,
     *     "enabled": true
     *   }
     * ]
     * ```
     */
    const val PROVIDERS = "providers"
    
    // ========================================
    // Routing Configuration
    // ========================================
    
    /**
     * Routing rules configuration for smart payment routing.
     * 
     * Expected format: List of routing rules
     * ```json
     * [
     *   {
     *     "condition": {"country": "US", "network": "AMEX"},
     *     "action": {"prefer": ["AdyenMock"], "mode": "PREFERRED"}
     *   }
     * ]
     * ```
     */
    const val ROUTING_RULES = "routing_rules"
    
    /**
     * Routing strategies configuration to enable/disable specific strategies.
     *
     * Expected format: Map of strategy names to boolean flags
     * ```json
     * {
     *   "cost": true,
     *   "weight": true,
     *   "health": true,
     *   "rules": true
     * }
     * ```
     */
    const val ROUTING_STRATEGIES = "routing_strategies"

    /**
     * Routing profile configuration for predefined strategy combinations.
     *
     * Expected format: String profile name
     * ```json
     * "cost_optimized"
     * ```
     */
    const val ROUTING_PROFILE = "routing_profile"

    /**
     * Routing profiles definition with strategy combinations.
     *
     * Expected format: Map of profile names to strategy configurations
     * ```json
     * {
     *   "cost_optimized": {
     *     "strategies": ["cost"],
     *     "weights": {"cost": 1.0}
     *   },
     *   "balanced": {
     *     "strategies": ["rules", "cost", "success_rate", "load_balancing"],
     *     "weights": {"rules": 0.4, "cost": 0.3, "success_rate": 0.2, "load_balancing": 0.1}
     *   }
     * }
     * ```
     */
    const val ROUTING_PROFILES = "routing_profiles"
    
    /**
     * Provider weights for weighted routing strategy.
     * 
     * Expected format: Map of provider names to weight values
     * ```json
     * {
     *   "StripeMock": 60,
     *   "AdyenMock": 30,
     *   "LocalBankMock": 10
     * }
     * ```
     */
    const val ROUTING_WEIGHTS = "routing_weights"
    
    // ========================================
    // Foreign Exchange Configuration
    // ========================================
    
    /**
     * Foreign exchange rates configuration.
     * 
     * Expected format: Map of currency pairs to exchange rates
     * ```json
     * {
     *   "USD-EUR": 0.85,
     *   "EUR-USD": 1.18,
     *   "GBP-USD": 1.25
     * }
     * ```
     */
    const val FX_RATES = "fx_rates"
    
    // ========================================
    // Feature Flags
    // ========================================
    
    /**
     * Feature flags for enabling/disabling application features.
     * 
     * Expected format: Map of feature names to boolean flags
     * ```json
     * {
     *   "smart_routing_enabled": true,
     *   "fx_conversion_enabled": true,
     *   "idempotency_enabled": true,
     *   "health_checks_enabled": true
     * }
     * ```
     */
    const val FEATURE_FLAGS = "feature_flags"
    
    // ========================================
    // System Configuration
    // ========================================
    
    /**
     * Idempotency configuration including TTL and cache settings.
     * 
     * Expected format: Map of idempotency settings
     * ```json
     * {
     *   "ttl_seconds": 3600,
     *   "max_cache_size": 10000,
     *   "cleanup_interval_seconds": 300
     * }
     * ```
     */
    const val IDEMPOTENCY_CONFIG = "idempotency_config"
    
    /**
     * Health check configuration for providers and system components.
     * 
     * Expected format: Map of health check settings
     * ```json
     * {
     *   "check_interval_seconds": 30,
     *   "timeout_seconds": 5,
     *   "failure_threshold": 3,
     *   "recovery_threshold": 2
     * }
     * ```
     */
    const val HEALTH_CHECK_CONFIG = "health_check_config"
    
    // ========================================
    // Security Configuration
    // ========================================
    
    /**
     * API rate limiting configuration.
     * 
     * Expected format: Map of rate limiting settings
     * ```json
     * {
     *   "requests_per_minute": 1000,
     *   "burst_capacity": 100,
     *   "enabled": true
     * }
     * ```
     */
    const val RATE_LIMITING_CONFIG = "rate_limiting_config"
    
    // ========================================
    // Monitoring Configuration
    // ========================================
    
    /**
     * Metrics and monitoring configuration.
     * 
     * Expected format: Map of monitoring settings
     * ```json
     * {
     *   "metrics_enabled": true,
     *   "detailed_logging": false,
     *   "export_interval_seconds": 60
     * }
     * ```
     */
    const val MONITORING_CONFIG = "monitoring_config"
    
    // ========================================
    // Utility Methods
    // ========================================
    
    /**
     * Get all configuration keys as a list.
     * Useful for bulk operations or validation.
     */
    fun getAllKeys(): List<String> = listOf(
        PROVIDERS,
        ROUTING_RULES,
        ROUTING_STRATEGIES,
        ROUTING_WEIGHTS,
        FX_RATES,
        FEATURE_FLAGS,
        IDEMPOTENCY_CONFIG,
        HEALTH_CHECK_CONFIG,
        RATE_LIMITING_CONFIG,
        MONITORING_CONFIG
    )
    
    /**
     * Get routing-related configuration keys.
     */
    fun getRoutingKeys(): List<String> = listOf(
        ROUTING_RULES,
        ROUTING_STRATEGIES,
        ROUTING_WEIGHTS
    )
    
    /**
     * Get system configuration keys.
     */
    fun getSystemKeys(): List<String> = listOf(
        FEATURE_FLAGS,
        IDEMPOTENCY_CONFIG,
        HEALTH_CHECK_CONFIG,
        RATE_LIMITING_CONFIG,
        MONITORING_CONFIG
    )
    
    /**
     * Validate if a key is a known configuration key.
     */
    fun isValidKey(key: String): Boolean = key in getAllKeys()
    
    /**
     * Get the category of a configuration key.
     */
    fun getKeyCategory(key: String): String = when (key) {
        PROVIDERS -> "Provider"
        in getRoutingKeys() -> "Routing"
        FX_RATES -> "ForeignExchange"
        in getSystemKeys() -> "System"
        else -> "Unknown"
    }
}
