# Payment Aggregator API Guide

## Overview

This guide provides detailed information about the Payment Aggregator API endpoints, including request/response formats, error handling, and usage examples.

## Base URL

```
http://localhost:8080
```

## Authentication

This demo application does not implement authentication. In a production environment, you would typically use:
- OAuth 2.0 / JWT tokens
- API keys
- Merchant-specific authentication

## Content Type

All API requests should use `Content-Type: application/json` for POST/PUT/PATCH requests.

## Error Handling

The API returns standard HTTP status codes:

- `200 OK` - Successful GET/PUT/PATCH
- `201 Created` - Successful POST
- `400 Bad Request` - Invalid request data
- `404 Not Found` - Resource not found
- `500 Internal Server Error` - Server error

Error responses follow this format:

```json
{
  "error": "BAD_REQUEST",
  "message": "Invalid request data",
  "timestamp": "2024-01-15T10:30:00Z"
}
```

## Required Headers

All POST requests require the following headers:

- `Content-Type: application/json` - Request content type
- `X-Request-Id: <unique-uuid>` - Unique request identifier for tracking and idempotency
- `Idempotency-Key: <unique-key>` - Optional, for duplicate request prevention

## Idempotency

POST requests support idempotency using both `X-Request-Id` and `Idempotency-Key` headers:

```bash
curl -X POST http://localhost:8080/payments \
  -H "Content-Type: application/json" \
  -H "X-Request-Id: 550e8400-e29b-41d4-a716-446655440999" \
  -H "Idempotency-Key: unique-key-123" \
  -d '{"amount": 100.00, "currency": "USD", "merchant_id": "..."}'
```

The combination of `X-Request-Id`, `Idempotency-Key`, and endpoint creates a unique idempotency key. If the same combination is used within the TTL period, the cached response will be returned.

## API Endpoints

### Payments

#### Create Payment

Creates a new payment in `INIT` status.

**Request:**
```http
POST /payments
Content-Type: application/json
X-Request-Id: 550e8400-e29b-41d4-a716-446655440999
Idempotency-Key: payment-create-001

{
  "amount": 100.00,               // Required, payment amount
  "currency": "USD",              // Required, 3-letter currency code
  "merchant_id": "uuid",          // Required, merchant identifier
  "customer_id": "uuid"           // Optional, customer identifier
}
```

**Response:**
```json
{
  "id": "payment-uuid",
  "request_id": "request-uuid",
  "amount": 100.00,
  "currency": "USD",
  "status": "INIT",
  "merchant_id": "merchant-uuid",
  "customer_id": "customer-uuid",
  "created_at": "2024-01-15T10:30:00Z",
  "last_modified_at": "2024-01-15T10:30:00Z"
}
```

#### Confirm Payment

Confirms a payment with payment method details and triggers smart routing.

**Request:**
```http
POST /payments/{id}/confirm
Content-Type: application/json
X-Request-Id: 550e8400-e29b-41d4-a716-446655441000
Idempotency-Key: payment-confirm-001

{
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
}
```

**Response:**
```json
{
  "id": "attempt-uuid",
  "payment_id": "payment-uuid",
  "amount": 100.00,
  "captured_amount": 0.00,
  "currency": "USD",
  "status": "RECEIVED",
  "merchant_id": "merchant-uuid",
  "routing_mode": "SMART",
  "route_decision": {
    "candidates": ["StripeMock", "AdyenMock"],
    "strategy_used": ["ELIGIBILITY", "RULES", "COST", "HEALTH", "WEIGHT"],
    "selected_provider": "StripeMock",
    "reason": "Routing completed successfully"
  },
  "provider_name": null,
  "created_at": "2024-01-15T10:30:00Z",
  "last_modified_at": "2024-01-15T10:30:00Z"
}
```

### Merchants

#### Create Merchant

**Request:**
```http
POST /merchants
Content-Type: application/json

{
  "business_name": "Demo Merchant Inc",
  "country": "US"
}
```

#### Get Merchant

**Request:**
```http
GET /merchants/{id}
```

### Customers

#### Create Customer

**Request:**
```http
POST /customers
Content-Type: application/json

{
  "request_id": "uuid",
  "email": "customer@example.com",
  "name": "John Doe",
  "country": "US",
  "address": {
    "line1": "123 Main St",
    "line2": "Apt 1",
    "city": "New York",
    "state": "NY",
    "country": "US",
    "postal_code": "10001"
  }
}
```

### Admin & Configuration

#### Get Configuration

**Request:**
```http
GET /admin/config
```

**Response:**
```json
{
  "providers": [...],
  "routing_rules": [...],
  "routing_strategies": {...},
  "routing_weights": {...},
  "fx_rates": [...]
}
```

#### Update Configuration

**Request:**
```http
PUT /admin/config/{key}
Content-Type: application/json

{
  "StripeMock": 70,
  "AdyenMock": 20,
  "LocalBankMock": 10
}
```

## Smart Routing

The routing engine applies strategies in this order:

1. **Eligibility** - Filters providers by network/currency/country support
2. **Rules** - Applies business rules (e.g., AMEX prefers Adyen)
3. **Health** - Removes unhealthy providers
4. **Cost** - Sorts by lowest fees
5. **Weight** - Applies traffic distribution weights

### Routing Test Scenarios

#### AMEX Routing (Prefers Adyen)
```json
{
  "payment_method": {
    "type": "card",
    "card": {
      "number": "378282246310005"  // AMEX card
    }
  }
}
```

#### Domestic BIN (Uses LocalBank)
```json
{
  "payment_method": {
    "type": "card",
    "card": {
      "number": "4111111111111111"  // Domestic VISA
    }
  }
}
```

#### Soft Decline Simulation
```json
{
  "amount": 123.37  // Amount ending in 37 triggers soft decline
}
```

## Monitoring

### Health Check

**Request:**
```http
GET /actuator/health
```

**Response:**
```json
{
  "status": "UP",
  "components": {
    "paymentProviders": {
      "status": "UP",
      "details": {
        "healthy_providers": ["StripeMock", "AdyenMock", "LocalBankMock"],
        "total_providers": 3
      }
    },
    "configuration": {
      "status": "UP",
      "details": {
        "config_count": 5,
        "required_configs": "all present"
      }
    }
  }
}
```

### Metrics

**Request:**
```http
GET /actuator/metrics
```

Key metrics:
- `payments.created` - Total payments created
- `payments.confirmed` - Total payments confirmed
- `routing.duration` - Routing decision time
- `provider.call.duration` - Provider response time

## Rate Limits

This demo application does not implement rate limiting. In production, consider:
- Per-merchant rate limits
- Per-IP rate limits
- Sliding window rate limiting

## Webhooks

This demo does not implement webhooks. In production, you would typically send:
- Payment status updates
- Failed payment notifications
- Routing decision logs

## SDK Examples

### cURL Examples

See the main README.md for comprehensive cURL examples.

### Postman Collection

Import the provided Postman collection for interactive testing:
- `postman/Payment-Aggregator-Demo.postman_collection.json`
- `postman/Payment-Aggregator-Demo.postman_environment.json`

## Support

For questions about this demo API:
- Check the interactive documentation at `/swagger-ui.html`
- Review the source code for implementation details
- Test scenarios using the provided Postman collection
