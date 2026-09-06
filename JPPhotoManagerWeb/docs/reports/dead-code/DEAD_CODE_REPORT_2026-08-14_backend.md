# Dead Code Report (backend) — 2026-08-14

**Commit:** 64e7da3
**Generated:** 2026-08-14T23:27:38Z
**Scope:** backend/pom.xml (via `mvn dependency:analyze`)

Every `spring-boot-starter-*` entry under "Unused declared dependencies" below is expected noise (see this script's own header comment for why) — not a real finding.

```
[INFO] Scanning for projects...
[INFO] Building jp-photo-manager 1.0.0-SNAPSHOT
[INFO]   from pom.xml
[WARNING] Used undeclared dependencies found:
[WARNING]    com.fasterxml.jackson.core:jackson-databind:jar:2.19.1:compile
[WARNING]    org.springframework:spring-tx:jar:6.2.8:compile
[WARNING]    com.fasterxml.jackson.core:jackson-annotations:jar:2.19.1:compile
[WARNING]    org.springframework.boot:spring-boot-test:jar:3.5.3:test
[WARNING]    org.springframework.security:spring-security-crypto:jar:6.5.1:compile
[WARNING]    org.hamcrest:hamcrest:jar:3.0:test
[WARNING]    org.springframework.batch:spring-batch-infrastructure:jar:5.2.2:compile
[WARNING]    org.springframework.security:spring-security-web:jar:6.5.1:compile
[WARNING]    jakarta.annotation:jakarta.annotation-api:jar:2.1.1:compile
[WARNING]    org.springframework.data:spring-data-commons:jar:3.5.1:compile
[WARNING]    org.springframework.boot:spring-boot:jar:3.5.3:compile
[WARNING]    org.slf4j:slf4j-api:jar:2.0.17:compile
[WARNING]    org.springframework.data:spring-data-mongodb:jar:4.5.1:compile
[WARNING]    org.springframework.data:spring-data-redis:jar:3.5.1:compile
[WARNING]    io.micrometer:micrometer-core:jar:1.15.1:compile
[WARNING]    org.springframework.security:spring-security-config:jar:6.5.1:compile
[WARNING]    io.swagger.core.v3:swagger-annotations-jakarta:jar:2.2.30:compile
[WARNING]    org.testcontainers:testcontainers:jar:1.21.2:test
[WARNING]    org.springframework.data:spring-data-jpa:jar:3.5.1:compile
[WARNING]    org.springframework.boot:spring-boot-test-autoconfigure:jar:3.5.3:test
[WARNING]    org.springframework.security:spring-security-core:jar:6.5.1:compile
[WARNING]    org.mockito:mockito-core:jar:5.17.0:test
[WARNING]    org.junit.jupiter:junit-jupiter-api:jar:5.12.2:test
[WARNING]    org.mockito:mockito-junit-jupiter:jar:5.17.0:test
[WARNING]    org.springframework.batch:spring-batch-core:jar:5.2.2:compile
[WARNING]    org.hibernate.orm:hibernate-core:jar:6.6.18.Final:compile
[WARNING]    org.springframework:spring-context:jar:6.2.8:compile
[WARNING]    org.springframework.boot:spring-boot-autoconfigure:jar:3.5.3:compile
[WARNING]    com.fasterxml.jackson.core:jackson-core:jar:2.19.1:compile
[WARNING]    org.springframework:spring-core:jar:6.2.8:compile
[WARNING]    jakarta.persistence:jakarta.persistence-api:jar:3.1.0:compile
[WARNING]    org.apache.kafka:kafka-clients:jar:3.9.1:compile
[WARNING]    org.springframework.boot:spring-boot-actuator:jar:3.5.3:compile
[WARNING]    org.springframework:spring-webmvc:jar:6.2.8:compile
[WARNING]    io.lettuce:lettuce-core:jar:6.6.0.RELEASE:compile
[WARNING]    org.springframework:spring-test:jar:6.2.8:test
[WARNING]    org.assertj:assertj-core:jar:3.27.3:test
[WARNING]    org.mongodb:bson:jar:5.5.1:compile
[WARNING]    org.springframework:spring-beans:jar:6.2.8:compile
[WARNING]    org.springframework:spring-web:jar:6.2.8:compile
[WARNING]    jakarta.validation:jakarta.validation-api:jar:3.0.2:compile
[WARNING]    org.apache.tomcat.embed:tomcat-embed-core:jar:10.1.42:compile
[WARNING] Unused declared dependencies found:
[WARNING]    org.springframework.boot:spring-boot-starter-web:jar:3.5.3:compile
[WARNING]    org.springframework.boot:spring-boot-starter-data-jpa:jar:3.5.3:compile
[WARNING]    org.springframework.boot:spring-boot-starter-validation:jar:3.5.3:compile
[WARNING]    org.springframework.boot:spring-boot-starter-actuator:jar:3.5.3:compile
[WARNING]    io.micrometer:micrometer-registry-prometheus:jar:1.15.1:compile
[WARNING]    org.postgresql:postgresql:jar:42.7.7:compile
[WARNING]    org.flywaydb:flyway-core:jar:11.7.2:compile
[WARNING]    org.flywaydb:flyway-database-postgresql:jar:11.7.2:compile
[WARNING]    org.kohsuke:github-api:jar:1.321:compile
[WARNING]    net.logstash.logback:logstash-logback-encoder:jar:8.0:compile
[WARNING]    org.springframework.boot:spring-boot-starter-data-redis:jar:3.5.3:compile
[WARNING]    org.springframework.boot:spring-boot-starter-data-mongodb:jar:3.5.3:compile
[WARNING]    org.springdoc:springdoc-openapi-starter-webmvc-ui:jar:2.8.9:compile
[WARNING]    org.springframework.boot:spring-boot-starter-batch:jar:3.5.3:compile
[WARNING]    org.springframework.boot:spring-boot-starter-security:jar:3.5.3:compile
[WARNING]    io.jsonwebtoken:jjwt-impl:jar:0.12.6:runtime
[WARNING]    io.jsonwebtoken:jjwt-jackson:jar:0.12.6:runtime
[WARNING]    org.springframework.boot:spring-boot-starter-test:jar:3.5.3:test
[WARNING] Non-test scoped test only dependencies found:
[WARNING]    org.mongodb:bson:jar:5.5.1:compile
[INFO] BUILD SUCCESS
[INFO] Total time:  3.940 s
[INFO] Finished at: 2026-08-14T20:27:44-03:00
```
