---
name: java-unit-test-developer
description: >
  JUnit 5 unit and integration test skill for the JPPhotoManager Spring Boot
  3.4 / Java 21 backend. TRIGGER when creating, fixing, or updating test files
  for any backend class: services, repositories, controllers, facades, or
  entities — including when an OpenSpec task calls for backend tests. Always
  invoke alongside java-developer when a new backend class is created or
  substantially modified. Enforces the project's testing conventions: Mockito
  for mocking, AssertJ for assertions, sut naming, one concept per test, and
  the method_condition_result naming pattern. Invoke this skill proactively —
  do not wait to be asked.
metadata:
  scope: [JPPhotoManagerWeb/backend]
---

# Java Unit Test Developer Skill

Write JUnit 5 tests that follow the conventions and best practices of the
JPPhotoManager backend project: Spring Boot 3.4 / Java 21, Mockito, AssertJ,
and clean architecture layering.

## Workflow

Make a todo list and work through it one task at a time.

The detailed rules for each topic live under `reference/`, split so you only
load what today's task actually touches instead of every convention in the
project every time this skill fires. Read the row(s) below matching what
you're testing — a plain service unit test doesn't need the integration-test
or controller-slice-test rules — and skip the rest.

| Topic | Read when you're testing… | File |
| ----- | --------------------------- | ---- |
| §1-4 Project setup, naming & unit test structure | any new test file — file naming/location, class/method naming, structure | `reference/setup-naming-structure.md` |
| §5-6 Mockito patterns & AssertJ assertions | mocking a dependency, or picking the right assertion | `reference/mockito-and-assertj.md` |
| §7-9 `@Async`, `@Transactional` & facade methods | an async method, a transactional boundary, or a facade | `reference/async-transactional-facade.md` |
| §10-11 Integration tests & controller slice tests | a Spring-context integration test, or a `@WebMvcTest`/controller slice test | `reference/integration-and-controller-tests.md` |
| §12-13 Test data setup & test organisation rules | builders/fixtures, or how to structure a test class | `reference/test-data-and-organisation.md` |
| §14 Running tests | how to invoke a specific test/class from the CLI | `reference/running-tests.md` |

---

## Wrap Up

After creating or modifying test files, provide a brief summary covering:

1. Test files created or modified and the class or method they cover
2. Test type (unit with Mockito or integration with Spring context)
3. How to run the new tests:
   ```bash
   cd JPPhotoManagerWeb/backend
   mvn test -Dtest=YourNewTest
   ```
4. Any `@MockitoBean` or `application-test.yml` changes needed for integration tests
