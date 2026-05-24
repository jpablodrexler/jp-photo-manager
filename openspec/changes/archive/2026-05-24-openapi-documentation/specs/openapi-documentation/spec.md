# openapi-documentation

A live Swagger UI is available at `/swagger-ui.html` and a machine-readable OpenAPI 3.0 spec at `/v3/api-docs`. All REST controllers are annotated with operation summaries and response codes. Both endpoints are accessible without JWT authentication.

---

## ADDED Requirements

### Requirement: Swagger UI is accessible without authentication

`/swagger-ui.html` and `/v3/api-docs/**` SHALL be exempted from JWT authentication in `SecurityConfig` and SHALL return `200 OK` to unauthenticated requests.

#### Scenario: Unauthenticated request to Swagger UI succeeds

- **GIVEN** no JWT cookie is present
- **WHEN** a browser requests `/swagger-ui.html`
- **THEN** the response is `200 OK` with the Swagger UI HTML page

#### Scenario: Unauthenticated request to OpenAPI spec succeeds

- **GIVEN** no JWT cookie is present
- **WHEN** a client requests `GET /v3/api-docs`
- **THEN** the response is `200 OK` with a valid OpenAPI 3.0 JSON document

### Requirement: All REST controllers are documented

All 13 `@RestController` classes SHALL have a `@Tag` annotation with a meaningful name and description. Every `@RequestMapping` method SHALL have an `@Operation` annotation with a `summary` and explicit `@ApiResponse` annotations for each possible HTTP status code.

#### Scenario: AssetController operations are documented

- **WHEN** a developer views the Swagger UI
- **THEN** the "Assets" tag is present with at least 6 documented endpoints (list, thumbnail, image, delete, move, catalog)

#### Scenario: Each endpoint shows its response codes

- **WHEN** a developer expands the `GET /api/assets/{id}/image` endpoint in Swagger UI
- **THEN** `200 OK`, `401 Unauthorized`, and `404 Not Found` response codes are shown

#### Scenario: HomeController operations are documented

- **WHEN** a developer views the Swagger UI
- **THEN** the "Home" tag is present with the `GET /api/home/stats` endpoint documented

#### Scenario: MediaController operations are documented

- **WHEN** a developer views the Swagger UI
- **THEN** the "Media" tag is present with `GET /api/assets/{id}/stream` and `GET /api/audio/playlist/{id}` documented, including `206 Partial Content` for the stream endpoint

### Requirement: springdoc-openapi-starter-webmvc-ui is present in pom.xml

The `pom.xml` SHALL include `org.springdoc:springdoc-openapi-starter-webmvc-ui` with a version compatible with Spring Boot 3.4.

#### Scenario: Build succeeds with the new dependency

- **WHEN** `mvn clean package -DskipTests` is run
- **THEN** the build succeeds and `springdoc-openapi-starter-webmvc-ui` is present in the packaged JAR
