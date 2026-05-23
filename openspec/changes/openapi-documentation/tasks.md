## 1. Maven dependency

- [ ] 1.1 Add to `pom.xml`:
  ```xml
  <dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.x.x</version>
  </dependency>
  ```
  (use latest stable version compatible with Spring Boot 3.4)

## 2. SecurityConfig — exempt Swagger endpoints

- [ ] 2.1 Add `.requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()` to the `SecurityConfig` `securityFilterChain` before the authenticated matcher

## 3. Controller annotations

- [ ] 3.1 Annotate `AssetController` with `@Tag(name = "Assets")` and add `@Operation` + `@ApiResponse` to each method
- [ ] 3.2 Annotate `FolderController` with `@Tag(name = "Folders")`
- [ ] 3.3 Annotate `AuthController` with `@Tag(name = "Authentication")`
- [ ] 3.4 Annotate `UserController` with `@Tag(name = "User Administration")`
- [ ] 3.5 Annotate `AlbumController` with `@Tag(name = "Albums")`
- [ ] 3.6 Annotate `SyncController` with `@Tag(name = "Sync")`
- [ ] 3.7 Annotate `ConvertController` with `@Tag(name = "Convert")`
- [ ] 3.8 Annotate `DuplicatesController` (or equivalent) with `@Tag(name = "Duplicates")`
- [ ] 3.9 Annotate `RecycleBinController` with `@Tag(name = "Recycle Bin")`
- [ ] 3.10 Annotate `TagController` with `@Tag(name = "Tags")`
- [ ] 3.11 Annotate `SearchPresetController` with `@Tag(name = "Search Presets")`

## 4. Verify Swagger UI

- [ ] 4.1 Start the backend with `mvn spring-boot:run` and open `http://localhost:8080/swagger-ui.html`
- [ ] 4.2 Verify all 11 controller tags are present
- [ ] 4.3 Verify `GET /v3/api-docs` returns a valid OpenAPI JSON document

## 5. Testing and Commit

- [ ] 5.1 Run backend test suite: `cd JPPhotoManagerWeb/backend && mvn test`
- [ ] 5.2 Run frontend test suite: `cd JPPhotoManagerWeb/frontend && npm test`
- [ ] 5.3 Commit all changes (only after both test suites pass)
