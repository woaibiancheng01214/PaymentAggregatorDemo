package com.example.payagg.api

import com.example.payagg.api.dto.*
import com.example.payagg.domain.Merchant
import com.example.payagg.domain.MerchantService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/merchants")
@Tag(name = "Merchants", description = "Merchant management operations")
class MerchantController(
    private val merchantService: MerchantService
) {
    
    @PostMapping
    @Operation(summary = "Create a new merchant")
    fun createMerchant(@Valid @RequestBody request: CreateMerchantRequest): ResponseEntity<MerchantResponse> {
        val merchant = merchantService.createMerchant(
            businessName = request.businessName,
            country = request.country
        )
        
        return ResponseEntity.status(HttpStatus.CREATED).body(merchant.toResponse())
    }
    
    @GetMapping("/{id}")
    @Operation(summary = "Get merchant by ID")
    fun getMerchant(@PathVariable id: UUID): ResponseEntity<MerchantResponse> {
        val merchant = merchantService.getMerchant(id)
            ?: return ResponseEntity.notFound().build()
        
        return ResponseEntity.ok(merchant.toResponse())
    }
    
    @PatchMapping("/{id}")
    @Operation(summary = "Update merchant")
    fun updateMerchant(
        @PathVariable id: UUID,
        @Valid @RequestBody request: UpdateMerchantRequest
    ): ResponseEntity<MerchantResponse> {
        val merchant = merchantService.updateMerchant(
            merchantId = id,
            businessName = request.businessName,
            country = request.country
        )
        
        return ResponseEntity.ok(merchant.toResponse())
    }
    
    @GetMapping
    @Operation(summary = "Get all merchants")
    fun getAllMerchants(@RequestParam(required = false) country: String?): ResponseEntity<List<MerchantResponse>> {
        val merchants = if (country != null) {
            merchantService.getMerchantsByCountry(country)
        } else {
            merchantService.getAllMerchants()
        }
        
        return ResponseEntity.ok(merchants.map { it.toResponse() })
    }
    
    private fun Merchant.toResponse(): MerchantResponse {
        return MerchantResponse(
            id = id,
            businessName = businessName,
            country = country,
            createdAt = createdAt,
            lastModifiedAt = lastModifiedAt
        )
    }
}
