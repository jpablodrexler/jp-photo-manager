# Migration version gaps

The version sequence here has gaps: `V19` → `V26` (V20–V25) and `V27` → `V32`
(V28–V31). These are not missing files — the migrations were abandoned or
reworked during feature development before merge (e.g. an approach was
changed mid-branch, or a change was squashed into a later migration), and
version numbers are never reused or renumbered once a number could plausibly
have been applied in any environment.

`V5__no_op.sql` is the precedent for this: instead of skipping a number
outright, it was kept as an explicit no-op placeholder. Prefer that pattern
for new abandoned migrations going forward, but don't backfill placeholders
for the existing gaps above — that would just be churn.

Never modify an already-applied migration file; always add a new
`V{n}__{Description}.sql` instead (see `CLAUDE.md`).
