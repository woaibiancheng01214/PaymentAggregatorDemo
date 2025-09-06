package com.example.payagg.infra

import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class WebConfig(
    private val idempotencyInterceptor: IdempotencyInterceptor
) : WebMvcConfigurer {
    
    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(idempotencyInterceptor)
            .addPathPatterns("/payments/**", "/merchants/**", "/customers/**")
    }
}
