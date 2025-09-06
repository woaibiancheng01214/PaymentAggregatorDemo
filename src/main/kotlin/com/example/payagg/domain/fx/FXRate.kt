package com.example.payagg.domain.fx

import java.math.BigDecimal
import java.time.Instant
import java.util.*

data class FXRate(
    val fromCurrency: Currency,
    val toCurrency: Currency,
    val rate: BigDecimal,
    val timestamp: Instant = Instant.now()
) {
    fun inverse(): FXRate {
        return FXRate(
            fromCurrency = toCurrency,
            toCurrency = fromCurrency,
            rate = BigDecimal.ONE.divide(rate, 6, BigDecimal.ROUND_HALF_UP),
            timestamp = timestamp
        )
    }
    
    fun isExpired(maxAge: java.time.Duration): Boolean {
        return Instant.now().isAfter(timestamp.plus(maxAge))
    }
}
