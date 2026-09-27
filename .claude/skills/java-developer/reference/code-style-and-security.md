# Java Developer — Code Style Rules & Spring Security

_Part of the `java-developer` skill — see `../SKILL.md` for the topic index. Load this file only when your current task touches this topic._

## 14. Code Style Rules

- **New use cases:** declare a single-method interface in `domain/port/in/<subpackage>/`, implement in `application/usecase/<subpackage>/` with `@Service @Transactional`. Inject only `domain/port/out/` interfaces.
- **New service ports:** declare interface (suffix `Port`) in `domain/port/out/`, implement (suffix `ServiceAdapter`) in `infrastructure/service/`.
- **New repository ports:** declare interface (suffix `Repository`) in `domain/port/out/`, implement (suffix `RepositoryImpl`) in `infrastructure/persistence/adapter/`, backed by a Spring Data JPA interface (prefix `Jpa`) in `infrastructure/persistence/jpa/`.
- **New controllers:** add to `infrastructure/web/controller/`; inject use-case interfaces only; use MapStruct mappers for all HTTP DTO ↔ domain model conversion.
- **New HTTP DTOs:** place in `infrastructure/web/dto/request/`, `infrastructure/web/dto/response/`, or `infrastructure/web/dto/shared/` based on verified usage — never directly in `infrastructure/web/dto/`. Name request DTOs `{BaseName}RequestDto` and response DTOs `{BaseName}ResponseDto`; `shared/` DTOs keep their existing name (no forced suffix) since they don't have a single direction.
- **MapStruct only** — never hand-write mappers between layers.
- **JPA entities** belong only in `infrastructure/persistence/entity/`; domain models in `domain/model/` must be plain POJOs.
- Never call a `@Transactional` or `@Async` method from within the same bean (`this.foo()`) — use a separate injected bean so the Spring proxy can intercept the call.
- No `System.out.println` — use `@Slf4j`.
- No field injection (`@Autowired` on fields) — use constructor injection via `@RequiredArgsConstructor`.
- No `open-in-view` (keep it `false`) — load associations within transactions.
- Use `@Transactional(readOnly = true)` for read-only use-case methods.
- Use `FetchType.LAZY` for all `@ManyToOne` and `@OneToMany` relationships on JPA entities.
- Validation annotations (`@NotBlank`, `@NotEmpty`) belong **only** on HTTP DTOs in `infrastructure/web/dto/request/` (or `shared/`), not on entities or domain models.
- Keep controllers thin — one method per endpoint, immediate delegation to the use case.
- Prefer streams and method references over imperative loops.
- Add comments only when the **why** is non-obvious; omit them otherwise.

---

## 15. Spring Security

### 15.1 JWT with HttpOnly Cookies

For browser-facing APIs, store the JWT in an **HttpOnly cookie** rather than requiring an `Authorization` header. Browsers send cookies automatically with all same-origin requests — including `<img src="...">` and `EventSource` — whereas custom headers are silently dropped by those APIs.

**Login — set the cookie:**

```java
ResponseCookie cookie = ResponseCookie.from("jwt", token)
        .httpOnly(true)
        .path("/")
        .sameSite("Strict")
        .maxAge(Duration.between(Instant.now(), expiresAt))
        .build();
response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
```

**Logout — clear the cookie:**

```java
ResponseCookie cookie = ResponseCookie.from("jwt", "")
        .httpOnly(true).path("/").sameSite("Strict").maxAge(0).build();
response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
```

**Filter — read from cookie only:**

```java
private String resolveToken(HttpServletRequest request) {
    if (request.getCookies() == null) return null;
    return Arrays.stream(request.getCookies())
            .filter(c -> "jwt".equals(c.getName()))
            .map(Cookie::getValue)
            .findFirst().orElse(null);
}
```

### 15.2 SseEmitter + Spring Security Async Dispatch

When `SseEmitter` writes events, Tomcat creates a secondary async dispatch thread. Spring Security's filter chain re-runs on that thread, but `SecurityContextHolder` is thread-local and empty → `AuthorizationDeniedException` with "response is already committed".

**Fix:** permit all `ASYNC` dispatcher types before the other rules:

```java
import jakarta.servlet.DispatcherType;

.authorizeHttpRequests(auth -> auth
        .dispatcherTypeMatchers(DispatcherType.ASYNC).permitAll()   // must be first
        .requestMatchers("/api/auth/login", "/api/auth/logout").permitAll()
        .requestMatchers("/api/**").authenticated()
        .anyRequest().permitAll()
)
```

### 15.3 SecurityConfig Circular Dependency

Extract `UserDetailsService` and `PasswordEncoder` beans into a separate `@Configuration` class (`UserConfig`) to break the cycle between `SecurityConfig` and `JwtAuthenticationFilter`.

### 15.4 CORS: Include All HTTP Methods in Use

```java
config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
```

Always keep this list in sync with the HTTP methods actually used by the API.

---

