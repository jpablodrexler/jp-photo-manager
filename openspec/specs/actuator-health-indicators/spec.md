# actuator-health-indicators

`GET /actuator/health` returns the aggregate health status (UP/DOWN/DEGRADED) with sub-indicators for the database, thumbnail storage, and geocoding service.

---

## ADDED Requirements

### Requirement: Health endpoint is publicly accessible

`GET /actuator/health` SHALL return the aggregate health status without requiring authentication.

#### Scenario: Health endpoint returns UP when all systems are healthy

- **GIVEN** the database is reachable, the thumbnail directory is writable, and the geocoding service is reachable
- **WHEN** `GET /actuator/health` is called
- **THEN** the response is `200 OK` with `{ "status": "UP", "components": { "database": { "status": "UP" }, "thumbnailStorage": { "status": "UP" }, "geocoding": { "status": "UP" } } }`

### Requirement: DatabaseHealthIndicator reports UP when the database is reachable

The `DatabaseHealthIndicator` SHALL execute a lightweight query against PostgreSQL and report `UP` on success, `DOWN` on failure.

#### Scenario: Database is unreachable

- **GIVEN** the PostgreSQL server is down
- **WHEN** `GET /actuator/health` is called
- **THEN** the `database` component has `"status": "DOWN"` and the aggregate status is `"DOWN"`

### Requirement: ThumbnailStorageHealthIndicator reports UP when the storage directory is writable

The `ThumbnailStorageHealthIndicator` SHALL check that the configured thumbnails directory exists and is writable.

#### Scenario: Thumbnail directory is not writable

- **GIVEN** the thumbnails directory has been made read-only
- **WHEN** `GET /actuator/health` is called
- **THEN** the `thumbnailStorage` component has `"status": "DOWN"`

### Requirement: GeocodingHealthIndicator reports DEGRADED when the geocoding service is unreachable

The `GeocodingHealthIndicator` SHALL ping the geocoding API; an unreachable service SHALL result in `DEGRADED` status (not DOWN), leaving the aggregate status as UP (DEGRADED does not fail the aggregate).

#### Scenario: Geocoding service is unreachable

- **GIVEN** the external geocoding API is unreachable
- **WHEN** `GET /actuator/health` is called
- **THEN** the `geocoding` component has `"status": "DEGRADED"` and the aggregate status remains `"UP"`
