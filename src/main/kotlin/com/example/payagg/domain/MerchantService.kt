package com.example.payagg.domain

import com.example.payagg.ports.MerchantRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.*

@Service
@Transactional
class MerchantService(
    private val merchantRepository: MerchantRepository
) {
    
    private val logger = LoggerFactory.getLogger(MerchantService::class.java)
    
    fun createMerchant(businessName: String, country: String): Merchant {
        val merchant = Merchant(
            businessName = businessName,
            country = country.uppercase()
        )
        
        val savedMerchant = merchantRepository.save(merchant)
        logger.info("Created merchant ${savedMerchant.id} with business name '${savedMerchant.businessName}'")
        
        return savedMerchant
    }
    
    fun getMerchant(merchantId: UUID): Merchant? {
        return merchantRepository.findById(merchantId).orElse(null)
    }
    
    fun updateMerchant(
        merchantId: UUID,
        businessName: String?,
        country: String?
    ): Merchant {
        val merchant = merchantRepository.findById(merchantId)
            .orElseThrow { IllegalArgumentException("Merchant not found: $merchantId") }
        
        val updatedMerchant = merchant.copy(
            businessName = businessName ?: merchant.businessName,
            country = country?.uppercase() ?: merchant.country,
            lastModifiedAt = Instant.now()
        )
        
        return merchantRepository.save(updatedMerchant)
    }
    
    fun getAllMerchants(): List<Merchant> {
        return merchantRepository.findAll()
    }
    
    fun getMerchantsByCountry(country: String): List<Merchant> {
        return merchantRepository.findByCountry(country.uppercase())
    }
}
