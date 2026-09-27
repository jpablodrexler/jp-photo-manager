# Java Developer — Async & Streaming, Error Handling/Logging & Pagination

_Part of the `java-developer` skill — see `../SKILL.md` for the topic index. Load this file only when your current task touches this topic._

## 7. Async & Streaming

- Annotate long-running methods with `@Async`.
- Return `CompletableFuture<T>` so the caller is non-blocking.
- Accept a `Consumer<T>` callback for progress streaming.
- Stream progress to the client using `SseEmitter` in the controller.
- Configure the thread pool in `AppConfig`:

```java
@Bean(name = "taskExecutor")
public Executor taskExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(2);
    executor.setMaxPoolSize(4);
    executor.setQueueCapacity(100);
    executor.setThreadNamePrefix("app-async-");
    executor.initialize();
    return executor;
}
```

---

## 8. Error Handling & Logging

- Inject the logger with `@Slf4j`; never use `System.out` or `System.err`.
- Log levels: `INFO` for normal flow, `ERROR` for failures, `DEBUG` for diagnostic traces.
- Always include context (file path, entity ID, etc.) in log messages.
- Catch specific exceptions; log with context; re-throw as `RuntimeException` or return a safe default.

```java
try {
    Asset asset = createAsset(folderPath, fileName);
    log.info("Cataloged asset: {}", asset.getFullPath());
} catch (IOException e) {
    log.error("Failed to read file: {}", filePath, e);
} catch (Exception e) {
    log.error("Unexpected error cataloging: {}", filePath, e);
    throw new RuntimeException("Catalog failed", e);
}
```

---

## 9. Pagination

- Accept `page` (0-based) and an optional `SortCriteria` enum from callers via `AssetFilter`.
- Return a generic `PaginatedResult<T>` wrapper from use cases.

---

