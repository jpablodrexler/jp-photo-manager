## 1. Dependency

- [x] 1.1 Add `bucket4j-core` dependency to `pom.xml`

## 2. Rate limit filter

- [x] 2.1 Create `infrastructure/web/filter/RateLimitFilter.java` implementing `jakarta.servlet.Filter`
- [x] 2.2 Maintain a `ConcurrentHashMap<String, Bucket>` keyed by client IP + endpoint key
- [x] 2.3 Create bucket for `POST /api/auth/login`: capacity 10, refill 10 per 60 seconds (interval)
- [x] 2.4 Create bucket for `POST /api/assets/catalog`: capacity 5, refill 5 per 3600 seconds (interval)
- [x] 2.5 On limit exceeded, set response status 429, add `Retry-After` header, write `ErrorResponse` JSON body, and return without calling `chain.doFilter()`
- [x] 2.6 Register the filter in Spring Security config (before the authentication filter)

## 3. GlobalExceptionHandler update

- [x] 3.1 Ensure 429 responses from the filter produce a body consistent with `ErrorResponse`; the filter writes the body directly since the exception is not propagated through Spring MVC

## 4. Backend unit tests

- [x] 4.1 Test that the 11th login request from the same IP within a minute returns 429
- [x] 4.2 Test that the 6th catalog request from the same IP within an hour returns 429
- [x] 4.3 Test that the `Retry-After` header is present and positive on 429 responses
- [x] 4.4 Test that requests from different IPs do not share the same bucket

## 5. Testing and Commit

- [x] 5.1 Run backend test suite: `cd JPPhotoManagerWeb/backend && mvn test`
- [x] 5.2 Run frontend test suite: `cd JPPhotoManagerWeb/frontend && npm test`
- [x] 5.3 Commit all changes (only after both test suites pass)
