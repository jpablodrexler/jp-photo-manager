# Changelog

## v2.4.0 — 2026-08-15

### Added
- Session management: view and revoke active user sessions
- Password strength policy enforced on user creation and admin password changes
- Request correlation ID and MDC logging on every backend request/log line
- Real-backend E2E suite (Cypress, against a full local k8s deployment) alongside the existing mocked tier
- Full quality-metrics reporting suite for both frontend and backend: accessibility audit (axe-core), mutation testing, secrets scanning, license compliance, dependency vulnerabilities (SCA), bundle size, dependency staleness, and E2E run/flakiness reports, plus `npm run reports:all` (frontend), `bash scripts/run-all-quality-reports.sh` (backend), and a combined top-level `scripts/run-all-quality-reports.sh` to run everything in one command
- 80% code coverage gate and a cyclomatic-complexity gate (backend and frontend), enforced during code review
- `gitflow` skill: `sync-feature` action, wired into `feature-development`
- `feature-development` skill: conditional E2E verification phase

### Changed
- Frontend upgraded to Angular 22, with unit and E2E tests migrated to Cypress
- Frontend migrated to zoneless change detection
- The real E2E tier now runs against a local k8s redeploy instead of docker-compose

### Fixed
- Refresh-token cookie path
- CI build hardened, keeping zone.js as a devDependency for Cypress Component Testing

## v2.3.0 — 2026-07-22

### Added
- Global error handler: consistent `{ status, message, error, timestamp }` JSON body on every 4xx/5xx backend response, plus an Angular `ErrorHandler` override with a `MatSnackBar` notification for unhandled frontend errors
- Six new Claude Code skills: `dependency-upgrade`, `incident-response`, `kafka-events-conventions`, `redis-caching-conventions`, `spec-compliance-check`, `web-docs-sync`, `feature-plan`
- `gitflow` skill: new `cleanup-branches` action; falls back to GitHub MCP tools when the `gh` CLI isn't available

### Changed
- Feature backlog moved to `docs/backlog/` and given Priority/Schema-Change/Effort/Area planning attributes per feature
- Various `feature-development`/`gitflow` workflow refinements (version-bump collapsing, branch-name normalization, free-text feature-pick validation)

### Fixed
- `AuthController` no longer swallows `AuthenticationException`/`InvalidRefreshTokenException` into a bodyless 401 — both now return the structured error body
- `gitflow`: owner:branch format for GitHub MCP merge-status checks
- `feature-development`: resume-status check ordering, `CHANGED_FILES` staleness across review rounds, stale-diff PR descriptions, archive data-loss ordering, `AskUserQuestion` option-cap violations, subagent-backgrounding/note-truncation issues
