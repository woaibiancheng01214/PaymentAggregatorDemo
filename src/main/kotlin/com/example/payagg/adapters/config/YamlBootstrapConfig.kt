package com.example.payagg.adapters.config

import com.example.payagg.ports.ConfigPort
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Component

@Component
class YamlBootstrapConfig(
    private val configPort: ConfigPort
) {
    
    private val logger = LoggerFactory.getLogger(YamlBootstrapConfig::class.java)
    private val yamlMapper = ObjectMapper(YAMLFactory())
    
    @EventListener(ApplicationReadyEvent::class)
    fun bootstrapFromYaml() {
        try {
            val resource = ClassPathResource("application-routing.yml")
            if (!resource.exists()) {
                logger.warn("application-routing.yml not found, skipping bootstrap")
                return
            }
            
            val yamlContent = yamlMapper.readValue(resource.inputStream, Map::class.java) as Map<String, Any>
            
            // Bootstrap providers configuration
            yamlContent["providers"]?.let { providers ->
                configPort.setConfig("providers", providers)
                logger.info("Bootstrapped providers configuration")
            }
            
            // Bootstrap routing configuration
            yamlContent["routing"]?.let { routing ->
                val routingMap = routing as Map<String, Any>
                
                routingMap["rules"]?.let { rules ->
                    configPort.setConfig("routing_rules", rules)
                }
                
                routingMap["strategies"]?.let { strategies ->
                    configPort.setConfig("routing_strategies", strategies)
                }
                
                routingMap["weights"]?.let { weights ->
                    configPort.setConfig("routing_weights", weights)
                }
                
                logger.info("Bootstrapped routing configuration")
            }
            
            // Bootstrap FX configuration
            yamlContent["fx"]?.let { fx ->
                val fxMap = fx as Map<String, Any>
                fxMap["rates"]?.let { rates ->
                    configPort.setConfig("fx_rates", rates)
                    logger.info("Bootstrapped FX rates configuration")
                }
            }
            
            logger.info("Configuration bootstrap completed successfully")
            
        } catch (e: Exception) {
            logger.error("Failed to bootstrap configuration from YAML", e)
        }
    }
}
