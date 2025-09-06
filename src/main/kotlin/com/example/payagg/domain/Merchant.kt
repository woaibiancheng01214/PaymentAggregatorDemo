package com.example.payagg.domain

import jakarta.persistence.*
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.Instant
import java.util.*

@Entity
@Table(name = "merchants")
@EntityListeners(AuditingEntityListener::class)
data class Merchant(
    @Id
    val id: UUID = UUID.randomUUID(),
    
    @Column(name = "business_name", nullable = false)
    val businessName: String,
    
    @Column(name = "country", nullable = false, length = 2)
    val country: String,
    
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
        businessName = "",
        country = ""
    )
}
