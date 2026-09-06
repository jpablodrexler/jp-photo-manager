# Lighthouse Report (frontend) — 2026-08-15

**Commit:** a62ecce
**Generated:** 2026-08-15T19:43:55.540Z
**Build:** production (`ng build --configuration production`), self-hosted — not the `ng serve` dev build, so scores reflect real optimized output.
**Scope:** /login — the only route reachable without an authenticated session; every other route requires signing in first, which this report doesn't attempt to simulate.

| Route | Performance | Accessibility | Best Practices | SEO |
| --- | --- | --- | --- | --- |
| /login | 66 | 98 | 100 | 82 |

## Accessibility audit failures

| Route | Audit |
| --- | --- |
| /login | Document does not have a main landmark. (`landmark-one-main`) |
