## 1. Backend — Database migration

- [x] 1.1 Create `V9__add_refresh_tokens.sql` in `backend/src/main/resources/db/migration/`; create table `refresh_tokens` with columns: `token_id BIGSERIAL PRIMARY KEY`, `user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE`, `token TEXT NOT NULL`, `expires_at TIMESTAMPTZ NOT NULL`, `revoked BOOLEAN NOT NULL DEFAULT FALSE`, `issued_at TIMESTAMPTZ NOT NULL DEFAULT NOW()`; add unique index `uq_refresh_tokens_token ON refresh_tokens(token)`; add index `ix_refresh_tokens_user_id ON refresh_tokens(user_id)`
- [x] 1.2 Add property `photomanager.refresh-token-expiry-days: 30` to `src/main/resources/application.yml` under the `photomanager:` block

## 2. Backend — Entity and Repository

- [x] 2.1 Create `RefreshToken.java` in `domain/entity/`; annotate `@Entity @Table(name = "refresh_tokens") @Data @NoArgsConstructor`; fields: `@Id @GeneratedValue(strategy = GenerationType.IDENTITY) Long tokenId`, `@ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "user_id", nullable = false) User user`, `@Column(nullable = false, unique = true) String token`, `@Column(nullable = false) Instant expiresAt`, `@Column(nullable = false) boolean revoked`, `@Column(nullable = false, updatable = false) Instant issuedAt`
- [x] 2.2 Create `RefreshTokenRepository.java` in `domain/repository/` extending `JpaRepository<RefreshToken, Long>`; add `Optional<RefreshToken> findByToken(String token)` and `void deleteByUser_Id(UUID userId)`

## 3. Backend — Domain service

- [x] 3.1 Create `RefreshTokenService.java` interface in `domain/service/` with methods:
  - `String issueRefreshToken(String username)` — creates a new token row and returns the raw token value
  - `RotatedToken validateAndRotate(String tokenValue)` — validates, marks revoked, issues new; throws `InvalidRefreshTokenException` if invalid or revoked or expired
  - `void revokeAllForUser(String username)` — deletes all token rows for the user
  - Define inner record `RotatedToken(String newTokenValue, String username, Instant newExpiresAt)` in the same file
- [x] 3.2 Create `InvalidRefreshTokenException.java` in `api/exception/` as a `RuntimeException` subclass with a no-arg constructor setting message `"Refresh token is invalid, expired, or revoked"`
- [x] 3.3 Create `RefreshTokenServiceImpl.java` in `infrastructure/service/`; annotate `@Service @RequiredArgsConstructor @Slf4j`; inject `RefreshTokenRepository refreshTokenRepository`, `UserRepository userRepository`; read `@Value("${photomanager.refresh-token-expiry-days}") int refreshTokenExpiryDays`
- [x] 3.4 Implement `issueRefreshToken(String username)`: look up `User` by username (throw `IllegalArgumentException` if not found); generate 256-bit random value with `SecureRandom` + `Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)`; construct `RefreshToken` with `user`, `token`, `expiresAt = Instant.now().plus(refreshTokenExpiryDays, ChronoUnit.DAYS)`, `revoked = false`, `issuedAt = Instant.now()`; save; return raw token value
- [x] 3.5 Implement `validateAndRotate(String tokenValue)` annotated `@Transactional`: call `refreshTokenRepository.findByToken(tokenValue)` — throw `InvalidRefreshTokenException` if empty; throw `InvalidRefreshTokenException` if `revoked` is true or `expiresAt.isBefore(Instant.now())`; mark existing token `revoked = true` and save; call `issueRefreshToken(user.getUsername())` to create the new token; return `RotatedToken(newToken, username, newExpiresAt)`
- [x] 3.6 Implement `revokeAllForUser(String username)` annotated `@Transactional`: look up user by username; call `refreshTokenRepository.deleteByUser_Id(user.getId())`

## 4. Backend — AuthController updates

- [x] 4.1 Inject `RefreshTokenService refreshTokenService` into `AuthController`
- [x] 4.2 Update `login()`: after getting the JWT `token` from `userService.authenticate()`, call `refreshTokenService.issueRefreshToken(request.username())` to get `refreshTokenValue`; build a second `ResponseCookie.from("refreshToken", refreshTokenValue)` with `.httpOnly(true).path("/api/auth/refresh").sameSite("Strict").maxAge(Duration.ofDays(30))`; add both cookies to the response; return `200 OK` with `LoginResponse`
- [x] 4.3 Add `POST /api/auth/refresh` handler `refresh(HttpServletRequest request, HttpServletResponse response)`: extract the `refreshToken` cookie value from `request.getCookies()` — return `401` if absent; call `refreshTokenService.validateAndRotate(tokenValue)` — catch `InvalidRefreshTokenException` and return `401`; call `jwtTokenService.generateToken(rotated.username())` for the new JWT; build and set both new cookies; return `200 OK` with `LoginResponse(rotated.username(), newJwtExpiry)`
- [x] 4.4 Update `logout()`: extract `refreshToken` cookie from the request; if present, call `refreshTokenService.revokeAllForUser(username)` — resolve username via `jwtTokenService.extractUsername` on the `jwt` cookie, or fall back to the `refreshToken` row's username; clear both `jwt` and `refreshToken` cookies (MaxAge=0, matching paths); return `200 OK`

## 5. Backend — SecurityConfig update

- [x] 5.1 In `SecurityConfig.securityFilterChain()`, add `"/api/auth/refresh"` to the `.requestMatchers(...).permitAll()` list alongside `"/api/auth/login"` and `"/api/auth/logout"`

## 6. Backend — Tests

- [x] 6.1 Create `RefreshTokenServiceTest` (`@ExtendWith(MockitoExtension.class)`): mock `RefreshTokenRepository` and `UserRepository`; test `issueRefreshToken` saves a token row with correct `expiresAt` and returns a non-blank string; test `validateAndRotate` with a valid token marks old as revoked and returns a `RotatedToken`; test `validateAndRotate` with a revoked token throws `InvalidRefreshTokenException`; test `validateAndRotate` with an expired token throws `InvalidRefreshTokenException`
- [x] 6.2 Create `AuthControllerRefreshTest` (`@WebMvcTest(AuthController.class)`): mock `UserService`, `JwtTokenService`, `RefreshTokenService`; assert `POST /api/auth/refresh` with a valid `refreshToken` cookie returns `200` and sets two new cookies; assert `POST /api/auth/refresh` without a `refreshToken` cookie returns `401`; assert `POST /api/auth/refresh` when `validateAndRotate` throws `InvalidRefreshTokenException` returns `401`
- [x] 6.3 Add test to `AuthControllerRefreshTest`: `POST /api/auth/login` sets both `jwt` and `refreshToken` cookies; `POST /api/auth/logout` clears both cookies
- [x] 6.4 Run `mvn test` and confirm all tests pass

## 7. Frontend — AuthService updates

- [x] 7.1 Add `private refreshTimer: ReturnType<typeof setTimeout> | null = null` field to `AuthService`
- [x] 7.2 Add `refresh(): Observable<void>` method: `POST /api/auth/refresh` with empty body; on success call `storeSession(res.username, res.expiresAt)` and `scheduleProactiveRefresh()`; `map(() => undefined)`
- [x] 7.3 Add `scheduleProactiveRefresh()` method: read `expiresAt` from `localStorage`; compute `delay = expiresAt - Date.now() - 5 * 60 * 1000`; if `delay > 0` clear existing timer and set `this.refreshTimer = setTimeout(() => this.refresh().subscribe(), delay)` 
- [x] 7.4 Update `login()`: after `tap(res => this.storeSession(...))` add a second `tap(() => this.scheduleProactiveRefresh())`
- [x] 7.5 Update `clearSession()`: cancel the proactive timer (`if (this.refreshTimer) clearTimeout(this.refreshTimer); this.refreshTimer = null`) before removing from `localStorage`

## 8. Frontend — AuthInterceptor updates

- [x] 8.1 Replace the body of `authInterceptor` with refresh-then-retry logic: on 401, if `!req.url.includes('/api/auth/login') && !req.url.includes('/api/auth/refresh')`, call `inject(AuthService).refresh()` then `switchMap(() => next(req.clone()))`; in the `catchError` branch of the refresh attempt, call `inject(AuthService).clearSession()` and `inject(Router).navigateByUrl('/login')` then `throwError(() => error)`; on 401 from `/api/auth/refresh` itself, fall through directly to `clearSession` + navigate
- [x] 8.2 Import `switchMap` from `rxjs/operators` in `auth.interceptor.ts`

## 9. Frontend — Tests

- [x] 9.1 In a new `auth.interceptor.cy.ts` (or as part of `auth.service.cy.ts`): stub `AuthService.refresh()` to return `of(undefined)`; simulate a 401 response from a non-auth endpoint; assert the interceptor calls `refresh()` and retries the original request
- [x] 9.2 Add test: when `refresh()` returns an error, assert `clearSession()` is called and the router navigates to `/login`
- [x] 9.3 Add test to the existing auth service tests: `scheduleProactiveRefresh()` called after login schedules a timer; `clearSession()` cancels the timer
- [x] 9.4 Run `npm test` and confirm all tests pass
