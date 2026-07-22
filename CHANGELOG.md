# Changelog

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
