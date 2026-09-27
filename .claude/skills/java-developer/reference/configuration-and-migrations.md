# Java Developer — Configuration & Database Migrations (Flyway)

_Part of the `java-developer` skill — see `../SKILL.md` for the topic index. Load this file only when your current task touches this topic._

## 10. Configuration

### application.yml

```yaml
spring:
  datasource:
    url: jdbc:postgresql://${POSTGRES_HOST:localhost}:${POSTGRES_PORT:5432}/${POSTGRES_DB:photomanager}
    username: ${POSTGRES_USERNAME:postgres}
    password: ${POSTGRES_PASSWORD:postgres}
    driver-class-name: org.postgresql.Driver
  jpa:
    database-platform: org.hibernate.dialect.PostgreSQLDialect
    hibernate:
      ddl-auto: none
    open-in-view: false
  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true

server:
  port: 8080

photomanager:
  initial-directory: ${user.home}/Pictures
  catalog-batch-size: 1000

logging:
  level:
    com.jpablodrexler: INFO
```

**Local development prerequisite:** PostgreSQL 18+ must be running:
```bash
docker run -d --name photomanager-db \
  -e POSTGRES_PASSWORD=postgres \
  -e POSTGRES_DB=photomanager \
  -p 5432:5432 postgres:18
```

### application-local.yml (local developer override)

Override machine-specific properties without committing them. Add to `application.yml`:

```yaml
spring:
  config:
    import: optional:classpath:application-local.yml
```

Create `src/main/resources/application-local.yml` (gitignored):

```yaml
photomanager:
  initial-directory: ${user.home}/Imágenes
  root-catalog-folders: ${user.home}/Imágenes
```

### application-test.yml

```yaml
spring:
  jpa:
    database-platform: org.hibernate.dialect.PostgreSQLDialect
    hibernate:
      ddl-auto: none
    open-in-view: false
  flyway:
    enabled: true
    locations: classpath:db/migration
```

No JDBC URL — Testcontainers injects it via `@ServiceConnection`. Integration tests extend `PostgresIntegrationTest`:

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@Testcontainers
public abstract class PostgresIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18");
}
```

### Inject properties

```java
@Value("${photomanager.catalog-batch-size:1000}")
private int catalogBatchSize;
```

---

## 11. Database Migrations (Flyway)

- Place SQL files in `src/main/resources/db/migration/`.
- Name them `V{n}__{Description}.sql` (two underscores).
- Never modify an applied migration; create a new one instead.

```sql
-- V1__initial_schema.sql  (PostgreSQL DDL)
CREATE TABLE folders (
    folder_id BIGSERIAL PRIMARY KEY,
    path      TEXT      NOT NULL
);

-- V2__add_example_table.sql
CREATE TABLE example (
    id          BIGSERIAL PRIMARY KEY,
    folder_id   BIGINT    NOT NULL REFERENCES folders(folder_id),
    name        TEXT      NOT NULL,
    active      BOOLEAN   NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP NOT NULL
);
```

**Key PostgreSQL DDL conventions:**
- Auto-increment primary keys: `BIGSERIAL PRIMARY KEY`
- Boolean columns: `BOOLEAN NOT NULL DEFAULT FALSE` (never `INTEGER DEFAULT 0`)
- Date/time columns: `TIMESTAMP` (Hibernate maps `LocalDateTime` natively)
- Do **not** use `IF NOT EXISTS` on `CREATE TABLE` — Flyway manages idempotency

---

