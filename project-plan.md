
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
- Clean Architecture
- TDD
- Hexagonal Architecture
- SOLID principles

## Technical details

database:
- use postgres
- use flyway for version control
- general table design: id, data, version, created_at, last_modified_at
- use READ UNCOMMITTED isolation level
- use optimistic locking for concurrency control

API design:
- require client to send requestId for idempotency control when creating resources

domain objects:

Payment object
```json
{
  "id": "4c25d0cb-5c67-49c8-9fd3-72a874f7e6d5", // UUID
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
  "currency": "USD", 3-letter ISO 4217 currency code
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
```