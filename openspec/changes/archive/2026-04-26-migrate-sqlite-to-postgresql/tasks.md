## 1. Update Maven Dependencies

- [x] 1.1 Remove `org.xerial:sqlite-jdbc` dependency from `pom.xml`
- [x] 1.2 Remove `org.hibernate.orm:hibernate-community-dialects` dependency from `pom.xml`
- [x] 1.3 Add `org.postgresql:postgresql` JDBC driver dependency to `pom.xml`
- [x] 1.4 Add `org.flywaydb:flyway-database-postgresql` dependency to `pom.xml`
- [x] 1.5 Add `org.testcontainers:postgresql` dependency (scope `test`) to `pom.xml`
- [x] 1.6 Add `org.testcontainers:junit-jupiter` dependency (scope `test`) to `pom.xml` if not already present

## 2. Rewrite Flyway Migration for PostgreSQL

- [x] 2.1 Rewrite `src/main/resources/db/migration/V1__initial_schema.sql` using PostgreSQL DDL: replace `INTEGER PRIMARY KEY AUTOINCREMENT` with `BIGSERIAL PRIMARY KEY`, replace `INTEGER DEFAULT 0` boolean columns with `BOOLEAN NOT NULL DEFAULT FALSE`, and remove SQLite-specific `IF NOT EXISTS` on `CREATE TABLE` statements

## 3. Update Application Configuration

- [x] 3.1 Update `application.yml`: set `spring.datasource.url` to `jdbc:postgresql://${POSTGRES_HOST:localhost}:${POSTGRES_PORT:5432}/${POSTGRES_DB:photomanager}`, set `spring.datasource.username` and `spring.datasource.password` to env-var-backed defaults, set `spring.datasource.driver-class-name` to `org.postgresql.Driver`, and update `spring.jpa.database-platform` to `org.hibernate.dialect.PostgreSQLDialect`
- [x] 3.2 Update `application-test.yml`: remove the SQLite datasource URL and driver, remove the Hikari `maximum-pool-size: 1` constraint, change `spring.jpa.database-platform` to `org.hibernate.dialect.PostgreSQLDialect`, set `spring.jpa.hibernate.ddl-auto: none`, and enable Flyway (`spring.flyway.enabled: true`)

## 4. Update JPA Entities

- [x] 4.1 In `Asset.java`: remove `columnDefinition = "INTEGER"` from `@Column` on `assetId` and `fileSize` so Hibernate maps them with PostgreSQL-appropriate types
- [x] 4.2 In `Folder.java`: remove any `columnDefinition = "INTEGER"` from the `@Column` on the primary key
- [x] 4.3 In `SyncAssetsDirectoriesDefinition.java`: remove `columnDefinition = "INTEGER"` from the primary key `@Column`; the `boolean` fields and `int order` field require no change (Hibernate maps these correctly to PostgreSQL)
- [x] 4.4 In `ConvertAssetsDirectoriesDefinition.java`: remove `columnDefinition = "INTEGER"` from the primary key `@Column`
- [x] 4.5 In `RecentTargetPath.java`: remove `columnDefinition = "INTEGER"` from the primary key `@Column`

## 5. Configure Testcontainers for Integration Tests

- [x] 5.1 Create a shared Testcontainers base class or static container field in `@SpringBootTest` integration tests that starts a `PostgreSQLContainer` and uses Spring Boot 3.1+ `@ServiceConnection` to inject the datasource URL, username, and password into the Spring context
- [x] 5.2 Annotate each existing `@SpringBootTest` integration test class with `@Testcontainers` and reference the shared container

## 6. Run and Fix Tests

- [x] 6.1 Run `mvn test` and fix any compilation errors caused by dependency or import changes
- [x] 6.2 Fix any runtime test failures caused by SQL dialect differences (e.g., function names, quoting, type mismatches)
- [x] 6.3 Confirm all tests pass with `mvn test`

## 7. Update Documentation and Skills

- [x] 7.1 Update `CLAUDE.md` (root and `JPPhotoManagerWeb/`): replace SQLite references with PostgreSQL, add prerequisite (PostgreSQL 15+ running locally, e.g., via `docker run -e POSTGRES_PASSWORD=postgres -p 5432:5432 postgres:15`), update connection config description. Root `CLAUDE.md` should only reference PostgreSQL for the Web version.
- [x] 7.2 Update `README.md` with the same PostgreSQL setup instructions and environment variable table
- [x] 7.3 Update the `java-developer` skill: replace SQLite dialect reference with `PostgreSQLDialect`, update persistence conventions
- [x] 7.4 Update the `java-unit-test-developer` skill: document Testcontainers pattern for integration tests, replace SQLite in-memory URL example
