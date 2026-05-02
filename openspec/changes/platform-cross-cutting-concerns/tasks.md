## 1. Structured JSON Logging

- [x] 1.1 Add `net.logstash.logback:logstash-logback-encoder:8.0` dependency to `JPPhotoManagerWeb/backend/pom.xml`
- [x] 1.2 Create `src/main/resources/logback-spring.xml` with a `RollingFileAppender` using `LogstashEncoder` (path: `${user.home}/.photomanager/logs/photomanager.log`, daily rolling, 30-day retention) and a `ConsoleAppender` using `PatternLayoutEncoder` for plain-text output
- [x] 1.3 Remove `logging.file.name` and `logging.level` entries from `application.yml` (now owned by `logback-spring.xml`); keep only entries that logback-spring.xml cannot express
- [x] 1.4 Add a **Logging** section to `JPPhotoManagerWeb/README.md` documenting the log file path, JSON format, and 30-day rolling retention

## 2. Global Exception Handler

- [x] 2.1 Create `ErrorResponse` record in `api/` with fields: `String timestamp`, `int status`, `String error`, `String message`
- [x] 2.2 Create `GlobalExceptionHandler` in `api/` annotated `@RestControllerAdvice`:
  - `handleEntityNotFound(EntityNotFoundException)` → 404
  - `handleIllegalArgument(IllegalArgumentException)` → 400
  - `handleGeneric(Exception)` → 500, log full stack trace at ERROR
- [x] 2.3 Create `GlobalExceptionHandlerTest` in `api/` covering all three handlers: verify status code, response body fields (`status`, `error`, `message`, `timestamp`)

## 3. Dependencies — Security and JWT

- [x] 3.1 Add `spring-boot-starter-security` to `pom.xml`
- [x] 3.2 Add `io.jsonwebtoken:jjwt-api`, `io.jsonwebtoken:jjwt-impl`, `io.jsonwebtoken:jjwt-jackson` (version 0.12.x) to `pom.xml`
- [x] 3.3 Add `spring-boot-starter-validation` to `pom.xml` (for `@Valid` on auth request bodies)
- [x] 3.4 Add to `application.yml`: `photomanager.jwt-secret: ""` (empty placeholder, with inline comment pointing to application-local.yml) and `photomanager.jwt-expiry-hours: 24`
- [x] 3.5 Create `src/main/resources/application-local.yml.example` showing how to set `photomanager.jwt-secret` with a note that the value is generated via `openssl rand -base64 32`

## 4. Users Table and Domain

- [x] 4.1 Create `V4__add_users.sql` in `src/main/resources/db/migration/` with: `CREATE TABLE users (id UUID PRIMARY KEY DEFAULT gen_random_uuid(), username VARCHAR(50) NOT NULL UNIQUE, password_hash VARCHAR(255) NOT NULL, created_at TIMESTAMPTZ NOT NULL DEFAULT now())`
- [x] 4.2 Create `User` JPA entity in `domain/entity/` with fields: `id` (UUID), `username` (String), `passwordHash` (String), `createdAt` (Instant)
- [x] 4.3 Create `UserRepository` in `domain/repository/` extending `JpaRepository<User, UUID>` with `Optional<User> findByUsername(String username)`

## 5. UserService

- [x] 5.1 Create `UserService` interface in `domain/service/` with `void register(String username, String password)` and `String authenticate(String username, String password)` (returns JWT on success)
- [x] 5.2 Create `UserServiceImpl` in `infrastructure/service/` annotated `@Service`:
  - Inject `UserRepository`, `BCryptPasswordEncoder`, `JwtUtil`
  - `register`: lowercase username, check uniqueness (throw `IllegalArgumentException` on duplicate), hash password, save
  - `authenticate`: find user by lowercase username, verify BCrypt match (throw `BadCredentialsException` on failure), return JWT from `JwtUtil`
- [x] 5.3 Create `UserServiceImplTest` covering: `register_newUser_savesWithHashedPassword`, `register_duplicateUsername_throwsIllegalArgumentException`, `authenticate_validCredentials_returnsToken`, `authenticate_invalidPassword_throwsBadCredentialsException`, `authenticate_unknownUsername_throwsBadCredentialsException`

## 6. JwtUtil and Security Config

- [x] 6.1 Create `JwtUtil` in `infrastructure/service/` (or `config/`):
  - `@Value` fields: `jwtSecret`, `jwtExpiryHours`
  - `@PostConstruct init()` — throw `IllegalStateException` if `jwtSecret` is blank ("photomanager.jwt-secret must not be blank — set it in application-local.yml")
  - `String generateToken(String username)` — creates HS256 JWT with subject = username, expiry = now + hours
  - `String extractUsername(String token)` — parses and validates token, returns subject
  - `boolean isTokenValid(String token)` — returns false on expired or malformed token
- [x] 6.2 Create `JwtAuthenticationFilter` extending `OncePerRequestFilter`:
  - Extract `Authorization: Bearer <token>` header
  - If present and valid: call `JwtUtil.extractUsername`, load user details, set `UsernamePasswordAuthenticationToken` in `SecurityContextHolder`
- [x] 6.3 Create `SecurityConfig` in `config/` annotated `@Configuration @EnableWebSecurity`:
  - `SecurityFilterChain` bean: permit `/api/auth/**`, require authentication for `/api/**`, stateless session, add `JwtAuthenticationFilter` before `UsernamePasswordAuthenticationFilter`
  - `BCryptPasswordEncoder` bean (strength 12)
  - `UserDetailsService` bean loading from `UserRepository`

## 7. AuthController

- [x] 7.1 Create `AuthRequest` record in `api/` with `@NotBlank String username` and `@NotBlank String password`
- [x] 7.2 Update `AuthController`: only `POST /login` (remove `POST /register`); update `SecurityConfig` to permitAll only `/api/auth/login`
- [x] 7.3 Update `AuthControllerTest`: only `login_validCredentials_returnsToken`, `login_invalidCredentials_returns401` (remove register tests)

## 7b. Backend — User Administration

- [x] 7b.1 Create `V5__seed_default_admin.sql` Flyway migration (no-op SQL comment); add `DataInitializer` component (`@Component @Profile("!test")`) that on `ApplicationReadyEvent` checks `userRepository.count() == 0` and calls `userService.register("admin", "admin")`, logging a warning to change the default password
- [x] 7b.2 Create `UserSummary` record in `application/dto/` with `UUID id`, `String username`, `Instant createdAt`
- [x] 7b.3 Create `CreateUserRequest` record in `api/` with `@NotBlank String username`, `@NotBlank String password`; create `UpdatePasswordRequest` record in `api/` with `@NotBlank String password`
- [x] 7b.4 Create `UserAdminService` interface in `domain/service/` with `List<UserSummary> listUsers()`, `UserSummary createUser(String username, String password)`, `void updatePassword(UUID id, String newPassword)`, `void deleteUser(UUID id)`
- [x] 7b.5 Create `UserAdminServiceImpl` in `infrastructure/service/`: `listUsers()` returns all users mapped to `UserSummary`; `createUser()` delegates to `userService.register` then returns summary; `updatePassword()` finds user by id, encodes new password, saves; `deleteUser()` deletes by id
- [x] 7b.6 Create `UserAdminController` in `api/` at `@RequestMapping("/api/admin/users")`: `GET /` → list, `POST /` → create (201), `PATCH /{id}/password` → update password (200), `DELETE /{id}` → delete (204)
- [x] 7b.7 Create `UserAdminControllerTest` covering: `listUsers_returnsUsers`, `createUser_validRequest_returns201`, `updatePassword_returns200`, `deleteUser_returns204`

## 8. Frontend — AuthService and Infrastructure

- [x] 8.1 Create `src/app/core/services/auth.service.ts`:
  - `login(username, password)` → POST `/api/auth/login`, store token in `localStorage`
  - `logout()` → remove token from `localStorage`
  - `isLoggedIn()` → check token exists and is not expired (decode JWT payload, check `exp`)
  - `getToken()` → return stored token or null
- [x] 8.2 Create `src/app/core/guards/auth.guard.ts` implementing `CanActivateFn`: redirect to `/login` (preserving `returnUrl`) if `authService.isLoggedIn()` is false
- [x] 8.3 Update `src/app/core/interceptors/auth.interceptor.ts`: remove `/api/auth/register` from PUBLIC_URLS (only `/api/auth/login` is public)
- [x] 8.4 Register the interceptor in `app.config.ts` using `withInterceptors([authInterceptor])`

## 9. Frontend — Login Component

- [x] 9.1 Create `src/app/features/auth/login/login.component.ts` (standalone): form with `username` and `password` fields; on submit call `authService.login()`; on success navigate to `returnUrl` or `/home`; on error show inline error message
- [x] 9.2 Update `src/app/features/auth/login/login.component.html`: remove "Don't have an account? Register" link
- [x] 9.3 Create `src/app/features/auth/login/login.component.scss` with basic centred-card layout

## 9b. Frontend — User Admin Component

- [x] 9b.1 Create `UserAdmin` interface in `core/models/` with `id: string`, `username: string`, `createdAt: string`
- [x] 9b.2 Create `UserAdminService` in `core/services/` with `getUsers(): Observable<UserAdmin[]>`, `createUser(username, password): Observable<UserAdmin>`, `updatePassword(id, password): Observable<void>`, `deleteUser(id): Observable<void>`
- [x] 9b.3 Create `src/app/features/admin/users/user-admin.component.ts` (standalone): inject `UserAdminService`; load users in `ngOnInit`; support add dialog, change password dialog, delete confirmation; import `MatTableModule`, `MatButtonModule`, `MatDialogModule`, `MatFormFieldModule`, `MatInputModule`
- [x] 9b.4 Create `src/app/features/admin/users/user-admin.component.html`: Material table with columns (username, createdAt, actions); FAB or button to add user; inline edit password; delete button with confirmation
- [x] 9b.5 Create `src/app/features/admin/users/user-admin.component.scss`

## 10. Frontend — Routes and Guards

- [x] 10.1 Add routes to `app.routes.ts`:
  - `{ path: 'login', loadComponent: ... LoginComponent }`
  - `{ path: 'home', loadComponent: ... HomeComponent, canActivate: [authGuard] }`
  - `{ path: 'admin/users', loadComponent: ... UserAdminComponent, canActivate: [authGuard] }`
  - Add `canActivate: [authGuard]` to all existing protected routes (`gallery`, `sync`, `convert`, `duplicates`)
  - Change `{ path: '', redirectTo: 'home', pathMatch: 'full' }` (default redirect goes to home, not gallery)
- [x] 10.2 Update `app.component.html` navbar to include links for Home and User Admin

## 11. Frontend — Tests

- [x] 11.1 Create `login.component.cy.ts`: test `submit_validCredentials_callsAuthServiceLogin`; test `login_serverError_displaysErrorMessage`
- [x] 11.2 Create `user-admin.component.cy.ts`: test `ngOnInit_displaysUsers`; test `deleteUser_callsServiceDelete`

## 12. Documentation

- [x] 12.1 Add an **Authentication** section to `JPPhotoManagerWeb/README.md` describing:
  - The JWT flow (login → Bearer token on all `/api/**` calls)
  - The one public endpoint (`POST /api/auth/login`)
  - The configuration properties (`photomanager.jwt-secret`, `photomanager.jwt-expiry-hours`)
  - Default admin credentials (`admin` / `admin`) and instructions to change them via the User Admin page
  - A **Setup** sub-section explaining: (1) copy `application-local.yml.example` to `application-local.yml`, (2) generate a secret with `openssl rand -base64 32`, (3) paste it into `photomanager.jwt-secret`; warn that the app will fail to start if the secret is blank

## 13. Backend — Home Stats Endpoint

- [x] 13.1 Create `HomeStats` record in `application/dto/` with: `long folderCount`, `long assetCount`, `Instant lastCatalogCompletedAt` (nullable)
- [x] 13.2 Add `HomeStats getHomeStats()` to `PhotoManagerFacade` interface and implement in `PhotoManagerFacadeImpl`: call `folderRepository.count()`, `assetRepository.count()`, and `catalogRunStateRepository.findById(1).map(CatalogRunState::getLastCompletedAt).orElse(null)`
- [x] 13.3 Create `HomeController` in `api/` annotated `@RestController @RequestMapping("/api/home")` with `@GetMapping("/stats")` delegating to `facade.getHomeStats()`; permit this endpoint in `SecurityConfig` (add `/api/home/**` to `permitAll` or keep it under JWT auth — it is a protected endpoint)
- [x] 13.4 Create `HomeControllerTest` using `@WebMvcTest(HomeController.class)` covering: `getStats_returnsCountsAndLastCompleted` (mock facade returns populated HomeStats), `getStats_noCompletedCatalog_returnsNullTimestamp` (mock facade returns null lastCatalogCompletedAt)

## 14. Frontend — Home Component

- [x] 14.1 Create `HomeStats` interface in `core/models/` with `folderCount: number`, `assetCount: number`, `lastCatalogCompletedAt: string | null`
- [x] 14.2 Create `HomeService` in `core/services/` with `getStats(): Observable<HomeStats>` — `GET /api/home/stats`
- [x] 14.3 Create `src/app/features/home/home.component.ts` (standalone): inject `HomeService`, call `getStats()` in `ngOnInit`, expose result for the template; import `MatCardModule`, `MatIconModule`, `DatePipe` (or `AsyncPipe`)
- [x] 14.4 Create `src/app/features/home/home.component.html`: display "JP Photo Manager" heading; three Angular Material cards — folders catalogued (count + folder icon), assets catalogued (count + photo icon), last catalog completed (formatted date or "Never" when null)
- [x] 14.5 Create `src/app/features/home/home.component.scss`: centred, card grid layout matching the application's dark/green theme

## 15. Frontend — Home Tests

- [x] 15.1 Create `home.component.cy.ts`: test `ngOnInit_displaysFolderCount`; test `ngOnInit_displaysAssetCount`; test `ngOnInit_nullLastCompleted_displaysNever`
