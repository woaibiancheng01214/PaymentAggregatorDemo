package com.example.payagg.domain.payments

import com.example.payagg.domain.*
import com.example.payagg.domain.routing.RoutingEngine
import com.example.payagg.ports.*
import io.mockk.*
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.util.*

class PaymentServiceTest {
    
    private val paymentRepository = mockk<PaymentRepository>()
    private val paymentAttemptRepository = mockk<PaymentAttemptRepository>()
    private val merchantRepository = mockk<MerchantRepository>()
    private val customerRepository = mockk<CustomerRepository>()
    private val routingEngine = mockk<RoutingEngine>()
    private val providerRegistry = mockk<com.example.payagg.adapters.providers.PaymentProviderRegistry>()
    
    private lateinit var paymentService: PaymentService
    
    @BeforeEach
    fun setUp() {
        clearAllMocks()
        paymentService = PaymentService(
            paymentRepository,
            paymentAttemptRepository,
            merchantRepository,
            customerRepository,
            routingEngine,
            providerRegistry
        )
    }
    
    @Test
    fun `should create payment successfully`() {
        // Given
        val merchantId = UUID.randomUUID()
        val customerId = UUID.randomUUID()
        val requestId = UUID.randomUUID()
        val amount = BigDecimal("100.00")
        val currency = "USD"
        
        val merchant = Merchant(id = merchantId, businessName = "Test Merchant", country = "US")
        val customer = Customer(id = customerId, requestId = requestId, email = "test@example.com", name = "Test Customer", country = "US")
        
        every { merchantRepository.findById(merchantId) } returns Optional.of(merchant)
        every { customerRepository.findById(customerId) } returns Optional.of(customer)
        every { paymentRepository.findByRequestId(requestId) } returns null
        every { paymentRepository.save(any<Payment>()) } answers { firstArg() }
        
        // When
        val result = paymentService.createPayment(requestId, amount, currency, merchantId, customerId)
        
        // Then
        assertThat(result.amount).isEqualTo(amount)
        assertThat(result.currency).isEqualTo(currency)
        assertThat(result.merchantId).isEqualTo(merchantId)
        assertThat(result.customerId).isEqualTo(customerId)
        assertThat(result.status).isEqualTo(PaymentStatus.INIT)
        
        verify { paymentRepository.save(any<Payment>()) }
    }
    
    @Test
    fun `should throw exception when merchant not found`() {
        // Given
        val merchantId = UUID.randomUUID()
        val amount = BigDecimal("100.00")
        val currency = "USD"
        
        every { merchantRepository.findById(merchantId) } returns Optional.empty()
        
        // When & Then
        assertThrows<IllegalArgumentException> {
            paymentService.createPayment(null, amount, currency, merchantId, null)
        }
    }
    
    @Test
    fun `should throw exception for duplicate request ID`() {
        // Given
        val merchantId = UUID.randomUUID()
        val requestId = UUID.randomUUID()
        val amount = BigDecimal("100.00")
        val currency = "USD"
        
        val merchant = Merchant(id = merchantId, businessName = "Test Merchant", country = "US")
        val existingPayment = Payment(
            requestId = requestId,
            amount = amount,
            currency = currency,
            status = PaymentStatus.INIT,
            merchantId = merchantId,
            customerId = null
        )
        
        every { merchantRepository.findById(merchantId) } returns Optional.of(merchant)
        every { paymentRepository.findByRequestId(requestId) } returns existingPayment
        
        // When & Then
        assertThrows<IllegalArgumentException> {
            paymentService.createPayment(requestId, amount, currency, merchantId, null)
        }
    }
    
    @Test
    fun `should confirm payment successfully`() {
        // Given
        val paymentId = UUID.randomUUID()
        val merchantId = UUID.randomUUID()
        val payment = Payment(
            id = paymentId,
            requestId = null,
            amount = BigDecimal("100.00"),
            currency = "USD",
            status = PaymentStatus.INIT,
            merchantId = merchantId,
            customerId = null
        )
        val merchant = Merchant(id = merchantId, businessName = "Test Merchant", country = "US")
        val paymentMethod = mapOf(
            "type" to "card",
            "card" to mapOf(
                "number" to "4111111111111111",
                "expiry_month" to 12,
                "expiry_year" to 2025,
                "cvv" to "123",
                "holder_name" to "John Doe"
            )
        )
        
        val routeDecision = RouteDecision(
            candidates = listOf("StripeMock"),
            strategyUsed = listOf("ELIGIBILITY", "RULES"),
            selectedProvider = "StripeMock"
        )
        
        every { paymentRepository.findById(paymentId) } returns Optional.of(payment)
        every { merchantRepository.findById(merchantId) } returns Optional.of(merchant)
        every { routingEngine.route(any()) } returns routeDecision
        every { paymentAttemptRepository.save(any<PaymentAttempt>()) } answers { firstArg() }
        every { paymentRepository.save(any<Payment>()) } answers { firstArg() }
        
        // When
        val result = paymentService.confirmPayment(paymentId, paymentMethod)
        
        // Then
        assertThat(result.paymentId).isEqualTo(paymentId)
        assertThat(result.status).isEqualTo(PaymentAttemptStatus.RECEIVED)
        assertThat(result.routingMode).isEqualTo(RoutingMode.SMART)
        assertThat(result.routeDecision).isEqualTo(routeDecision)
        
        verify { paymentAttemptRepository.save(any<PaymentAttempt>()) }
        verify { paymentRepository.save(match<Payment> { it.status == PaymentStatus.PENDING }) }
    }
    
    @Test
    fun `should cancel payment successfully`() {
        // Given
        val paymentId = UUID.randomUUID()
        val payment = Payment(
            id = paymentId,
            requestId = null,
            amount = BigDecimal("100.00"),
            currency = "USD",
            status = PaymentStatus.INIT,
            merchantId = UUID.randomUUID(),
            customerId = null
        )
        
        every { paymentRepository.findById(paymentId) } returns Optional.of(payment)
        every { paymentRepository.save(any<Payment>()) } answers { firstArg() }
        
        // When
        val result = paymentService.cancelPayment(paymentId)
        
        // Then
        assertThat(result.status).isEqualTo(PaymentStatus.CANCELLED)
        verify { paymentRepository.save(match<Payment> { it.status == PaymentStatus.CANCELLED }) }
    }
}
