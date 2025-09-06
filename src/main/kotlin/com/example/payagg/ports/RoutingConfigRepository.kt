package com.example.payagg.ports

import com.example.payagg.domain.RoutingConfig
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface RoutingConfigRepository : JpaRepository<RoutingConfig, UUID> {
    fun findByConfigKey(configKey: String): RoutingConfig?
    fun findAllByConfigKeyIn(configKeys: List<String>): List<RoutingConfig>
}
