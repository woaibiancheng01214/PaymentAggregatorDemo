# Payment Aggregator Demo

A comprehensive payment aggregator demo application built with Kotlin and Spring Boot, featuring smart routing, multiple payment providers, and enterprise-grade observability.

## 🚀 Quick Start

**One-liner to get everything running:**

```bash
./scripts/run.sh
```

This will:
- Start PostgreSQL and Redis using Docker Compose
- Build and run the Payment Aggregator application
- Set up all necessary dependencies
- Display service URLs and connection information

## 📋 Features

### Core Payment Processing
- ✅ Create, update, and cancel payments
- ✅ Confirm payments with card details
- ✅ Support for multiple currencies (USD, EUR, GBP)
- ✅ Merchant and customer management
- ✅ Payment attempt tracking with detailed routing decisions

### Smart Routing Engine
- ✅ **Eligibility filtering** - Network, currency, and country support
- ✅ **Rules-based routing** - Business rules (e.g., AMEX prefers Adyen)
- ✅ **Cost-based routing** - Route to lowest-fee providers
- ✅ **Weight-based routing** - A/B testing and traffic distribution
- ✅ **Health-based filtering** - Exclude unhealthy providers
- ✅ **Retry logic** - Automatic failover on soft declines

### Payment Providers (Mock Implementations)
- ✅ **StripeMock** - Simulates Stripe with soft declines on amounts ending in 37
- ✅ **AdyenMock** - Simulates Adyen with AMEX support and surcharges
- ✅ **LocalBankMock** - Simulates domestic bank with great rates for local cards

### Enterprise Features
- ✅ **FX Service** - Currency conversion with static rates
- ✅ **Idempotency** - Redis-based duplicate request prevention
- ✅ **Configuration Management** - Runtime config updates via admin APIs
- ✅ **Observability** - Health checks, metrics, structured logging
- ✅ **OpenAPI Documentation** - Interactive API docs with Swagger UI

## 🏗️ Architecture

The application follows **Clean Architecture** and **Domain-Driven Design** principles:

```
├── api/              # REST controllers and DTOs
├── domain/           # Core business logic
│   ├── payments/     # Payment entities and services
│   ├── routing/      # Smart routing engine
│   └── fx/           # Foreign exchange services
├── ports/            # Interfaces (repositories, providers)
├── adapters/         # External integrations
│   ├── providers/    # Payment provider implementations
│   ├── config/       # Configuration management
│   ├── fx/           # FX rate adapters
│   └── idempotency/  # Redis idempotency store
└── infra/            # Infrastructure (DB, web, logging)
```

## 🔧 Technology Stack

- **Backend**: Kotlin, Spring Boot 3.3, JPA/Hibernate
- **Database**: PostgreSQL with Flyway migrations
- **Cache**: Redis for idempotency and session storage
- **Build**: Gradle with Kotlin DSL
- **Containerization**: Docker & Docker Compose
- **Documentation**: OpenAPI 3 with Swagger UI
- **Testing**: JUnit 5, MockK, Spring Boot Test
- **Observability**: Spring Actuator, Micrometer metrics

## 📊 API Endpoints

### Payments
- `POST /payments` - Create a new payment
- `GET /payments/{id}` - Get payment details
- `PATCH /payments/{id}` - Update payment
- `POST /payments/{id}/confirm` - Confirm payment with card details
- `POST /payments/{id}/cancel` - Cancel payment

### Merchants
- `POST /merchants` - Create merchant
- `GET /merchants/{id}` - Get merchant details
- `PATCH /merchants/{id}` - Update merchant

### Customers
- `POST /customers` - Create customer
- `GET /customers/{id}` - Get customer details
- `PATCH /customers/{id}` - Update customer

### Admin
- `GET /admin/config` - Get all configuration
- `PUT /admin/config/{key}` - Update configuration
- `POST /admin/config/refresh` - Refresh configuration

### Monitoring
- `GET /actuator/health` - Health check
- `GET /actuator/metrics` - Application metrics
- `GET /swagger-ui.html` - Interactive API documentation

## 🧪 Testing the Application

### 1. Create a Payment

```bash
curl -X POST http://localhost:8080/payments \
  -H "Content-Type: application/json" \
  -d '{
    "request_id": "550e8400-e29b-41d4-a716-446655440999",
    "amount": 100.00,
    "currency": "USD",
    "merchant_id": "550e8400-e29b-41d4-a716-446655440001"
  }'
```

### 2. Confirm Payment

```bash
curl -X POST http://localhost:8080/payments/{payment_id}/confirm \
  -H "Content-Type: application/json" \
  -d '{
    "payment_method": {
      "type": "card",
      "card": {
        "number": "4111111111111111",
        "expiry_month": 12,
        "expiry_year": 2025,
        "cvv": "123",
        "holder_name": "John Doe"
      }
    }
  }'
```

### 3. Check Routing Decision

The confirm payment response includes detailed routing information:

```json
{
  "id": "...",
  "payment_id": "...",
  "route_decision": {
    "candidates": ["StripeMock", "AdyenMock"],
    "strategy_used": ["ELIGIBILITY", "RULES", "COST", "HEALTH", "WEIGHT"],
    "selected_provider": "StripeMock",
    "reason": "Routing completed successfully"
  }
}
```

## 🎯 Smart Routing Examples

### Test Different Scenarios

1. **AMEX in US** (should prefer Adyen):
   ```json
   {"card": {"number": "378282246310005"}}
   ```

2. **Domestic BIN range** (should use LocalBank):
   ```json
   {"card": {"number": "4111111111111111"}}
   ```

3. **Soft decline simulation** (amount ending in 37):
   ```json
   {"amount": 123.37}
   ```

4. **Provider timeout** (amount ending in 99):
   ```json
   {"amount": 199.99}
   ```

## 🔧 Configuration Management

Update routing configuration at runtime:

```bash
# Update provider weights
curl -X PUT http://localhost:8080/admin/config/routing_weights \
  -H "Content-Type: application/json" \
  -d '{"StripeMock": 70, "AdyenMock": 20, "LocalBankMock": 10}'

# Refresh configuration
curl -X POST http://localhost:8080/admin/config/refresh
```

## 📈 Monitoring & Observability

- **Health Check**: http://localhost:8080/actuator/health
- **Metrics**: http://localhost:8080/actuator/metrics
- **Swagger UI**: http://localhost:8080/swagger-ui.html

### Key Metrics
- `payments.created` - Total payments created
- `payments.confirmed` - Total payments confirmed
- `routing.duration` - Routing decision time
- `provider.call.duration` - Provider response time

## 🗄️ Database

The application uses PostgreSQL with Flyway for schema management. Seed data includes:

- 3 sample merchants (US, UK, DE)
- 3 sample customers
- Pre-configured routing rules and provider settings
- FX rates for USD/EUR/GBP

## 🔄 Development

### Local Development

```bash
# Start dependencies only
docker-compose up postgres redis -d

# Run application locally
./gradlew bootRun

# Run tests
./gradlew test
```

### Building

```bash
# Build JAR
./gradlew bootJar

# Build Docker image
docker build -t payment-aggregator .
```

## 🚨 Error Handling

The application includes comprehensive error handling:

- **4xx errors** for client mistakes (invalid data, not found)
- **5xx errors** for server issues (provider timeouts, database errors)
- **Structured error responses** with error codes and messages
- **Provider error mapping** (never leak internal provider details)

## 🔒 Security Considerations

This is a **demo application** and includes simplified security:

- No authentication/authorization (would use OAuth2/JWT in production)
- No PCI compliance features (would encrypt card data in production)
- No rate limiting (would use Redis-based rate limiting in production)
- Admin endpoints are unprotected (would require admin roles in production)

## 📚 Additional Resources

- **Postman Collection**: `postman/Payment-Aggregator-Demo.postman_collection.json`
- **API Documentation**: http://localhost:8080/swagger-ui.html
- **Health Checks**: http://localhost:8080/actuator/health

## 🤝 Contributing

This is a demo project showcasing payment aggregation patterns. Feel free to explore the code and adapt it for your needs!

## 📄 License

MIT License - see LICENSE file for details.
