package com.example.payagg.adapters.config

import com.example.payagg.domain.RoutingConfig
import com.example.payagg.ports.ConfigPort
import com.example.payagg.ports.RoutingConfigRepository
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

@Service
@Transactional
class DatabaseConfigAdapter(
    private val routingConfigRepository: RoutingConfigRepository,
    private val objectMapper: ObjectMapper
) : ConfigPort {
    
    private val logger = LoggerFactory.getLogger(DatabaseConfigAdapter::class.java)
    private val configCache = ConcurrentHashMap<String, Any>()
    
    init {
        refreshConfig()
    }
    
    override fun <T> getConfig(key: String, type: Class<T>): T? {
        return try {
            val value = configCache[key] ?: return null
            objectMapper.convertValue(value, type)
        } catch (e: Exception) {
            logger.error("Failed to convert config value for key: $key", e)
            null
        }
    }
    
    override fun setConfig(key: String, value: Any): Boolean {
        return try {
            val existingConfig = routingConfigRepository.findByConfigKey(key)
            
            val config = if (existingConfig != null) {
                existingConfig.copy(
                    configValue = value,
                    lastModifiedAt = Instant.now()
                )
            } else {
                RoutingConfig(
                    configKey = key,
                    configValue = value
                )
            }
            
            routingConfigRepository.save(config)
            configCache[key] = value
            
            logger.info("Updated config: $key")
            true
        } catch (e: Exception) {
            logger.error("Failed to set config for key: $key", e)
            false
        }
    }
    
    override fun getAllConfigs(): Map<String, Any> {
        return configCache.toMap()
    }
    
    override fun deleteConfig(key: String): Boolean {
        return try {
            val config = routingConfigRepository.findByConfigKey(key)
            if (config != null) {
                routingConfigRepository.delete(config)
                configCache.remove(key)
                logger.info("Deleted config: $key")
                true
            } else {
                false
            }
        } catch (e: Exception) {
            logger.error("Failed to delete config for key: $key", e)
            false
        }
    }
    
    override fun refreshConfig() {
        try {
            val configs = routingConfigRepository.findAll()
            configCache.clear()
            
            configs.forEach { config ->
                configCache[config.configKey] = config.configValue
            }
            
            logger.info("Refreshed ${configs.size} configuration entries")
        } catch (e: Exception) {
            logger.error("Failed to refresh configuration", e)
        }
    }
}
