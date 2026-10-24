# Feature 74 — mongodb-user-preferences

**As a** developer of the project, **I want** user preferences and search presets stored as flexible documents, **so that** adding a preference or a preset field needs no database migration.

## Acceptance Criteria
- AC1: Given a user, when their preferences and presets are read, then they come from a single document keyed by user id.
- AC2: Given a new preference, when it is added, then no relational migration is needed and existing presets are not altered.
- AC3: Given the persistence adapters, when they change, then use cases, domain models and controllers behave the same.
- AC4: Given the one-time data migration, when it completes, then the old relational tables are dropped.

## Technical notes

Move `user_preferences` and `search_presets` from rigid PostgreSQL tables to a single MongoDB `user_configs` collection keyed by `userId`; adding a new UI preference (theme, gallery layout, notification toggle) requires no Flyway migration; search preset payloads grow naturally as new filter fields are added without altering existing rows; only the two persistence adapters (`UserPreferenceRepositoryImpl`, `SearchPresetRepositoryImpl`) change — use cases, domain models, and REST controllers are untouched; the existing PostgreSQL tables are dropped after a one-time data migration

## Definition of Done

Project-level Definition of Done applies (`definition-of-done.md`). Additional for this feature:
- None
