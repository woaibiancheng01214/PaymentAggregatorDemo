# Idempotency Changes: X-Request-Id Implementation

## Overview

This document outlines the changes made to implement `X-Request-Id` header-based idempotency instead of the previous `X-Merchant-Id` approach. This change makes the idempotency system more general-purpose and follows industry best practices.

## Changes Made

### 1. Core Idempotency System

#### IdempotencyInterceptor.kt
- **Changed**: `X-Merchant-Id` header → `X-Request-Id` header
- **Updated**: Header constant from `MERCHANT_ID_HEADER` to `REQUEST_ID_HEADER`
- **Modified**: Validation logic to require both `Idempotency-Key` and `X-Request-Id` headers
- **Enhanced**: Logging to include request ID for better traceability

#### IdempotencyService.kt
- **Changed**: Method parameters from `merchantId: String` to `requestId: String`
- **Updated**: All method signatures:
  - `executeIdempotent(requestId, idempotencyKey, endpoint, operation)`
  - `isOperationExecuted(requestId, idempotencyKey, endpoint)`
  - `invalidateIdempotencyKey(requestId, idempotencyKey, endpoint)`

#### IdempotencyKey.kt (in IdempotencyPort.kt)
- **Changed**: Property from `merchantId: String` to `requestId: String`
- **Updated**: Redis key format from `"idempotency:$merchantId:$endpoint:$idempotencyKey"` to `"idempotency:$requestId:$endpoint:$idempotencyKey"`

### 2. API Layer Changes

#### Request DTOs
- **CreatePaymentRequest**: Removed `request_id` field from payload
- **CreateCustomerRequest**: Removed `request_id` field from payload
- **Note**: `request_id` is now provided via `X-Request-Id` header instead

#### Controllers
- **PaymentController**:
  - Added `@RequestHeader("X-Request-Id") requestId: String?` parameter to `createPayment()` and `confirmPayment()`
  - Enhanced OpenAPI documentation with header descriptions
- **CustomerController**:
  - Added `@RequestHeader("X-Request-Id") requestId: String?` parameter to `createCustomer()`
  - Enhanced OpenAPI documentation

### 3. Documentation Updates

#### README.md
- **Added**: "Required Headers" section explaining `X-Request-Id` and `Idempotency-Key`
- **Updated**: All API examples to include the new headers
- **Enhanced**: Testing section with proper header usage

#### API_GUIDE.md
- **Added**: Comprehensive "Required Headers" section
- **Updated**: Idempotency section with new header requirements
- **Modified**: All API endpoint examples to include `X-Request-Id` header
- **Enhanced**: Explanations of how idempotency keys are constructed

#### Postman Collection
- **Updated**: All POST requests to include:
  - `X-Request-Id: {{$randomUUID}}`
  - `Idempotency-Key: <operation>-{{$timestamp}}`
- **Removed**: `request_id` from request payloads
- **Enhanced**: Dynamic header generation for testing

### 4. OpenAPI/Swagger Documentation
- **Added**: `@Parameter` annotations for `X-Request-Id` headers
- **Enhanced**: Operation descriptions to mention header requirements
- **Improved**: API documentation clarity for developers

## Benefits of This Change

### 1. **General Purpose Design**
- Request ID is no longer tied to merchant context
- Can be used across all API endpoints consistently
- Follows industry standard practices

### 2. **Better Traceability**
- Each request has a unique identifier for end-to-end tracking
- Improved logging and debugging capabilities
- Better correlation across distributed systems

### 3. **Enhanced Idempotency**
- More robust idempotency key construction
- Clearer separation of concerns (request tracking vs. business context)
- Better support for client-side request management

### 4. **Industry Compliance**
- Follows common patterns used by major payment processors
- Aligns with REST API best practices
- Easier integration for client applications

## Migration Guide

### For API Clients

#### Before:
```bash
curl -X POST http://localhost:8080/payments \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: unique-key-123" \
  -H "X-Merchant-Id: merchant-uuid" \
  -d '{
    "request_id": "550e8400-e29b-41d4-a716-446655440999",
    "amount": 100.00,
    "currency": "USD",
    "merchant_id": "550e8400-e29b-41d4-a716-446655440001"
  }'
```

#### After:
```bash
curl -X POST http://localhost:8080/payments \
  -H "Content-Type: application/json" \
  -H "X-Request-Id: 550e8400-e29b-41d4-a716-446655440999" \
  -H "Idempotency-Key: unique-key-123" \
  -d '{
    "amount": 100.00,
    "currency": "USD",
    "merchant_id": "550e8400-e29b-41d4-a716-446655440001"
  }'
```

### Key Changes:
1. **Remove** `request_id` from request body
2. **Add** `X-Request-Id` header with unique UUID
3. **Keep** `Idempotency-Key` header (optional but recommended)
4. **Remove** `X-Merchant-Id` header (no longer needed)

## Testing

All changes have been tested and verified:
- ✅ Build compiles successfully
- ✅ Unit tests pass
- ✅ API documentation updated
- ✅ Postman collection updated
- ✅ README examples updated

## Backward Compatibility

⚠️ **Breaking Change**: This is a breaking change for API clients. The old `X-Merchant-Id` header approach is no longer supported. Clients must update to use the new `X-Request-Id` header pattern.
