# Route E2E Coverage Report (frontend) — 2026-08-14

**Commit:** 64e7da3
**Generated:** 2026-08-14T23:25:35.919Z
**Scope:** 12 declared routes; 16 E2E spec files (4 real-backend, 12 mocked)

Whether each declared route has at least one literal `cy.visit(...)` in either E2E tier. A route built from a variable rather than a string literal won't be detected by this scan; a parameterized route (`albums/:id`) is matched by its static path prefix instead of an exact string.

| Route | Real-backend tier | Mocked tier |
| --- | --- | --- |
| /admin/users | ✓ | ✓ |
| /albums | ✓ | ✓ |
| /albums/:id | ✓ | ✓ |
| /analytics | — | ✓ |
| /convert | — | ✓ |
| /duplicates | — | ✓ |
| /gallery | — | ✓ |
| /home | — | ✓ |
| /login | ✓ | ✓ |
| /profile/sessions | ✓ | ✓ |
| /recycle-bin | — | ✓ |
| /sync | — | ✓ |

**Routes with no E2E coverage in either tier:** none
