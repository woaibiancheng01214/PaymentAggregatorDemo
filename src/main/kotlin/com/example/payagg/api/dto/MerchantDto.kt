package com.example.payagg.api.dto

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.Instant
import java.util.*

data class CreateMerchantRequest(
    @field:NotBlank
    @JsonProperty("business_name")
    val businessName: String,
    
    @field:NotBlank
    @field:Size(min = 2, max = 2)
    val country: String
)

data class UpdateMerchantRequest(
    @JsonProperty("business_name")
    val businessName: String?,
    val country: String?
)

data class MerchantResponse(
    val id: UUID,
    @JsonProperty("business_name")
    val businessName: String,
    val country: String,
    @JsonProperty("created_at")
    val createdAt: Instant,
    @JsonProperty("last_modified_at")
    val lastModifiedAt: Instant
)
