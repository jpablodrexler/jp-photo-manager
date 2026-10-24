# Feature 41 — actuator-health-indicators

**As a** administrator, **I want** a health endpoint with checks for disk space, thumbnail directory and the database, **so that** I learn about the most likely runtime failures before users do.

## Acceptance Criteria
- AC1: Given everything is healthy, when /actuator/health is called without authentication, then it returns UP.
- AC2: Given the database is unreachable, when the endpoint is called, then it reports DOWN for that indicator.
- AC3: Given the thumbnail directory is not writable or low on disk, when the endpoint is called, then it reports DOWN for that indicator.
- AC4: Given the geocoding service is unreachable, when the endpoint is called, then the failure is reported without taking the application down.
- AC5: Given the actuator, when it is configured, then only health and info are exposed.

## Technical notes

`spring-boot-starter-actuator` is already in `pom.xml` but has no `management.*` configuration and `/actuator/**` is blocked by the JWT filter; expose `/actuator/health` in `SecurityConfig.permitAll()`; add `management.endpoints.web.exposure.include=health,info` to `application.yml`; implement three custom `HealthIndicator` beans: disk space on the thumbnails directory, thumbnails directory writability, and PostgreSQL connectivity — covering the two most likely runtime failures

## Definition of Done

Project-level Definition of Done applies (`definition-of-done.md`). Additional for this feature:
- None
