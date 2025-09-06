package com.example.payagg.domain

data class FailureDetails(
    val code: String,
    val message: String,
    val providerCode: String? = null,
    val providerMessage: String? = null
)
