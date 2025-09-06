package com.example.payagg.domain

data class RouteDecision(
    val candidates: List<String>,
    val strategyUsed: List<String>,
    val selectedProvider: String? = null,
    val reason: String? = null
)
