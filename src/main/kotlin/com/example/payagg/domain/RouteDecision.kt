package com.example.payagg.domain

import com.example.payagg.domain.routing.RoutingMetadataContainer

data class RouteDecision(
    val candidates: List<String>,
    val strategyUsed: List<String>,
    val selectedProvider: String? = null,
    val reason: String? = null,
    val metadata: RoutingMetadataContainer = RoutingMetadataContainer(emptyList())
)
