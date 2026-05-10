## Context

The backend currently uses SQLite as its persistence layer, configured via `application.yml` with the `sqlite-jdbc` driver and Hibernate's `SQLiteDialect` (from `hibernate-community-dialects`). Flyway manages schema migrations, with `V1__initial_schema.sql` written in SQLite DDL. Integration tests use an in-memory SQLite database (Flyway disabled, Hibernate `create-drop`).

The migration replaces every SQLite-specific dependency and configuration with PostgreSQL equivalents, and upgrades the test strategy to use Testcontainers so tests run against a real PostgreSQL engine.

## Goals / Non-Goals

**Goals:**

- Replace SQLite with PostgreSQL in production configuration.
- Rewrite `V1__initial_schema.sql` in standard PostgreSQL DDL.
- Replace the in-memory SQLite test database with a Testcontainers PostgreSQL container.
- Keep the REST API surface unchanged.
- Update all documentation and skills to reflect the new setup.

**Non-Goals:**

- Data migration of existing SQLite databases (out of scope; no production data exists yet).
- Multi-schema or multi-tenant PostgreSQL configuration.
- Connection pooling tuning beyond defaults (PgBouncer, HikariCP tuning).
- Switching ORM (Hibernate/Spring Data JPA are retained).

## Decisions

### 1. PostgreSQL JDBC driver + Flyway PostgreSQL extension

**Decision:** Add `org.postgresql:postgresql` JDBC driver and `org.flywaydb:flyway-database-postgresql` to `pom.xml`; remove `org.xerial:sqlite-jdbc` and `org.hibernate.orm:hibernate-community-dialects`.

**Rationale:** The official PostgreSQL JDBC driver is actively maintained and is the standard choice. Flyway requires its PostgreSQL extension starting from Flyway 10+.

**Alternative considered:** Using R2DBC for reactive persistence — rejected because the rest of the stack is servlet-based and adding reactive DB access would require major refactoring.

### 2. Flyway migration rewrite (new V1)

**Decision:** Rewrite `V1__initial_schema.sql` with PostgreSQL DDL:

- `INTEGER PRIMARY KEY AUTOINCREMENT` → `BIGSERIAL PRIMARY KEY`
- `INTEGER DEFAULT 0` boolean columns → `BOOLEAN NOT NULL DEFAULT FALSE`
- Remove `IF NOT EXISTS` from `CREATE TABLE` (Flyway creates on a clean database; idempotency is Flyway's responsibility)
- Retain `CREATE INDEX IF NOT EXISTS` (supported in PostgreSQL)

**Rationale:** A clean rewrite is safe because no production SQLite data needs to be preserved. Keeping `IF NOT EXISTS` on indexes is harmless and defensive.

**Alternative considered:** Adding a `V2__` migration to alter the SQLite schema — rejected because it would leave SQLite-specific DDL in `V1__` and confuse future maintainers.

### 3. Testcontainers for integration tests

**Decision:** Update `application-test.yml` to enable Flyway and use PostgreSQL dialect; add `org.testcontainers:postgresql` dependency (test scope); annotate `@SpringBootTest` integration tests with `@Testcontainers` and inject the container URL via `@DynamicPropertySource`.

**Rationale:** Running tests against a real PostgreSQL container catches SQL compatibility issues that an in-memory SQLite database would hide. Testcontainers integrates cleanly with Spring Boot 3.4's `@ServiceConnection` or `@DynamicPropertySource`.

**Alternative considered:** Using an embedded PostgreSQL (e.g., `io.zonky.test:embedded-postgresql`) — rejected to keep the approach consistent with the wider Spring ecosystem's recommendation and to avoid an additional embedded binary.

### 4. Application configuration

**Decision:** Use environment variables for the production JDBC URL (`${POSTGRES_URL}`, `${POSTGRES_USERNAME}`, `${POSTGRES_PASSWORD}`) with sensible defaults in `application.yml` pointing to `localhost:5432/photomanager`.

**Rationale:** Externalising credentials via env vars is the twelve-factor approach and avoids secrets in source control.

## Risks / Trade-offs

- **Local developer setup** → Developers must run a PostgreSQL instance locally (Docker recommended). Mitigation: document `docker run` or Docker Compose snippet in CLAUDE.md and README.md.
- **Testcontainers pull time** → First test run pulls the `postgres` Docker image, adding ~30 s cold-start. Mitigation: document this and recommend pre-pulling the image in CI.
- **Schema type changes** → `BOOLEAN` instead of `INTEGER` for flag columns may require `@Column` mapping adjustments in JPA entities. Mitigation: verify each entity field mapping as part of the implementation task.

## Migration Plan

1. Update `pom.xml` — swap dependencies.
2. Rewrite `V1__initial_schema.sql` — PostgreSQL DDL.
3. Update `application.yml` — datasource, dialect, Flyway.
4. Update `application-test.yml` — Testcontainers, re-enable Flyway.
5. Update JPA entities — adjust any SQLite-specific type mappings.
6. Run all tests and fix failures.
7. Update CLAUDE.md, README.md, and skills.
8. Add a Docker Compose file to the repository to simplify local developer setup.
9. Use `@ServiceConnection` (Spring Boot 3.1+) instead of `@DynamicPropertySource` for Testcontainers wiring.

**Rollback:** Revert the `pom.xml` and YAML changes; restore the original `V1__initial_schema.sql`. No data to restore (pre-production).
