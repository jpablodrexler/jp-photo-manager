# Lighthouse Report (frontend) — 2026-08-14

**Commit:** 64e7da3
**Generated:** 2026-08-14T23:33:03.416Z
**Build:** production (`ng build --configuration production`), self-hosted — not the `ng serve` dev build, so scores reflect real optimized output.
**Scope:** /login — the only route reachable without an authenticated session; every other route requires signing in first, which this report doesn't attempt to simulate.

| Route | Performance | Accessibility | Best Practices | SEO |
| --- | --- | --- | --- | --- |
| /login | 70 | 98 | 96 | 82 |

## Accessibility audit failures

| Route | Audit |
| --- | --- |
| /login | Document does not have a main landmark. (`landmark-one-main`) |
