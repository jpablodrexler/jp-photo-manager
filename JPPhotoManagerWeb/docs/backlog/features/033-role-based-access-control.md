# Feature 33 — role-based-access-control

Enforce the existing `role` column on `User` by adding a `VIEWER` role that can browse, view, and download but cannot delete, move, catalog, upload, or administer users; implement with `@PreAuthorize` annotations on write-path use cases and controller methods; `SecurityConfig` already provides the filter chain foundation; no schema migration needed beyond seeding the new role value
