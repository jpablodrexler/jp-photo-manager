# Feature 39 — api-rate-limiting

**As a** administrator, **I want** rate limits on login and cataloging, **so that** brute-force attempts and accidental concurrent catalog runs are stopped.

## Acceptance Criteria
- AC1: Given a client within the limit, when it logs in, then the request succeeds.
- AC2: Given a client over the login limit, when it logs in, then it receives 429 Too Many Requests with a Retry-After header.
- AC3: Given a client over the catalog limit, when it starts a catalog run, then it receives 429 with a Retry-After header.
- AC4: Given limits are per IP, when two clients call, then one does not exhaust the other's bucket.

## Technical notes

Add `bucket4j-spring-boot-starter` with per-IP token-bucket limits on `POST /api/auth/login` (brute-force prevention) and `GET /api/assets/catalog` (prevent accidental concurrent runs from multiple browser tabs); returns `429 Too Many Requests` with a `Retry-After` header when the bucket is exhausted; no schema change required

## Definition of Done

Project-level Definition of Done applies (`definition-of-done.md`). Additional for this feature:
- None
