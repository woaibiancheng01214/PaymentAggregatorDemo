
# Payment Aggregator Demo Project Plan

## Overview

The Payment Aggregator Demo is a sample application that demonstrates how to use the Payment Aggregator API to process payments. The application is built using Kotlin and Spring Boot. The application is designed to be run locally and is not intended for production use.

## high level scope

MVP feature set (accept payments only):

Payment APIs
- Create payment (POST /payments): 
- Get payment (GET /payments/{id})
- Update payment (PATCH /payments/{id})
- Confirm payment (POST /payments/{id}/confirm): authorize+capture with card
- Cancel payment (POST /payments/{id}/cancel)

Merchants management APIs
- Create merchant (POST /merchants)
- Get merchant (GET /merchants/{id})
- Update merchant (PATCH /merchants/{id})

Customer management APIs
- Create customer (POST /customers)
- Get customer (GET /customers/{id})
- Update customer (PATCH /customers/{id})

Routing management module
- routing CURD API
- Smart routing with multiple strategies, hot-swappable at runtime:
  - Rules-based (country/BIN/network).
  - Cost-based (fees).
  - Success-rate/weight-based (AB routing).
  - Health/failover + retry on soft declines.
- FX shim (convert merchant currency → PSP settlement currency via static rates).
- Card network constraints (basic: VISA/MC/AMEX acceptance flags, 3DS required flag).
- Provider adapters for 2–3 PSPs (mocked via WireMock or internal simulators).
- Config from YAML + hot-reload via admin APIs into H2 (so both file and runtime configurable).

Non-functional requirements:
- Observability: OpenAPI, health/readiness, structured logs, minimal metrics.
- Batteries included: Dockerfile, docker-compose (app + postgres + Redis), seed data, Postman collection, README with one-liner run.

## Out-of-scope (documented notes only):
- internal transfer, 
- payout, 
- full KYC—include a short routing design note at the end.

## coding principles
- DDD
- SOLID principles
- Clean Architecture
- Tests: fast unit first; one or two full integration flows.
- Hexagonal Architecture: no domain logic in controllers/adapters.

- Exceptions: map to 4xx/5xx; never leak provider internals

## tech stack
- spring boot
- kotlin
- postgres
- redis
- gradle
- docker
- docker-compose
- wiremock
- postman
- junit
- mockk
- assertj
- spring boot actuator
- spring boot admin
- spring boot devtools
- spring boot test
- spring boot data jpa
- spring boot data redis
- spring boot flyway
- spring boot h2
- spring boot web

## Technical details

database:
- use postgres
- use flyway for version control
- general table design: id, data, version, created_at, last_modified_at
- use READ UNCOMMITTED isolation level
- use optimistic locking for concurrency control


domain objects:

Payment object
```json
{
  "id": "4c25d0cb-5c67-49c8-9fd3-72a874f7e6d5", // UUID
  "requestId": "4c25d0cb-5c67-49c8-9fd3-72a874f7e6d5", // UUID
  "amount": "100", // BigDecimal
  "currency": "USD", // 3-letter ISO 4217 currency code
  "status": "PENDING", // INIT, REQUIRES_AUTHORISATION, REQUIRES_CAPTURE, PENDING, SUCCEEDED, CANCELLED
  "merchantId": "4c25d0cb-5c67-49c8-9fd3-72a874f7e6d5", // UUID
  // TBD
}
```

Payment Attempt Object: created every time customer tries to pay (call payment confirm API)
```json
{
  "id": "4c25d0cb-5c67-49c8-9fd3-72a874f7e6d5", // UUID
  "paymentId": "4c25d0cb-5c67-49c8-9fd3-72a874f7e6d5",
  "amount": "100",  // BigDecimal
  "capturedAmount": "50",  // BigDecimal
  "currency": "USD", // 3-letter ISO 4217 currency code
  "status": "PENDING", // RECEIVED, AUTHORISED, CAPTURED, SETTLED, FAILED, CANCELLED
  "merchantId": "4c25d0cb-5c67-49c8-9fd3-72a874f7e6d5",
  "paymentMethod": "method",
  "failureDetails": {
    "code": "00",
    "message": ""
  },
  "routingMode": "SMART",
  "routeDecision": {
    "candidates": ["StripeM","AdyenM","LocalBankM"],
    "strategyUsed": ["ELIGIBILITY","RULES","COST","HEALTH","WEIGHT"]
  }
}
```

Merchant object
```json
{
  "id": "4c25d0cb-5c67-49c8-9fd3-72a874f7e6d5", // UUID
  "businessName": "Merchant busienss name",
  "country": "US"
  
}
```

Customer object
```json
{
  "id": "4c25d0cb-5c67-49c8-9fd3-72a874f7e6d5", // UUID
  "requestId": "4c25d0cb-5c67-49c8-9fd3-72a874f7e6d5", // UUID
  "email": "customer@example.com",
  "name": "John Doe",
  "country": "US",
  "address": {
    "line1": "123 Main St",
    "line2": "Apt 1",
    "city": "New York",
    "state": "NY",
    "country": "US",
    "postalCode": "10001"
  }
}
```

PaymentProvider interface
```kotlin
// for currency use java.util.Currency
interface PaymentProvider {
  val name: String
  fun authorize(req: ProviderPaymentRequest): ProviderPaymentResponse
  fun capture(req: ProviderPaymentRequest): ProviderPaymentResponse
  fun supports(network: CardNetwork, currency: Currency, country: String): Boolean
  fun feeFor(currency: Currency, amount: Long): Fee   // for cost-based routing
  fun health(): ProviderHealth
}

```

Routing engine (composable strategies):
```text
RoutingEngine orchestrates a chain:
  - Eligibility filter (network/currency/country support, 3DS requirement).
  - Rules strategy (hard business rules).
  - Cost strategy (lowest fee).
  - Weight strategy (AB or weighted random).
  - Health filter (remove degraded providers).

Output: ordered provider candidates with rationale.

Retry policy: on SOFT_DECLINE or TIMEOUT → try next candidate (max N); log attempts.
```

Config sources:
  - Bootstrap from application-routing.yml.
  - Runtime: admin APIs persist JSON into H2 via ConfigPort.
  - Optional feature flags (e.g., enable_cost_strategy).

Idempotency:
  - RedisIdempotencyStore keyed by merchantId + Idempotency-Key + endpoint.
  - Write-once semantics with TTL.

Testing & simulation:

Providers are mock adapters:
  - StripeMockAdapter (simple success unless amount ends with 37 → soft decline).
  - AdyenMockAdapter (reject AMEX, surcharge fee).
  - LocalBankMockAdapter (great fees for domestic BINs, flaky health toggled by config).


Example config file: application-routing.yml
```yaml
providers:
  - name: StripeMock
    currencies: [USD, EUR]
    countries: [US, GB, DE]
    networks: [VISA, MASTERCARD]
    baseFeeBps: 300
  - name: AdyenMock
    currencies: [USD, EUR, GBP]
    countries: [US, NL, GB, DE]
    networks: [VISA, MASTERCARD, AMEX]
    baseFeeBps: 290
  - name: LocalBankMock
    currencies: [USD]
    countries: [US]
    networks: [VISA, MASTERCARD]
    baseFeeBps: 220

routing:
  rules:
    - if: { country: US, network: AMEX }
      action: { prefer: [AdyenMock], mode: PREFERRED }
    - if: { binRange: "411111-411119" }
      action: { prefer: [LocalBankMock], mode: STRICT }
  strategies: { cost: true, weight: true, health: true }
  weights: { StripeMock: 60, AdyenMock: 30, LocalBankMock: 10 }

fx:
  rates:
    - { from: USD, to: EUR, rate: 0.90 }
    - { from: EUR, to: USD, rate: 1.11 }
    - { from: USD, to: GBP, rate: 0.78 }

```

sample folder structure
```text

payment-aggregator/
  app/ (SpringBoot app)
    src/main/kotlin/...
      com.example.paymentAggregator/
        api/            # REST controllers + DTOs
        domain/         # Entities, value objects, services
          routing/      # Strategy engine, policies
          payments/     # PaymentService, Payment entity
          fx/           # FX service
        ports/          # Interfaces (PaymentProvider, RiskPort, FXPort, ConfigPort)
        adapters/
          providers/    # StripeMockAdapter, AdyenMockAdapter, LocalBankMockAdapter
          config/       # H2ConfigRepository, YamlBootstrap
          fx/           # StaticRateFXAdapter
          idempotency/  # RedisIdempotencyStore
        infra/          # db config, web config, logging, exceptions, security
        support/        # utils, error codes
    src/test/kotlin/... # unit + slice + integration tests
  docker/               # docker-compose, WireMock mappings (if used)
  postman/              # collection + env
  README.md
  build.gradle.kts / gradle.properties
  Dockerfile

```
### Deliverables checklist (what reviewers will see)

1. Runnable project
- ./gradlew bootRun or docker compose up brings API online.
- Seed routing/providers/FX in data.sql or bootstrap YAML.
2. OpenAPI at /swagger-ui + /v3/api-docs.
3. Endpoints: as described above
4. Domain objects and persistence objects as described above
5. database: postgres, initial sql, flyway schema control, docker-compose, database connection via JPA
6. Postman collection json file with sample requests + scripts.
7Tests
- Unit: routing, idempotency, FX. 
- Integration: happy path, failover path, cost-based selection.
8. Observability
- /actuator/health, /actuator/readiness
- Structured logs with provider attempts + routing rationale.