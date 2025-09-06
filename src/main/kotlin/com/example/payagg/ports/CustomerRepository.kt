package com.example.payagg.ports

import com.example.payagg.domain.Customer
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface CustomerRepository : JpaRepository<Customer, UUID> {
    fun findByEmail(email: String): Customer?
    fun findByRequestId(requestId: UUID): Customer?
}
