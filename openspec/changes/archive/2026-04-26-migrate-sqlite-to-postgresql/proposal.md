## Why

SQLite is suitable for local desktop applications, but as the JPPhotoManager web application grows it becomes a bottleneck: it lacks concurrent write support, has limited data types, and is not a realistic target for cloud or multi-user deployments. Migrating to PostgreSQL unlocks proper concurrency, richer SQL semantics, and alignment with standard production infrastructure.

## What Changes

- Replace the `sqlite-jdbc` driver and `hibernate-community-dialects` with the `postgresql` JDBC driver and `flyway-database-postgresql` extension in `pom.xml`.
- Update `application.yml` to configure a PostgreSQL datasource URL, driver, and JPA dialect.
- Rewrite the Flyway migration `V1__initial_schema.sql` using PostgreSQL-compatible DDL (`BIGSERIAL`, `BOOLEAN`, standard index syntax).
- Update `application-test.yml` to use Testcontainers (`postgresql` container) so integration tests run against a real PostgreSQL instance.
- Update `CLAUDE.md` and `README.md` to reflect the new database engine, connection configuration, and test prerequisites.
- Update the `java-unit-test-developer` and `java-developer` skills to document PostgreSQL conventions.

## Capabilities

### New Capabilities

- `postgresql-persistence`: Configure PostgreSQL as the application database — datasource, dialect, Flyway migrations, and test infrastructure.

### Modified Capabilities

_(none — no existing spec files exist yet)_

## Impact

- **`pom.xml`**: remove `sqlite-jdbc` + `hibernate-community-dialects`; add `postgresql` JDBC + `flyway-database-postgresql`.
- **`application.yml`**: datasource URL, driver class, and JPA dialect updated.
- **`application-test.yml`**: Testcontainers `@DynamicPropertySource` replaces the SQLite in-memory URL; Flyway re-enabled for tests.
- **`V1__initial_schema.sql`**: rewritten in PostgreSQL DDL; `AUTOINCREMENT` → `BIGSERIAL`; `INTEGER` booleans → `BOOLEAN`.
- **`CLAUDE.md` / `README.md`**: updated setup instructions, prerequisites, and local-dev guidance.
- **Skills** (`java-developer`, `java-unit-test-developer`): updated persistence conventions.
- **No API surface changes** — all REST endpoints remain identical.
