package com.example.payagg.domain.fx

import com.example.payagg.ports.FXConversionResult
import com.example.payagg.ports.FXPort
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.util.*

@Service
class FXService(
    private val fxPort: FXPort
) {
    
    private val logger = LoggerFactory.getLogger(FXService::class.java)
    
    fun convertForProvider(
        merchantAmount: BigDecimal,
        merchantCurrency: Currency,
        providerSettlementCurrency: Currency
    ): FXConversionResult {
        logger.debug("Converting $merchantAmount ${merchantCurrency.currencyCode} to ${providerSettlementCurrency.currencyCode}")
        
        return fxPort.convert(merchantAmount, merchantCurrency, providerSettlementCurrency)
    }
    
    fun isConversionRequired(merchantCurrency: Currency, providerCurrency: Currency): Boolean {
        return merchantCurrency != providerCurrency
    }
    
    fun getSupportedCurrencies(): Set<Currency> {
        return fxPort.getSupportedCurrencies()
    }
    
    fun getExchangeRate(fromCurrency: Currency, toCurrency: Currency): BigDecimal? {
        return fxPort.getRate(fromCurrency, toCurrency)
    }
    
    fun validateCurrencySupport(currency: Currency): Boolean {
        return currency in getSupportedCurrencies()
    }
}
