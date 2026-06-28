## 1. Dependencies and Configuration

- [x] 1.1 Add `spring-boot-starter-data-redis` and `com.bucket4j:bucket4j-redis` (latest stable) to `JPPhotoManagerWeb/backend/pom.xml`
- [x] 1.2 Add `REDIS_HOST` (default `localhost`) and `REDIS_PORT` (default `6379`) properties to `src/main/resources/application.yml`
- [x] 1.3 Add a `redis` service to `docker-compose.yml` using `redis:7-alpine`, port `6379`, `--maxmemory 256mb --maxmemory-policy allkeys-lru`

## 2. Spring Bean Setup

- [x] 2.1 Add a `RedisConnectionFactory` bean to `AppConfig` using `LettuceConnectionFactory` wired to `REDIS_HOST` / `REDIS_PORT`
- [x] 2.2 Add a `LettuceBasedProxyManager<String>` bean to `AppConfig` that accepts the `RedisConnectionFactory`
- [ ] 2.3 Verify the application starts and connects to Redis with `docker-compose up redis`

## 3. RateLimitFilter Refactor

- [x] 3.1 Add a `ProxyManager<String>` constructor parameter to `RateLimitFilter` (remove the `ConcurrentHashMap<String, Bucket>` field)
- [x] 3.2 Replace `buckets.computeIfAbsent(bucketKey, k -> createBucket(endpointKey))` with `proxyManager.builder().build(bucketKey, () -> BucketConfiguration.builder().addLimit(createLimit(endpointKey)).build())`
- [x] 3.3 Extract the bandwidth creation into a `createLimit(String endpointKey)` method returning `BandwidthDefinition`
- [x] 3.4 Wrap the `proxyManager` call in a try-catch for `Exception`; on error log a warning and call `chain.doFilter(request, response)` (fail-open per design D4)
- [x] 3.5 Confirm the `@Component` and `@RequiredArgsConstructor` annotations still apply correctly with the injected `ProxyManager`

## 4. Test Updates

- [x] 4.1 Update `RateLimitFilterTest` to construct `RateLimitFilter` with an in-memory `ProxyManager` (use `Bucket4j.extension(Local.class).proxyManagerForMap(new ConcurrentHashMap<>())` or equivalent in-memory substitute from `bucket4j-redis`)
- [x] 4.2 Verify all five existing test cases still pass: `loginEndpoint_11thRequestFromSameIp_returns429`, `catalogEndpoint_6thRequestFromSameIp_returns429`, `limitExceeded_retryAfterHeaderPresentAndPositive`, `differentIps_doNotShareBucket`
- [x] 4.3 Add a test `redisUnavailable_requestIsAllowedThrough` that injects a proxy manager throwing a `RuntimeException` and asserts the filter returns HTTP 200 and logs a warning

## 5. Verification

- [x] 5.1 Run `mvn test -Dtest=RateLimitFilterTest` and confirm all tests pass
- [ ] 5.2 Start the full stack with `docker-compose up` and manually trigger 11 rapid login attempts; confirm the 11th returns `429` with `Retry-After`
- [x] 5.3 Run `mvn test` (full unit test suite) and confirm no regressions
