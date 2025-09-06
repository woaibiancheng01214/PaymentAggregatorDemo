package com.example.payagg.domain

import jakarta.persistence.Embeddable

@Embeddable
data class Address(
    val line1: String?,
    val line2: String?,
    val city: String?,
    val state: String?,
    val country: String?,
    val postalCode: String?
) {
    companion object {
        fun empty() = Address(null, null, null, null, null, null)
    }
}
