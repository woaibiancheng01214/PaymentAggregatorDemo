package com.example.payagg.infra

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class MetricsConfig {
    
    @Bean
    fun paymentCreatedCounter(meterRegistry: MeterRegistry): Counter {
        return Counter.builder("payments.created")
            .description("Number of payments created")
            .register(meterRegistry)
    }
    
    @Bean
    fun paymentConfirmedCounter(meterRegistry: MeterRegistry): Counter {
        return Counter.builder("payments.confirmed")
            .description("Number of payments confirmed")
            .register(meterRegistry)
    }
    
    @Bean
    fun paymentFailedCounter(meterRegistry: MeterRegistry): Counter {
        return Counter.builder("payments.failed")
            .description("Number of payments failed")
            .register(meterRegistry)
    }
    
    @Bean
    fun routingTimer(meterRegistry: MeterRegistry): Timer {
        return Timer.builder("routing.duration")
            .description("Time taken for routing decisions")
            .register(meterRegistry)
    }
    
    @Bean
    fun providerCallTimer(meterRegistry: MeterRegistry): Timer {
        return Timer.builder("provider.call.duration")
            .description("Time taken for provider calls")
            .register(meterRegistry)
    }
}
