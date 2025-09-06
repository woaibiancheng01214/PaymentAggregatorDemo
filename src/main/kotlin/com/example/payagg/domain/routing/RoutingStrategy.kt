package com.example.payagg.domain.routing

import com.example.payagg.ports.PaymentProvider
import java.math.BigDecimal

interface RoutingStrategy {
    val name: String
    fun route(context: RoutingContext, candidates: List<PaymentProvider>): RoutingResult
}

data class RoutingContext(
    val amount: BigDecimal,
    val currency: java.util.Currency,
    val country: String,
    val cardNetwork: com.example.payagg.domain.CardNetwork,
    val binRange: String? = null,
    val merchantId: String,
    val metadata: Map<String, Any> = emptyMap()
)

data class RoutingResult(
    val providers: List<PaymentProvider>,
    val strategy: String,
    val reason: String,
    val metadata: Map<String, Any> = emptyMap()
)

enum class RoutingMode {
    STRICT,     // Must use specified providers only
    PREFERRED,  // Prefer specified providers but allow fallback
    EXCLUDED    // Exclude specified providers
}
