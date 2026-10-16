# 0001. Replace the project's workflow skills with backlog-kit's and keep the project-specific checks in the reference docs

**Date:** 2026-10-14
**Status:** Accepted

## Context

This project grew its own copy of the workflow skills (planning, the feature and bug pipelines and their batch runners, `gitflow`, the reviewers, `spec-compliance-check`, `dependency-upgrade`, `decision-record` and `web-docs-sync`) in parallel with its sibling project pablo-web, and the two copies drifted. The shared part was extracted as the open source [backlog-kit](https://github.com/jpablodrexler/backlog-kit), which pablo-web has already adopted. What only this project knows (the Spring and Angular checklists, the Flyway rules, the file-system security rules, the deploy manifest parity rule, the custom metrics rule) was written inside those skills.

## Decision

Delete the 22 workflow skills in `.claude/skills/` and install the kit's (the 12 core skills and the 10 extras, `web-docs-sync` becoming `docs-sync`). The stack skills (`java-developer`, `angular-developer`, the Cypress, JUnit, `e2e-*`, Kafka and Redis skills, `quality-metrics`, `incident-response`, `feature-gantt`, the `openspec-*` skills) stay.

The project-specific checks move out of the skills into named sections of the reference docs under `JPPhotoManagerWeb/docs/`, which the kit's skills read by name; the review section numbers (§N) are unchanged:

- `architecture.md`: `Architecture review checks` (P1–P16)
- `backend.md` and `frontend.md`: `Code review checks` (each §N lives in one of them), plus `Spec compliance checks` and the shared and backend parts of `Dependency upgrade checks` in `backend.md` and the frontend survey in `frontend.md`
- `security.md` (renamed from `authentication.md`): `Security review checks`
- `database.md` (new): `Database review checks`
- `product.md` (new): `Product scope review checks`

The two checks `web-docs-sync` made beyond documentation (deploy manifest parity between `docker-compose.yml` and `k8s/configmap.yaml`, and custom metrics against the Grafana dashboard) became sections of `kubernetes.md` and `backend.md`'s Observability. What each doc is compared with is the "Checked against" column of the `Kit conventions` table in `JPPhotoManagerWeb/CLAUDE.md`. Because the product lives in `JPPhotoManagerWeb/`, `.backlog-kit.toml` at the repository root sets `backlog_dir = "JPPhotoManagerWeb/docs/backlog"`, which makes `JPPhotoManagerWeb/` the project root for the skills (kit 0.1.1); the WPF application and its root `docs/` are outside them.

## Alternatives considered

- **Keep the project skills and only publish the kit** — rejected: two copies of the workflow would keep drifting apart.
- **Keep the project knowledge as files under a `.backlog-kit/` folder** — rejected: those files were not a standard place for Claude Code; the reference docs and `CLAUDE.md` already are, and the docs gain a section they should have had.
- **One `Code review checks` section in a single doc** — rejected: the kit reads `frontend.md` and `backend.md` per side, so each side's checks live with that side's doc.

## Consequences

- The kit's skills are the same text in both projects; the differences live in each project's docs and `CLAUDE.md`.
- `gitflow` has no `commit` action: commits are made by the person.
- A reviewer that finds its section missing says so in its report and falls back to its generic frame.
- Reverting the migration commit restores the previous skills and `authentication.md`.
