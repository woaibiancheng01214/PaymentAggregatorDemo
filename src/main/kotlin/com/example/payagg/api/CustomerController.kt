package com.example.payagg.api

import com.example.payagg.api.dto.*
import com.example.payagg.domain.Address
import com.example.payagg.domain.Customer
import com.example.payagg.domain.CustomerService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/customers")
@Tag(name = "Customers", description = "Customer management operations")
class CustomerController(
    private val customerService: CustomerService
) {
    
    @PostMapping
    @Operation(
        summary = "Create a new customer",
        description = "Creates a new customer. Requires X-Request-Id header for tracking and idempotency."
    )
    fun createCustomer(
        @Valid @RequestBody request: CreateCustomerRequest,
        @Parameter(description = "Unique request identifier for tracking and idempotency", required = true)
        @RequestHeader("X-Request-Id") requestId: String?
    ): ResponseEntity<CustomerResponse> {
        val customer = customerService.createCustomer(
            requestId = requestId?.let { UUID.fromString(it) },
            email = request.email,
            name = request.name,
            country = request.country,
            address = request.address?.toDomain()
        )

        return ResponseEntity.status(HttpStatus.CREATED).body(customer.toResponse())
    }
    
    @GetMapping("/{id}")
    @Operation(summary = "Get customer by ID")
    fun getCustomer(@PathVariable id: UUID): ResponseEntity<CustomerResponse> {
        val customer = customerService.getCustomer(id)
            ?: return ResponseEntity.notFound().build()
        
        return ResponseEntity.ok(customer.toResponse())
    }
    
    @PatchMapping("/{id}")
    @Operation(summary = "Update customer")
    fun updateCustomer(
        @PathVariable id: UUID,
        @Valid @RequestBody request: UpdateCustomerRequest
    ): ResponseEntity<CustomerResponse> {
        val customer = customerService.updateCustomer(
            customerId = id,
            email = request.email,
            name = request.name,
            country = request.country,
            address = request.address?.toDomain()
        )
        
        return ResponseEntity.ok(customer.toResponse())
    }
    
    @GetMapping
    @Operation(summary = "Get all customers")
    fun getAllCustomers(@RequestParam(required = false) email: String?): ResponseEntity<List<CustomerResponse>> {
        val customers = if (email != null) {
            listOfNotNull(customerService.getCustomerByEmail(email))
        } else {
            customerService.getAllCustomers()
        }
        
        return ResponseEntity.ok(customers.map { it.toResponse() })
    }
    
    private fun Customer.toResponse(): CustomerResponse {
        return CustomerResponse(
            id = id,
            requestId = requestId,
            email = email,
            name = name,
            country = country,
            address = address.toDto(),
            createdAt = createdAt,
            lastModifiedAt = lastModifiedAt
        )
    }
    
    private fun AddressDto.toDomain(): Address {
        return Address(
            line1 = line1,
            line2 = line2,
            city = city,
            state = state,
            country = country,
            postalCode = postalCode
        )
    }
    
    private fun Address.toDto(): AddressDto? {
        if (line1 == null && line2 == null && city == null && state == null && country == null && postalCode == null) {
            return null
        }
        return AddressDto(
            line1 = line1,
            line2 = line2,
            city = city,
            state = state,
            country = country,
            postalCode = postalCode
        )
    }
}
