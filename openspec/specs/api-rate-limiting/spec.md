# api-rate-limiting

The login and catalog endpoints enforce per-IP rate limits. Clients exceeding the limit receive a `429 Too Many Requests` response with a `Retry-After` header indicating when they may retry.

---

## Requirements

### Requirement: Login endpoint is rate-limited to 10 requests per minute per IP

`POST /api/auth/login` SHALL reject requests from a client IP that has exceeded 10 requests within the current 60-second window with `429 Too Many Requests`.

#### Scenario: Login succeeds within the rate limit

- **GIVEN** a client IP that has made 9 login attempts in the current minute
- **WHEN** the client makes a 10th login attempt
- **THEN** the request is processed normally (success or 401 depending on credentials)

#### Scenario: Login is rejected when rate limit is exceeded

- **GIVEN** a client IP that has made 10 login attempts in the current minute
- **WHEN** the client makes an 11th login attempt
- **THEN** the response is `429 Too Many Requests` with a `Retry-After` header and body `{ "status": 429, "message": "Too many requests. Please try again later.", "timestamp": "..." }`

### Requirement: Catalog endpoint is rate-limited to 5 requests per hour per IP

`POST /api/assets/catalog` SHALL reject requests from a client IP that has exceeded 5 requests within the current hour with `429 Too Many Requests`.

#### Scenario: Catalog is rejected when rate limit is exceeded

- **GIVEN** a client IP that has triggered catalog 5 times in the current hour
- **WHEN** the client triggers catalog a 6th time
- **THEN** the response is `429 Too Many Requests` with a `Retry-After` header indicating seconds until the next refill

### Requirement: Rate-limited responses include a Retry-After header

All `429 Too Many Requests` responses SHALL include a `Retry-After` header with the number of seconds until the rate limit resets.

#### Scenario: Retry-After header is present on 429 response

- **GIVEN** a client that has exceeded the login rate limit
- **WHEN** the 429 response is received
- **THEN** the response headers include `Retry-After: <positive integer>`
