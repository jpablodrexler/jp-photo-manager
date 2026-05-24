## 1. Maven dependency

- [x] 1.1 Add to `pom.xml`:
  ```xml
  <dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.x.x</version>
  </dependency>
  ```
  (use latest stable version compatible with Spring Boot 3.4)

## 2. SecurityConfig — exempt Swagger endpoints

- [x] 2.1 Add `.requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()` to the `SecurityConfig` `securityFilterChain` before the authenticated matcher

## 3. Controller annotations

- [x] 3.1 Annotate `AssetController` with `@Tag(name = "Assets")` and add `@Operation` + `@ApiResponse` to each method
- [x] 3.2 Annotate `FolderController` with `@Tag(name = "Folders")`
- [x] 3.3 Annotate `AuthController` with `@Tag(name = "Authentication")`
- [x] 3.4 Annotate `UserController` with `@Tag(name = "User Administration")`
- [x] 3.5 Annotate `AlbumController` with `@Tag(name = "Albums")`
- [x] 3.6 Annotate `SyncController` with `@Tag(name = "Sync")`
- [x] 3.7 Annotate `ConvertController` with `@Tag(name = "Convert")`
- [x] 3.8 Annotate `DuplicatesController` (or equivalent) with `@Tag(name = "Duplicates")` — no standalone controller; duplicates endpoint covered under AssetController
- [x] 3.9 Annotate `RecycleBinController` with `@Tag(name = "Recycle Bin")`
- [x] 3.10 Annotate `TagController` with `@Tag(name = "Tags")`
- [x] 3.11 Annotate `SearchPresetController` with `@Tag(name = "Search Presets")`
- [x] 3.12 Annotate `HomeController` with `@Tag(name = "Home")` and add `@Operation` + `@ApiResponse` to `getStats`
- [x] 3.13 Annotate `MediaController` with `@Tag(name = "Media")` and add `@Operation` + `@ApiResponse` to `streamAsset` (include `206 Partial Content`) and `getPlaylist`

## 4. Verify Swagger UI

- [x] 4.1 Start the backend with `mvn spring-boot:run` and open `http://localhost:8080/swagger-ui.html`
- [x] 4.2 Verify all 13 controller tags are present
- [x] 4.3 Verify `GET /v3/api-docs` returns a valid OpenAPI JSON document

## 5. Testing and Commit

- [x] 5.1 Run backend test suite: `cd JPPhotoManagerWeb/backend && mvn test`
- [x] 5.2 Run frontend test suite: `cd JPPhotoManagerWeb/frontend && npm test`
- [x] 5.3 Commit all changes (only after both test suites pass)
