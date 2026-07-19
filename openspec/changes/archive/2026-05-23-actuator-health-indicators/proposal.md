## Why

The backend currently exposes no operational health endpoint. Operators and monitoring tools cannot check whether the database connection is healthy, whether the thumbnail storage directory is writable, or whether the external geocoding service is reachable. Spring Boot Actuator with custom `HealthIndicator` beans provides these checks via a standard `/actuator/health` endpoint.

## What Changes

- Enable Spring Boot Actuator and expose the `health` endpoint
- Add three custom `HealthIndicator` beans:
  1. `DatabaseHealthIndicator` — verifies the PostgreSQL connection with a lightweight query
  2. `ThumbnailStorageHealthIndicator` — checks that the thumbnails directory exists and is writable
  3. `GeocodingHealthIndicator` — pings the geocoding API base URL with a HEAD request (degraded if unreachable)
- The health endpoint is publicly accessible (no authentication required)

## Capabilities

### New Capabilities

- `actuator-health-indicators`: `GET /actuator/health` returns the aggregate health status (UP/DOWN/DEGRADED) with sub-indicators for the database, thumbnail storage, and geocoding service.

### Modified Capabilities

_(none)_

## Impact

- `JPPhotoManagerWeb/backend/pom.xml` — add `spring-boot-starter-actuator`
- `JPPhotoManagerWeb/backend/src/main/resources/application.yml` — expose health endpoint, enable health details
- `JPPhotoManagerWeb/backend/src/main/java/com/jpablodrexler/photomanager/infrastructure/health/DatabaseHealthIndicator.java` — new bean
- `JPPhotoManagerWeb/backend/src/main/java/com/jpablodrexler/photomanager/infrastructure/health/ThumbnailStorageHealthIndicator.java` — new bean
- `JPPhotoManagerWeb/backend/src/main/java/com/jpablodrexler/photomanager/infrastructure/health/GeocodingHealthIndicator.java` — new bean
- `JPPhotoManagerWeb/backend/src/main/java/com/jpablodrexler/photomanager/config/SecurityConfig.java` — permit `/actuator/health` without authentication
- `JPPhotoManagerWeb/backend/src/test/` — tests for each health indicator
