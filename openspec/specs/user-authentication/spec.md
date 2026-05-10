## ADDED Requirements

### Requirement: User registration creates a new account

The backend SHALL expose `POST /api/auth/register` accepting `{ "username": string, "password": string }`. On success it SHALL persist a new user with a BCrypt-hashed password and return HTTP 201. If the username already exists it SHALL return HTTP 409. Usernames SHALL be normalized to lowercase before storage and uniqueness checks.

#### Scenario: Successful registration
- **WHEN** `POST /api/auth/register` is called with a unique username and a non-empty password
- **THEN** the response status is 201
- **AND** a new row exists in the `users` table with the normalized username and a BCrypt hash of the password

#### Scenario: Duplicate username returns 409
- **WHEN** `POST /api/auth/register` is called with a username that already exists (case-insensitive)
- **THEN** the response status is 409
- **AND** no new row is inserted in the `users` table

#### Scenario: Empty username or password returns 400
- **WHEN** `POST /api/auth/register` is called with an empty username or an empty password
- **THEN** the response status is 400

### Requirement: User login returns a JWT

The backend SHALL expose `POST /api/auth/login` accepting `{ "username": string, "password": string }`. On success it SHALL return HTTP 200 with `{ "token": "<jwt>" }`. The JWT SHALL expire after `photomanager.jwt-expiry-hours` hours (default 24). On invalid credentials it SHALL return HTTP 401.

#### Scenario: Successful login
- **WHEN** `POST /api/auth/login` is called with valid credentials
- **THEN** the response status is 200
- **AND** the response body contains a `token` field with a signed JWT

#### Scenario: Invalid password returns 401
- **WHEN** `POST /api/auth/login` is called with a correct username but wrong password
- **THEN** the response status is 401

#### Scenario: Unknown username returns 401
- **WHEN** `POST /api/auth/login` is called with a username that does not exist
- **THEN** the response status is 401

### Requirement: All API endpoints require authentication

The backend SHALL reject requests to `/api/**` (except `/api/auth/register` and `/api/auth/login`) that do not carry a valid JWT in the `Authorization: Bearer <token>` header with HTTP 401.

#### Scenario: Request without token is rejected
- **WHEN** a request is made to `/api/assets` without an `Authorization` header
- **THEN** the response status is 401

#### Scenario: Request with valid token is allowed
- **WHEN** a request is made to `/api/assets` with a valid `Authorization: Bearer <token>` header
- **THEN** the request proceeds normally

#### Scenario: Request with expired token is rejected
- **WHEN** a request is made with an expired JWT
- **THEN** the response status is 401

### Requirement: Frontend redirects unauthenticated users to login

The Angular application SHALL redirect any navigation to a protected route to `/login` when no valid JWT is stored in localStorage. After a successful login the user SHALL be redirected to the originally requested route (or `/gallery` if no prior route exists).

#### Scenario: Unauthenticated access redirects to login
- **WHEN** a user navigates to `/gallery` without a stored JWT
- **THEN** the Angular router redirects to `/login`

#### Scenario: Post-login redirect
- **WHEN** a user logs in successfully after being redirected from `/gallery`
- **THEN** the Angular router navigates to `/gallery`

### Requirement: Frontend attaches JWT to all API requests

The Angular HTTP interceptor SHALL add `Authorization: Bearer <token>` to every outgoing HTTP request when a token is stored. Requests to the login and register endpoints SHALL be sent without a token.

#### Scenario: API request includes Authorization header
- **WHEN** an authenticated user's component makes an HTTP request via `HttpClient`
- **THEN** the request contains an `Authorization: Bearer <token>` header

### Requirement: JWT secret must not be blank at startup

The `photomanager.jwt-secret` property SHALL be declared as an empty string in the committed `application.yml`. The actual secret SHALL be supplied via `application-local.yml` (git-ignored). `JwtUtil` SHALL validate that the secret is non-blank on application startup and throw `IllegalStateException` if it is empty, ensuring misconfigured instances fail fast.

#### Scenario: Application fails to start with blank secret
- **WHEN** the application starts with `photomanager.jwt-secret` equal to an empty string
- **THEN** startup fails with `IllegalStateException` containing a message directing the administrator to set the secret in `application-local.yml`

#### Scenario: Application starts with a non-blank secret
- **WHEN** the application starts with a non-empty `photomanager.jwt-secret`
- **THEN** startup succeeds normally

### Requirement: Frontend provides Login and Register pages

The Angular application SHALL provide a `LoginComponent` at `/login` and a `RegisterComponent` at `/register`. Both SHALL display a form with username and password fields. The Login page SHALL include a link to Register and vice versa. Both SHALL display an error message when the backend returns an error.

#### Scenario: Login form submits credentials
- **WHEN** a user enters a username and password and submits the Login form
- **THEN** `AuthService.login()` is called with those credentials

#### Scenario: Register form submits credentials
- **WHEN** a user enters a username and password and submits the Register form
- **THEN** `AuthService.register()` is called with those credentials

#### Scenario: Login error is displayed
- **WHEN** the backend returns 401 on login
- **THEN** the Login page displays an error message

#### Scenario: Register duplicate username error is displayed
- **WHEN** the backend returns 409 on registration
- **THEN** the Register page displays an error message indicating the username is taken

### Requirement: Home page is the authenticated landing page

The Angular application SHALL provide a `HomeComponent` at `/home` (protected by `AuthGuard`). After a successful login the user SHALL be navigated to `/home` (or to `returnUrl` if set). The root path (`/`) SHALL redirect to `/home`.

#### Scenario: Post-login lands on home
- **WHEN** a user logs in successfully with no prior `returnUrl`
- **THEN** the Angular router navigates to `/home`

#### Scenario: Root path redirects to home
- **WHEN** a user navigates to `/`
- **THEN** the Angular router redirects to `/home`

### Requirement: Home page displays catalog statistics

The backend SHALL expose `GET /api/home/stats` returning `{ folderCount, assetCount, lastCatalogCompletedAt }`. `lastCatalogCompletedAt` SHALL be null when no catalog run has ever completed.

The `HomeComponent` SHALL display:
- The application name ("JP Photo Manager")
- The number of catalogued folders
- The number of catalogued assets
- The last successful catalog completion time; if null, display "Never"

#### Scenario: Stats displayed on home
- **WHEN** `HomeComponent` initialises
- **THEN** it calls `HomeService.getStats()`
- **AND** displays folder count, asset count, and last catalog time

#### Scenario: No completed catalog shows "Never"
- **WHEN** `HomeService.getStats()` returns `lastCatalogCompletedAt: null`
- **THEN** the template displays "Never" for the last catalog time
