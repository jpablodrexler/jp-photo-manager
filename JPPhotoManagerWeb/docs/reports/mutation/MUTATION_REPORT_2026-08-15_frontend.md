# Mutation Testing Report (frontend) — 2026-08-15

**Commit:** cdb2dbf
**Generated:** 2026-08-15T03:32:43.671Z
**Scope:** curated core/ files with genuine branching logic, see stryker.conf.mjs `mutate` (thin CRUD-wrapper services excluded)
**Tool:** StrykerJS command runner (`npx stryker run`), driving a scoped `cypress run --component --spec` invocation per mutant

**Overall mutation score:** 99.82% (552/553 detected, excluding 0 ignored + 0 compile errors)
**Total mutants generated:** 553
**Killed:** 552  **Survived:** 1  **Timeout:** 0  **No coverage:** 0  **Runtime errors:** 0  **Compile errors:** 0  **Ignored:** 0

A survived mutant means the test suite ran and passed even though the mutated line changed the code's behavior — the tests exercise that code path but don't actually assert on the behavior a bug there would break. A "no coverage" mutant means no test reaches that line at all.

## Per-file results (worst mutation score first)

| File | Score | Total | Killed | Survived | Timeout | No cov | Runtime err | Compile err | Ignored |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| src/app/core/error-handler/global-error-handler.ts | 88.89% | 9 | 8 | 1 | 0 | 0 | 0 | 0 | 0 |
| src/app/core/interceptors/auth.interceptor.ts | 100.00% | 66 | 66 | 0 | 0 | 0 | 0 | 0 | 0 |
| src/app/core/services/asset.service.ts | 100.00% | 103 | 103 | 0 | 0 | 0 | 0 | 0 | 0 |
| src/app/core/services/auth.service.ts | 100.00% | 94 | 94 | 0 | 0 | 0 | 0 | 0 | 0 |
| src/app/core/services/background-sync.service.ts | 100.00% | 45 | 45 | 0 | 0 | 0 | 0 | 0 | 0 |
| src/app/core/services/folder.service.ts | 100.00% | 15 | 15 | 0 | 0 | 0 | 0 | 0 | 0 |
| src/app/core/services/media-player.service.ts | 100.00% | 174 | 174 | 0 | 0 | 0 | 0 | 0 | 0 |
| src/app/core/services/theme.service.ts | 100.00% | 47 | 47 | 0 | 0 | 0 | 0 | 0 | 0 |

## 1 surviving / uncovered mutant(s)

| File | Line | Mutator | Replacement | Status |
| --- | --- | --- | --- | --- |
| src/app/core/error-handler/global-error-handler.ts | 16 | LogicalOperator | `error instanceof Error || error.message` | Survived |
