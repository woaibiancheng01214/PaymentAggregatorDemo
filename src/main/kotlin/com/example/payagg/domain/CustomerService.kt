package com.example.payagg.domain

import com.example.payagg.ports.CustomerRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.*

@Service
@Transactional
class CustomerService(
    private val customerRepository: CustomerRepository
) {
    
    private val logger = LoggerFactory.getLogger(CustomerService::class.java)
    
    fun createCustomer(
        requestId: UUID?,
        email: String,
        name: String,
        country: String,
        address: Address?
    ): Customer {
        // Check for duplicate request ID
        if (requestId != null) {
            customerRepository.findByRequestId(requestId)?.let {
                throw IllegalArgumentException("Customer with request ID $requestId already exists")
            }
        }
        
        // Check for duplicate email
        customerRepository.findByEmail(email)?.let {
            throw IllegalArgumentException("Customer with email $email already exists")
        }
        
        val customer = Customer(
            requestId = requestId,
            email = email,
            name = name,
            country = country.uppercase(),
            address = address ?: Address.empty()
        )
        
        val savedCustomer = customerRepository.save(customer)
        logger.info("Created customer ${savedCustomer.id} with email '${savedCustomer.email}'")
        
        return savedCustomer
    }
    
    fun getCustomer(customerId: UUID): Customer? {
        return customerRepository.findById(customerId).orElse(null)
    }
    
    fun updateCustomer(
        customerId: UUID,
        email: String?,
        name: String?,
        country: String?,
        address: Address?
    ): Customer {
        val customer = customerRepository.findById(customerId)
            .orElseThrow { IllegalArgumentException("Customer not found: $customerId") }
        
        // Check for duplicate email if changing
        if (email != null && email != customer.email) {
            customerRepository.findByEmail(email)?.let {
                throw IllegalArgumentException("Customer with email $email already exists")
            }
        }
        
        val updatedCustomer = customer.copy(
            email = email ?: customer.email,
            name = name ?: customer.name,
            country = country?.uppercase() ?: customer.country,
            address = address ?: customer.address,
            lastModifiedAt = Instant.now()
        )
        
        return customerRepository.save(updatedCustomer)
    }
    
    fun getCustomerByEmail(email: String): Customer? {
        return customerRepository.findByEmail(email)
    }
    
    fun getAllCustomers(): List<Customer> {
        return customerRepository.findAll()
    }
}
