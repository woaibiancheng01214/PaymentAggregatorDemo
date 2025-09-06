package com.example.payagg.domain

import jakarta.persistence.*
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.Instant
import java.util.*

@Entity
@Table(name = "customers")
@EntityListeners(AuditingEntityListener::class)
data class Customer(
    @Id
    val id: UUID = UUID.randomUUID(),
    
    @Column(name = "request_id")
    val requestId: UUID?,
    
    @Column(name = "email", nullable = false)
    val email: String,
    
    @Column(name = "name", nullable = false)
    val name: String,
    
    @Column(name = "country", nullable = false, length = 2)
    val country: String,
    
    @Embedded
    @AttributeOverrides(
        AttributeOverride(name = "line1", column = Column(name = "address_line1")),
        AttributeOverride(name = "line2", column = Column(name = "address_line2")),
        AttributeOverride(name = "city", column = Column(name = "address_city")),
        AttributeOverride(name = "state", column = Column(name = "address_state")),
        AttributeOverride(name = "country", column = Column(name = "address_country")),
        AttributeOverride(name = "postalCode", column = Column(name = "address_postal_code"))
    )
    val address: Address = Address.empty(),
    
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
        requestId = null,
        email = "",
        name = "",
        country = ""
    )
}
