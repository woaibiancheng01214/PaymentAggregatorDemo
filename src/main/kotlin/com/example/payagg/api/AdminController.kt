package com.example.payagg.api

import com.example.payagg.ports.ConfigPort
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/admin")
@Tag(name = "Admin", description = "Administrative operations")
class AdminController(
    private val configPort: ConfigPort
) {
    
    @GetMapping("/config")
    @Operation(summary = "Get all configuration")
    fun getAllConfig(): ResponseEntity<Map<String, Any>> {
        return ResponseEntity.ok(configPort.getAllConfigs())
    }
    
    @GetMapping("/config/{key}")
    @Operation(summary = "Get configuration by key")
    fun getConfig(@PathVariable key: String): ResponseEntity<Any> {
        val value = configPort.getConfig(key, Any::class.java)
            ?: return ResponseEntity.notFound().build()
        
        return ResponseEntity.ok(value)
    }
    
    @PutMapping("/config/{key}")
    @Operation(summary = "Set configuration value")
    fun setConfig(
        @PathVariable key: String,
        @RequestBody value: Any
    ): ResponseEntity<Map<String, String>> {
        val success = configPort.setConfig(key, value)
        
        return if (success) {
            ResponseEntity.ok(mapOf("message" to "Configuration updated successfully"))
        } else {
            ResponseEntity.internalServerError().body(mapOf("error" to "Failed to update configuration"))
        }
    }
    
    @DeleteMapping("/config/{key}")
    @Operation(summary = "Delete configuration")
    fun deleteConfig(@PathVariable key: String): ResponseEntity<Map<String, String>> {
        val success = configPort.deleteConfig(key)
        
        return if (success) {
            ResponseEntity.ok(mapOf("message" to "Configuration deleted successfully"))
        } else {
            ResponseEntity.notFound().build()
        }
    }
    
    @PostMapping("/config/refresh")
    @Operation(summary = "Refresh configuration from database")
    fun refreshConfig(): ResponseEntity<Map<String, String>> {
        configPort.refreshConfig()
        return ResponseEntity.ok(mapOf("message" to "Configuration refreshed successfully"))
    }
}
