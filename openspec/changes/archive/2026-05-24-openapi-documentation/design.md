## Context

The backend has the following `@RestController` classes: `AssetController`, `FolderController`, `AuthController`, `UserAdminController`, `AlbumController`, `SyncController`, `ConvertController`, `RecycleBinController`, `TagController`, `SearchPresetController`, `HomeController`, and `MediaController`. None have Swagger/OpenAPI annotations. Note: there is no standalone `DuplicatesController`; the duplicates endpoint lives in `AssetController`. `springdoc-openapi` auto-discovers these controllers and generates a spec; annotations add summaries, descriptions, and explicit response codes.

## Goals / Non-Goals

**Goals:**
- Add `springdoc-openapi-starter-webmvc-ui` to `pom.xml`
- Annotate all 13 controllers with `@Tag` (controller-level), `@Operation` (method-level summary), and `@ApiResponse` (explicit 200/400/401/404 responses)
- Exempt Swagger UI and spec endpoints from JWT auth in `SecurityConfig`

**Non-Goals:**
- Documenting request/response schemas in detail (SpringDoc generates these automatically from Java types)
- Generating a client SDK from the spec
- Adding authentication to the Swagger UI (it will be publicly accessible on `/swagger-ui.html`)

## Decisions

### 1. `springdoc-openapi-starter-webmvc-ui` (not `springfox`)

**Decision:** Use `org.springdoc:springdoc-openapi-starter-webmvc-ui` which is compatible with Spring Boot 3.x.

**Rationale:** Springfox is deprecated and does not support Spring Boot 3.x or Spring Security 6.x. SpringDoc is the actively maintained alternative with first-class Spring Boot 3 support.

### 2. Exempt Swagger UI from JWT auth

**Decision:** Add `.requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()` to `SecurityConfig`.

**Rationale:** The Swagger UI must be accessible without authentication so developers can browse the spec. The spec itself does not expose sensitive data — it only describes endpoint shapes.

**Alternative considered:** Requiring authentication for the Swagger UI. Rejected because it makes the UI inaccessible in environments where the JWT flow has not been set up yet.

### 3. Annotations on controllers only (not use cases)

**Decision:** Apply OpenAPI annotations exclusively to `@RestController` classes. Do not annotate use case interfaces or domain models.

**Rationale:** OpenAPI describes the HTTP interface, not internal domain logic. Keeping annotations in the controller layer maintains clean hexagonal boundaries.

## Risks / Trade-offs

| Risk | Severity | Mitigation |
|---|---|---|
| Swagger UI exposes API structure to unauthenticated users | Low | Acceptable for a self-hosted app; no sensitive business data is in the spec |
| Annotation maintenance burden as endpoints evolve | Low | SpringDoc auto-generates schemas from Java types; only summaries need manual updates |
