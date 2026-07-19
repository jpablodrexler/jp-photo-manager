## 1. Cache manager configuration

- [x] 1.1 In `config/AppConfig.java`, replace the `CaffeineCacheManager` in `cacheManager()` with a
      `RedisCacheManager` built from the existing `RedisConnectionFactory` bean, configuring
      per-cache TTLs for `home-stats` (10 min), `sub-folders` (5 min), `asset-exif` (30 min),
      `assets` (5 min), and `tags` (5 min), and setting `computePrefixWith(name -> name + ":")` on
      the default `RedisCacheConfiguration` so keys use a single colon separator.
- [x] 1.2 Add a `CacheErrorHandler` bean (extends/wraps `SimpleCacheErrorHandler`) that logs cache
      get/put/evict exceptions at `WARN` instead of rethrowing, and wire it via
      `CachingConfigurerSupport`/`errorHandler()` so all five caches fail open on a Redis outage.
- [x] 1.3 Remove the now-unused `com.github.benmanes.caffeine.cache.Caffeine` and
      `org.springframework.cache.caffeine.CaffeineCacheManager` imports from `AppConfig.java` if no
      longer referenced elsewhere.

## 2. Asset search cache

- [x] 2.1 Create `infrastructure/service/AssetSearchCacheKeyGenerator.java` implementing
      `org.springframework.cache.interceptor.KeyGenerator`, building
      `{folderId}:{sha256Hex(search|dateFrom|dateTo|minRating|sortCriteria|page|pageSize|includeDeleted|sortedTags)}`
      from the single `AssetFilter` argument passed to `GetAssetsUseCaseImpl.execute`. Register it as
      a `@Component` (or `@Bean` in `AppConfig`) named `assetSearchCacheKeyGenerator`.
- [x] 2.2 Annotate `GetAssetsUseCaseImpl.execute` with
      `@Cacheable(cacheNames = "assets", keyGenerator = "assetSearchCacheKeyGenerator")`.
- [x] 2.3 Add `AssetSearchCacheKeyGeneratorTest` covering: same filter twice produces the same key;
      different `page`/`minRating`/`tags` produce different keys; `null` `folderId` is handled
      without throwing.

## 3. Folder-scoped invalidation listener

- [x] 3.1 Create `infrastructure/kafka/AssetSearchCacheInvalidationListener.java` with
      `@KafkaListener(topics = "asset.cataloged", groupId = "asset-search-cache-invalidator", ...)`
      and `@KafkaListener(topics = "asset.deleted", groupId = "asset-search-cache-invalidator", ...)`
      methods, following the explicit-persistent-consumer-group pattern used by
      `AuditLogKafkaListener`.
- [x] 3.2 For `asset.deleted`, evict directly using `event.folderId()`. For `asset.cataloged`
      (which carries only `folderPath`), resolve `folderId` via the existing
      `GetFolderIdByPathUseCase` port before evicting.
- [x] 3.3 Implement `evictFolder(Long folderId)` using `RedisConnectionFactory.getConnection().scan(...)`
      with `ScanOptions.scanOptions().match("assets:" + folderId + ":*").count(200).build()` to
      enumerate and delete matching keys; do not use `KEYS`. Skip eviction (log at `DEBUG`) if
      `folderId` cannot be resolved.
- [x] 3.4 Wrap the eviction call in try/catch, logging failures at `WARN` without rethrowing, so a
      Redis hiccup never fails Kafka message processing or triggers a redelivery loop.
- [x] 3.5 Add `AssetSearchCacheInvalidationListenerTest` covering: `asset.deleted` evicts using the
      event's `folderId`; `asset.cataloged` resolves `folderId` via `GetFolderIdByPathUseCase` then
      evicts; an unresolvable folder path is skipped without throwing; a Redis exception during
      eviction is caught and logged.

## 4. Tag cache

- [x] 4.1 Annotate `ListTagsUseCaseImpl.execute` with
      `@Cacheable(cacheNames = "tags", key = "'all'", condition = "#query == null or #query.isBlank()")`.
- [x] 4.2 Annotate `AddTagToAssetUseCaseImpl.execute` and `RemoveTagFromAssetUseCaseImpl.execute`
      with `@CacheEvict(cacheNames = "tags", key = "'all'")`.
- [x] 4.3 Update `ListTagsUseCaseImplTest`, `AddTagToAssetUseCaseImplTest`, and
      `RemoveTagFromAssetUseCaseImplTest` if needed to confirm behavior is unchanged when caching is
      bypassed in unit tests (cache annotations are inert without a Spring context in plain Mockito
      tests — no assertion changes expected, but confirm no regressions).

## 5. Integration verification

- [x] 5.1 Add or extend an integration test (extending `PostgresIntegrationTest`, with Redis reachable
      per the standard local dev setup) that asserts: a repeated `GET /api/assets` call for the same
      folder/filters does not increase the count of underlying repository invocations, and a
      `POST`/tag mutation or catalog/delete event subsequently invalidates the relevant entries.
- [x] 5.2 Manually verify locally: start Redis, run the backend, confirm `redis-cli --scan --pattern
      "assets:*"` and `redis-cli GET tags:all` show populated entries after use, and that they clear
      after a catalog run / tag mutation respectively.

## 6. Documentation

- [x] 6.1 Update `CLAUDE.md`'s "Persistence"/cache description (root `CLAUDE.md` web-application
      section and/or `JPPhotoManagerWeb/CLAUDE.md`) to note that `server-side-spring-cache` is now
      backed by Redis (not Caffeine) and covers `assets`/`tags` in addition to the original three
      caches, following the existing documentation style for other Redis-backed features.
