# Feature 34 — openapi-documentation

Add `springdoc-openapi-starter-webmvc-ui` to `pom.xml`; annotate all `@RestController` classes with `@Operation` and `@ApiResponse`; exposes live Swagger UI at `/swagger-ui.html` and a machine-readable spec at `/v3/api-docs`; no schema change required; `/swagger-ui.html` is exempted from JWT authentication in `SecurityConfig`
