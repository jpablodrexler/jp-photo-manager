---
name: cypress-unit-test-developer
description: >
  Cypress Component Testing skill for writing unit/component tests for the
  JPPhotoManager Angular 22 frontend. TRIGGER when creating or modifying
  *.cy.ts test files for standalone components, services, or pipes —
  including when an OpenSpec task calls for frontend tests. Always invoke
  alongside angular-developer when a new Angular component or service is
  created. Enforces the project's strict TypeScript conventions and
  feature-based architecture. Invoke this skill proactively — do not wait
  to be asked.
license: MIT
metadata:
  author: Juan Pablo Drexler
  version: "1.0"
  scope: [JPPhotoManagerWeb/frontend]
---

# Cypress Unit Test Developer Skill

Write Cypress Component Tests that follow the conventions and best practices of
the JPPhotoManager frontend: Angular 22 / TypeScript 6, standalone components,
Angular Material, RxJS, and strict TypeScript.

## Workflow

Make a todo list and work through it one task at a time.

The detailed rules for each topic live under `reference/`, split so you only
load what today's task actually touches instead of every convention in the
project every time this skill fires. Read the row(s) below matching what
you're testing — a pipe test doesn't need the SSE-mocking rules, a plain
component test doesn't need TypeScript-in-tests gotchas — and skip the rest.

| Topic | Read when you're testing… | File |
| ----- | --------------------------- | ---- |
| §1-3 Architecture, file naming/location & naming conventions | any new test file — where it goes, what it's named | `reference/architecture-and-naming.md` |
| §4-5 Component test structure & mocking services | a component test, or stubbing an injected service | `reference/component-structure-and-mocking.md` |
| §6-7 Service unit tests (HTTP) & pipes | a `core/*.service.ts` HTTP test, or a custom pipe test | `reference/service-tests-and-pipes.md` |
| §8-9 Angular output events & EventSource/SSE in components | an `output()` emitter, or a component consuming a Server-Sent Events stream | `reference/output-events-and-sse.md` |
| §10-11 DOM interaction patterns & assertions reference | clicking/typing/asserting on DOM, or picking the right assertion | `reference/dom-and-assertions.md` |
| §12-14 TypeScript rules, Angular Material/zoneless gotchas & test organisation | a TS compile issue in a test, a zoneless/Material quirk, or structuring a test file | `reference/typescript-material-organisation.md` |
| §15 Running tests | how to invoke a specific spec from the CLI | `reference/running-tests.md` |

---

## Wrap Up

After creating or modifying test files, provide a brief summary covering:

1. Test files created or modified and what they cover
2. Any new `data-cy` attributes added to templates
3. Whether `MockEventSource` was used and where it lives
4. How to run the new tests:
   ```bash
   npx cypress run --component --spec "src/app/<path>/<file>.cy.ts"
   ```
5. Any gotcha from §13 (zoneless `markForCheck()`) or §11 (`rejectionOf()`)
   that a new test needed
