package com.example.payagg.domain

import io.hypersistence.utils.hibernate.type.json.JsonType
import jakarta.persistence.*
import org.hibernate.annotations.Type
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.Instant
import java.util.*

@Entity
@Table(name = "routing_config")
@EntityListeners(AuditingEntityListener::class)
data class RoutingConfig(
    @Id
    val id: UUID = UUID.randomUUID(),
    
    @Column(name = "config_key", nullable = false, unique = true)
    val configKey: String,
    
    @Type(JsonType::class)
    @Column(name = "config_value", nullable = false, columnDefinition = "jsonb")
    val configValue: Any,
    
    @Version
    val version: Long = 0,
    
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),
    
    @LastModifiedDate
    @Column(name = "last_modified_at", nullable = false)
    val lastModifiedAt: Instant = Instant.now()
) {
    // JPA requires no-arg constructor
    constructor() : this(
        id = UUID.randomUUID(),
        configKey = "",
        configValue = emptyMap<String, Any>()
    )
}
