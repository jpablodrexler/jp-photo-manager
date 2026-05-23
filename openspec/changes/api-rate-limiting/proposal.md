## Why

The API currently has no request rate limits. Brute-force login attempts and runaway catalog requests can saturate the server without any protection. Rate limiting the authentication endpoint prevents credential stuffing, and throttling the catalog endpoint prevents accidental or malicious resource exhaustion.

## What Changes

- Add `bucket4j-spring-boot-starter` to the backend
- Configure per-IP rate limits: 10 requests/minute on `POST /api/auth/login`, 5 requests/hour on `POST /api/assets/catalog`
- Requests that exceed the limit receive `429 Too Many Requests` with a `Retry-After` header
- `GlobalExceptionHandler` handles `RateLimitExceededException` to return a consistent error body

## Capabilities

### New Capabilities

- `api-rate-limiting`: The login and catalog endpoints enforce per-IP rate limits. Clients exceeding the limit receive a `429 Too Many Requests` response with a `Retry-After` header indicating when they may retry.

### Modified Capabilities

_(none)_

## Impact

- `JPPhotoManagerWeb/backend/pom.xml` — add `bucket4j-spring-boot-starter`
- `JPPhotoManagerWeb/backend/src/main/resources/application.yml` — bucket4j rate limit configuration
- `JPPhotoManagerWeb/backend/src/main/java/com/jpablodrexler/photomanager/infrastructure/web/filter/RateLimitFilter.java` — servlet filter applying rate limits
- `JPPhotoManagerWeb/backend/src/main/java/com/jpablodrexler/photomanager/infrastructure/web/exception/GlobalExceptionHandler.java` — handle `RateLimitExceededException`
- `JPPhotoManagerWeb/backend/src/test/` — tests for rate limit enforcement
