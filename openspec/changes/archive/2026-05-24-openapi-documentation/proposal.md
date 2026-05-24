## Why

The backend has 11 `@RestController` classes with no API documentation. Developers working on the frontend or integrating with the API must read source code to understand the available endpoints, their parameters, and their response shapes. Adding `springdoc-openapi-starter-webmvc-ui` generates a live Swagger UI at `/swagger-ui.html` and a machine-readable OpenAPI spec at `/v3/api-docs` with zero configuration beyond the dependency.

## What Changes

- Add `springdoc-openapi-starter-webmvc-ui` to `pom.xml`
- Annotate all 11 `@RestController` classes with `@Tag`, `@Operation`, and `@ApiResponse`
- Exempt `/swagger-ui.html` and `/v3/api-docs/**` from JWT authentication in `SecurityConfig`
- No schema change required

## Capabilities

### New Capabilities

- `openapi-documentation`: A live Swagger UI is available at `/swagger-ui.html`; a machine-readable OpenAPI 3.0 spec is available at `/v3/api-docs`. All 11 REST controllers are documented with operation summaries and response codes.

### Modified Capabilities

_(none)_

## Impact

- `JPPhotoManagerWeb/backend/pom.xml` — add `springdoc-openapi-starter-webmvc-ui` dependency
- `JPPhotoManagerWeb/backend/src/main/java/com/jpablodrexler/photomanager/config/SecurityConfig.java` — permit `/swagger-ui.html`, `/swagger-ui/**`, `/v3/api-docs/**`
- All 11 `@RestController` files — add `@Tag`, `@Operation`, `@ApiResponse` annotations
