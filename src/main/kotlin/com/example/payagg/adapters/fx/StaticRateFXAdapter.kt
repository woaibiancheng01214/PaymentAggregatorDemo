package com.example.payagg.adapters.fx

import com.example.payagg.domain.fx.FXRate
import com.example.payagg.ports.FXConversionResult
import com.example.payagg.ports.FXPort
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.util.*
import java.util.concurrent.ConcurrentHashMap

@Service
class StaticRateFXAdapter : FXPort {
    
    private val logger = LoggerFactory.getLogger(StaticRateFXAdapter::class.java)
    private val rates = ConcurrentHashMap<String, FXRate>()
    
    init {
        loadStaticRates()
    }
    
    override fun convert(amount: BigDecimal, fromCurrency: Currency, toCurrency: Currency): FXConversionResult {
        if (fromCurrency == toCurrency) {
            return FXConversionResult(
                originalAmount = amount,
                convertedAmount = amount,
                fromCurrency = fromCurrency,
                toCurrency = toCurrency,
                rate = BigDecimal.ONE,
                timestamp = Instant.now()
            )
        }
        
        val rate = getRate(fromCurrency, toCurrency)
            ?: throw IllegalArgumentException("No exchange rate available for ${fromCurrency.currencyCode} to ${toCurrency.currencyCode}")
        
        val convertedAmount = amount.multiply(rate).setScale(2, RoundingMode.HALF_UP)
        
        logger.debug("FX conversion: $amount ${fromCurrency.currencyCode} = $convertedAmount ${toCurrency.currencyCode} (rate: $rate)")
        
        return FXConversionResult(
            originalAmount = amount,
            convertedAmount = convertedAmount,
            fromCurrency = fromCurrency,
            toCurrency = toCurrency,
            rate = rate,
            timestamp = Instant.now()
        )
    }
    
    override fun getRate(fromCurrency: Currency, toCurrency: Currency): BigDecimal? {
        if (fromCurrency == toCurrency) {
            return BigDecimal.ONE
        }
        
        val key = "${fromCurrency.currencyCode}-${toCurrency.currencyCode}"
        val reverseKey = "${toCurrency.currencyCode}-${fromCurrency.currencyCode}"
        
        // Try direct rate
        rates[key]?.let { return it.rate }
        
        // Try inverse rate
        rates[reverseKey]?.let { 
            return BigDecimal.ONE.divide(it.rate, 6, RoundingMode.HALF_UP)
        }
        
        // Try cross-currency conversion via USD
        if (fromCurrency.currencyCode != "USD" && toCurrency.currencyCode != "USD") {
            val usd = Currency.getInstance("USD")
            val fromUsdRate = getRate(fromCurrency, usd)
            val toUsdRate = getRate(usd, toCurrency)
            
            if (fromUsdRate != null && toUsdRate != null) {
                return fromUsdRate.multiply(toUsdRate).setScale(6, RoundingMode.HALF_UP)
            }
        }
        
        return null
    }
    
    override fun getSupportedCurrencies(): Set<Currency> {
        val currencies = mutableSetOf<Currency>()
        rates.values.forEach { rate ->
            currencies.add(rate.fromCurrency)
            currencies.add(rate.toCurrency)
        }
        return currencies
    }
    
    private fun loadStaticRates() {
        // Load static rates as defined in the project plan
        val staticRates = listOf(
            Triple("USD", "EUR", BigDecimal("0.90")),
            Triple("EUR", "USD", BigDecimal("1.11")),
            Triple("USD", "GBP", BigDecimal("0.78")),
            Triple("GBP", "USD", BigDecimal("1.28")),
            Triple("EUR", "GBP", BigDecimal("0.87")),
            Triple("GBP", "EUR", BigDecimal("1.15"))
        )
        
        staticRates.forEach { (from, to, rate) ->
            val fromCurrency = Currency.getInstance(from)
            val toCurrency = Currency.getInstance(to)
            val key = "$from-$to"
            
            rates[key] = FXRate(
                fromCurrency = fromCurrency,
                toCurrency = toCurrency,
                rate = rate,
                timestamp = Instant.now()
            )
        }
        
        logger.info("Loaded ${rates.size} static FX rates: ${rates.keys}")
    }
    
    // Admin method to update rates at runtime
    fun updateRate(fromCurrency: Currency, toCurrency: Currency, rate: BigDecimal) {
        val key = "${fromCurrency.currencyCode}-${toCurrency.currencyCode}"
        rates[key] = FXRate(
            fromCurrency = fromCurrency,
            toCurrency = toCurrency,
            rate = rate,
            timestamp = Instant.now()
        )
        logger.info("Updated FX rate: $key = $rate")
    }
    
    fun getAllRates(): Map<String, FXRate> {
        return rates.toMap()
    }
}
