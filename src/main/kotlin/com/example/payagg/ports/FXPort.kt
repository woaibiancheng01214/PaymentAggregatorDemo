package com.example.payagg.ports

import java.math.BigDecimal
import java.util.*

interface FXPort {
    fun convert(amount: BigDecimal, fromCurrency: Currency, toCurrency: Currency): FXConversionResult
    fun getRate(fromCurrency: Currency, toCurrency: Currency): BigDecimal?
    fun getSupportedCurrencies(): Set<Currency>
}

data class FXConversionResult(
    val originalAmount: BigDecimal,
    val convertedAmount: BigDecimal,
    val fromCurrency: Currency,
    val toCurrency: Currency,
    val rate: BigDecimal,
    val timestamp: java.time.Instant
)
