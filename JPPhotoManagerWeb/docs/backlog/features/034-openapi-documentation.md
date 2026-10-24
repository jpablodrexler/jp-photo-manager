# Feature 34 — openapi-documentation

**As a** developer integrating with the API, **I want** live API documentation with Swagger UI and an OpenAPI spec, **so that** I can discover and try the endpoints without reading the code.

## Acceptance Criteria
- AC1: Given an unauthenticated request, when it opens /swagger-ui.html or /v3/api-docs, then it succeeds.
- AC2: Given the asset, home and media controllers, when the spec is read, then every operation is documented with its response codes.
- AC3: Given the dependency is added, when the build runs, then it succeeds.

## Technical notes

Add `springdoc-openapi-starter-webmvc-ui` to `pom.xml`; annotate all `@RestController` classes with `@Operation` and `@ApiResponse`; exposes live Swagger UI at `/swagger-ui.html` and a machine-readable spec at `/v3/api-docs`; no schema change required; `/swagger-ui.html` is exempted from JWT authentication in `SecurityConfig`

## Definition of Done

Project-level Definition of Done applies (`definition-of-done.md`). Additional for this feature:
- None
