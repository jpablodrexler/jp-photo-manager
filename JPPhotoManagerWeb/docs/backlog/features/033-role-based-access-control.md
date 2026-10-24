# Feature 33 — role-based-access-control

**As a** administrator, **I want** a read-only VIEWER role, **so that** I can let someone browse the library without risking changes.

## Acceptance Criteria
- AC1: Given a VIEWER, when they browse, view, download or rate assets, then it works.
- AC2: Given a VIEWER, when they try to catalog, delete, move, upload or administer users, then the request is refused.
- AC3: Given an ADMIN, when they perform any write operation, then it is allowed.
- AC4: Given a VIEWER, when the UI renders, then write actions such as the Catalog button are not shown.
- AC5: Given the new role, when the system starts, then the role value is seeded without a schema migration.

## Technical notes

Enforce the existing `role` column on `User` by adding a `VIEWER` role that can browse, view, and download but cannot delete, move, catalog, upload, or administer users; implement with `@PreAuthorize` annotations on write-path use cases and controller methods; `SecurityConfig` already provides the filter chain foundation; no schema migration needed beyond seeding the new role value

## Definition of Done

Project-level Definition of Done applies (`definition-of-done.md`). Additional for this feature:
- None
