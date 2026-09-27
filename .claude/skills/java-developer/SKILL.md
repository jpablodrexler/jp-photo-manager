---
name: java-developer
description: >
  Java developer skill for the JPPhotoManager Spring Boot 3.4 / Java 21
  backend. TRIGGER whenever work touches JPPhotoManagerWeb/backend — including
  when implementing OpenSpec tasks: adding a new use case, controller, entity,
  repository, DTO, or enum; fixing a bug in any Java class; refactoring or
  extracting logic into a new class; wiring up transactions, async operations,
  or Spring beans. Enforces hexagonal (ports and adapters) architecture
  (infrastructure/web → application/usecase → domain ← infrastructure/persistence | infrastructure/service),
  the port-interface / adapter-implementation split for every service and
  repository, and all other coding standards. Invoke this skill proactively —
  do not wait to be asked.
metadata:
  scope: [JPPhotoManagerWeb/backend]
---

# Java Developer Skill

Write Java code that follows the conventions and best practices of the
JPPhotoManager backend project: a Spring Boot 3.4 / Java 21 application with
hexagonal (ports and adapters) architecture, Lombok, MapStruct, Spring Data
JPA, Flyway, and PostgreSQL.

## Workflow

Make a todo list and work through it one task at a time.

The detailed rules for each topic live under `reference/`, split so you only
load what today's task actually touches instead of every convention in the
project every time this skill fires. Read the row(s) below matching your
todo items — a DTO/controller-only change doesn't need the Flyway migration
rules, a pure domain-model change doesn't need Spring Security — and skip
the rest. When a task spans several topics (the common case for a full
feature), read each relevant file as you reach that part of the work, not
all of them upfront.

| Topic | Read when you're touching… | File |
| ----- | --------------------------- | ---- |
| §1-2 Project setup & package/layer structure | build config, or deciding which package a new file goes in | `reference/project-setup-and-layers.md` |
| §3-5 Port/adapter split, naming conventions & annotations/Lombok | a new service/repository interface + implementation, naming a class, or Lombok annotations | `reference/ports-naming-annotations.md` |
| §6.1-6.3 Layer patterns — web, application & domain | a controller, HTTP DTO, use case, domain model, or port interface | `reference/layer-patterns-web-app-domain.md` |
| §6.4-6.7 Layer patterns — infrastructure (persistence, mappers, transactions) | a JPA persistence adapter, a MapStruct mapper, the delete-then-insert pattern, or `@Transactional`/self-invocation | `reference/layer-patterns-infrastructure.md` |
| §7-9 Async & streaming, error handling/logging & pagination | `@Async`/SSE, exception handling, logging, or a paginated endpoint | `reference/async-error-pagination.md` |
| §10-11 Configuration & database migrations (Flyway) | `application.yml`/config properties, or a new Flyway migration | `reference/configuration-and-migrations.md` |
| §12-13 Testing & Java 21 features | writing/editing a backend test, or a records/pattern-matching/switch-expression question | `reference/testing-and-java21.md` |
| §14-15 Code style rules & Spring Security | general style questions, or anything touching `SecurityConfig`/auth rules | `reference/code-style-and-security.md` |

---

## Wrap Up

After implementing the requested feature, provide a brief summary covering:

1. Files created or modified and their purpose
2. Any new database migration files added
3. How to run the tests for the new code:
   ```bash
   cd JPPhotoManagerWeb/backend && mvn test -Dtest=YourNewTest
   ```
4. Any configuration properties the user should set
