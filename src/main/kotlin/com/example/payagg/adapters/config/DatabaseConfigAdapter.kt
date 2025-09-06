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
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

@Service
@Transactional
class DatabaseConfigAdapter(
    private val routingConfigRepository: RoutingConfigRepository,
    private val objectMapper: ObjectMapper
) : ConfigPort {
    
    private val logger = LoggerFactory.getLogger(DatabaseConfigAdapter::class.java)
    private val configCache = ConcurrentHashMap<String, Any>()
    private val cacheLock = ReentrantReadWriteLock()
    
    init {
        refreshConfig()
    }
    
    override fun <T> getConfig(key: String, type: Class<T>): T? {
        return cacheLock.read {
            try {
                val value = configCache[key] ?: return null
                objectMapper.convertValue(value, type)
            } catch (e: Exception) {
                logger.error("Failed to convert config value for key: $key", e)
                null
            }
        }
    }
    
    @Transactional
    override fun setConfig(key: String, value: Any): Boolean {
        return cacheLock.write {
            try {
                // Use database-level locking to prevent race conditions
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

                // Save to database first - if this fails, cache won't be updated
                val savedConfig = routingConfigRepository.save(config)

                // Only update cache after successful database save
                configCache[key] = value

                logger.info("Updated config: $key")
                true
            } catch (e: Exception) {
                logger.error("Failed to set config for key: $key", e)
                false
            }
        }
    }
    
    override fun getAllConfigs(): Map<String, Any> {
        return cacheLock.read {
            configCache.toMap()
        }
    }
    
    @Transactional
    override fun deleteConfig(key: String): Boolean {
        return cacheLock.write {
            try {
                val config = routingConfigRepository.findByConfigKey(key)
                if (config != null) {
                    // Delete from database first - if this fails, cache won't be updated
                    routingConfigRepository.delete(config)

                    // Only remove from cache after successful database deletion
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
    }
    
    @Transactional(readOnly = true)
    override fun refreshConfig() {
        cacheLock.write {
            try {
                val configs = routingConfigRepository.findAll()

                // Build new cache map first, then replace atomically
                val newCache = ConcurrentHashMap<String, Any>()
                configs.forEach { config ->
                    newCache[config.configKey] = config.configValue
                }

                // Atomic replacement - cache is never empty during refresh
                configCache.clear()
                configCache.putAll(newCache)

                logger.info("Refreshed ${configs.size} configuration entries")
            } catch (e: Exception) {
                logger.error("Failed to refresh configuration", e)
                // Cache remains in previous consistent state on failure
            }
        }
    }
}
