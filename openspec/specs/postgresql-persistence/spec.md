# postgresql-persistence

Specifies how the application connects to and manages a PostgreSQL database.

---

### Requirement: Application uses PostgreSQL as its database engine
The system SHALL connect to a PostgreSQL 15+ database for all persistence operations. The JDBC URL, username, and password SHALL be configurable via the environment variables `POSTGRES_URL`, `POSTGRES_USERNAME`, and `POSTGRES_PASSWORD`, with default values suitable for local development (`localhost:5432/photomanager`, `postgres`, `postgres`).

#### Scenario: Application starts with a valid PostgreSQL connection
- **WHEN** the Spring Boot application starts and a PostgreSQL instance is reachable at the configured URL
- **THEN** the application connects successfully and Flyway applies pending migrations before the first request is served

#### Scenario: Application fails fast on missing database
- **WHEN** the Spring Boot application starts and no PostgreSQL instance is reachable
- **THEN** the application fails to start with a clear datasource connection error

---

### Requirement: Flyway manages the PostgreSQL schema
The system SHALL use Flyway to create and evolve the database schema. `V1__initial_schema.sql` SHALL contain valid PostgreSQL DDL, using `BIGSERIAL` for auto-increment primary keys and `BOOLEAN` for flag columns.

#### Scenario: Fresh database migration
- **WHEN** the application connects to an empty PostgreSQL database
- **THEN** Flyway runs `V1__initial_schema.sql` and creates all tables (`folders`, `assets`, `sync_assets_directories_definitions`, `convert_assets_directories_definitions`, `recent_target_paths`) and the `ix_assets_folder_id` index

#### Scenario: Already-migrated database
- **WHEN** the application connects to a database where `V1__initial_schema.sql` has already been applied
- **THEN** Flyway reports no pending migrations and the application starts without modifying the schema

---

### Requirement: Integration tests run against a Testcontainers PostgreSQL instance
The system's integration tests (`@SpringBootTest`) SHALL use Testcontainers to spin up a real PostgreSQL container. Flyway SHALL be enabled in the `test` Spring profile so that the schema is applied before each test suite.

#### Scenario: Integration test suite starts
- **WHEN** a `@SpringBootTest` test class is loaded with the `test` profile
- **THEN** a PostgreSQL Docker container is started, the datasource URL is injected dynamically, and Flyway applies all migrations before the first test runs

#### Scenario: Integration test isolation
- **WHEN** a test modifies data in the database
- **THEN** the change is visible within that test but does not affect other tests (each test class gets a fresh schema via `@Transactional` rollback or container lifecycle)

---

### Requirement: JPA entities are mapped correctly to PostgreSQL types
All JPA entity fields SHALL use Java types that map correctly to PostgreSQL column types: `Long` for `BIGSERIAL` primary keys, `Boolean` for `BOOLEAN` columns, and `String` for `TEXT` columns.

#### Scenario: Asset entity persisted and retrieved
- **WHEN** an `Asset` entity is saved via its repository
- **THEN** it is assigned a `Long` ID by the PostgreSQL sequence and can be retrieved by that ID with all fields intact

#### Scenario: Boolean flag columns persist correctly
- **WHEN** a `SyncAssetsDirectoriesDefinition` entity with `includeSubFolders = true` is saved
- **THEN** the `include_sub_folders` column in PostgreSQL stores `true` and the entity is retrieved with `includeSubFolders == true`
