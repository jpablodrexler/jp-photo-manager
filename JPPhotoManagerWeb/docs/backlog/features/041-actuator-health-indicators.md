# Feature 41 — actuator-health-indicators

`spring-boot-starter-actuator` is already in `pom.xml` but has no `management.*` configuration and `/actuator/**` is blocked by the JWT filter; expose `/actuator/health` in `SecurityConfig.permitAll()`; add `management.endpoints.web.exposure.include=health,info` to `application.yml`; implement three custom `HealthIndicator` beans: disk space on the thumbnails directory, thumbnails directory writability, and PostgreSQL connectivity — covering the two most likely runtime failures
