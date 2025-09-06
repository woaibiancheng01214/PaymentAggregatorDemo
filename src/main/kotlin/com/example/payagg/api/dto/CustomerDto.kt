package com.example.payagg.api.dto

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.Instant
import java.util.*

data class CreateCustomerRequest(
    @JsonProperty("request_id")
    val requestId: UUID?,
    
    @field:NotBlank
    @field:Email
    val email: String,
    
    @field:NotBlank
    val name: String,
    
    @field:NotBlank
    @field:Size(min = 2, max = 2)
    val country: String,
    
    val address: AddressDto?
)

data class UpdateCustomerRequest(
    val email: String?,
    val name: String?,
    val country: String?,
    val address: AddressDto?
)

data class CustomerResponse(
    val id: UUID,
    @JsonProperty("request_id")
    val requestId: UUID?,
    val email: String,
    val name: String,
    val country: String,
    val address: AddressDto?,
    @JsonProperty("created_at")
    val createdAt: Instant,
    @JsonProperty("last_modified_at")
    val lastModifiedAt: Instant
)

data class AddressDto(
    val line1: String?,
    val line2: String?,
    val city: String?,
    val state: String?,
    val country: String?,
    @JsonProperty("postal_code")
    val postalCode: String?
)
