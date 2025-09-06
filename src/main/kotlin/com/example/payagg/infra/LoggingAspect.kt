package com.example.payagg.infra

import com.fasterxml.jackson.databind.ObjectMapper
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.stereotype.Component
import java.util.*

@Aspect
@Component
class LoggingAspect(
    private val objectMapper: ObjectMapper
) {
    
    private val logger = LoggerFactory.getLogger(LoggingAspect::class.java)
    
    @Around("@annotation(org.springframework.web.bind.annotation.PostMapping) || " +
            "@annotation(org.springframework.web.bind.annotation.PatchMapping) || " +
            "@annotation(org.springframework.web.bind.annotation.PutMapping)")
    fun logApiCalls(joinPoint: ProceedingJoinPoint): Any? {
        val requestId = UUID.randomUUID().toString()
        MDC.put("requestId", requestId)
        
        val methodName = "${joinPoint.signature.declaringTypeName}.${joinPoint.signature.name}"
        val startTime = System.currentTimeMillis()
        
        try {
            logger.info("API_CALL_START: method={}, requestId={}", methodName, requestId)
            
            val result = joinPoint.proceed()
            val duration = System.currentTimeMillis() - startTime
            
            logger.info("API_CALL_SUCCESS: method={}, requestId={}, duration={}ms", 
                methodName, requestId, duration)
            
            return result
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            logger.error("API_CALL_ERROR: method={}, requestId={}, duration={}ms, error={}", 
                methodName, requestId, duration, e.message, e)
            throw e
        } finally {
            MDC.remove("requestId")
        }
    }
    
    @Around("execution(* com.example.payagg.domain.routing.RoutingEngine.route(..))")
    fun logRoutingDecisions(joinPoint: ProceedingJoinPoint): Any? {
        val routingId = UUID.randomUUID().toString()
        MDC.put("routingId", routingId)
        
        try {
            val args = joinPoint.args
            logger.info("ROUTING_START: routingId={}, context={}", 
                routingId, objectMapper.writeValueAsString(args[0]))
            
            val result = joinPoint.proceed()
            
            logger.info("ROUTING_COMPLETE: routingId={}, decision={}", 
                routingId, objectMapper.writeValueAsString(result))
            
            return result
        } catch (e: Exception) {
            logger.error("ROUTING_ERROR: routingId={}, error={}", routingId, e.message, e)
            throw e
        } finally {
            MDC.remove("routingId")
        }
    }
    
    @Around("execution(* com.example.payagg.ports.PaymentProvider.authorize(..))")
    fun logProviderCalls(joinPoint: ProceedingJoinPoint): Any? {
        val providerCallId = UUID.randomUUID().toString()
        MDC.put("providerCallId", providerCallId)
        
        val provider = joinPoint.target
        val providerName = provider.javaClass.simpleName
        
        try {
            logger.info("PROVIDER_CALL_START: provider={}, callId={}", providerName, providerCallId)
            
            val startTime = System.currentTimeMillis()
            val result = joinPoint.proceed()
            val duration = System.currentTimeMillis() - startTime
            
            logger.info("PROVIDER_CALL_SUCCESS: provider={}, callId={}, duration={}ms, result={}", 
                providerName, providerCallId, duration, objectMapper.writeValueAsString(result))
            
            return result
        } catch (e: Exception) {
            logger.error("PROVIDER_CALL_ERROR: provider={}, callId={}, error={}", 
                providerName, providerCallId, e.message, e)
            throw e
        } finally {
            MDC.remove("providerCallId")
        }
    }
}
