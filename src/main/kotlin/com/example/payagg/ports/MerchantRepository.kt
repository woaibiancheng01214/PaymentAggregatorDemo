package com.example.payagg.ports

import com.example.payagg.domain.Merchant
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface MerchantRepository : JpaRepository<Merchant, UUID> {
    fun findByCountry(country: String): List<Merchant>
}
