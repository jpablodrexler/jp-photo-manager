# Feature 39 — api-rate-limiting

Add `bucket4j-spring-boot-starter` with per-IP token-bucket limits on `POST /api/auth/login` (brute-force prevention) and `GET /api/assets/catalog` (prevent accidental concurrent runs from multiple browser tabs); returns `429 Too Many Requests` with a `Retry-After` header when the bucket is exhausted; no schema change required
