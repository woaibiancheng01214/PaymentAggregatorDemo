package com.example.payagg.infra

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.info.License
import io.swagger.v3.oas.models.servers.Server
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfig {
    
    @Bean
    fun customOpenAPI(): OpenAPI {
        return OpenAPI()
            .info(
                Info()
                    .title("Payment Aggregator API")
                    .description("A demo payment aggregator with smart routing capabilities")
                    .version("1.0.0")
                    .contact(
                        Contact()
                            .name("Payment Aggregator Team")
                            .email("support@payagg.example.com")
                    )
                    .license(
                        License()
                            .name("MIT License")
                            .url("https://opensource.org/licenses/MIT")
                    )
            )
            .servers(
                listOf(
                    Server()
                        .url("http://localhost:8080")
                        .description("Local development server"),
                    Server()
                        .url("https://api.payagg.example.com")
                        .description("Production server")
                )
            )
    }
}
