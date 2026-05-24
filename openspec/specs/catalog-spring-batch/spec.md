# catalog-spring-batch Specification

## Purpose
TBD - created by archiving change catalog-spring-batch. Update Purpose after archive.
## Requirements
### Requirement: Catalog uses Spring Batch with parallel folder processing

The catalog job SHALL use a `PartitionedStep` to process multiple folders concurrently, with a configurable grid size.

#### Scenario: Multiple folders processed in parallel

- **GIVEN** `photomanager.catalog-partition-grid-size = 4` and 10 folders to catalog
- **WHEN** the catalog job runs
- **THEN** up to 4 folders are processed concurrently; the remaining folders are processed as threads become available; all 10 folders are cataloged

### Requirement: Assets are committed in configurable chunks

The catalog job SHALL commit assets in chunks of `photomanager.catalog-chunk-size` (default 50) rather than one transaction per folder.

#### Scenario: Chunk size controls transaction boundaries

- **GIVEN** `photomanager.catalog-chunk-size = 50` and a folder with 120 new files
- **WHEN** the catalog job processes that folder
- **THEN** three transactions are committed: one for files 1-50, one for 51-100, one for 101-120

### Requirement: Spring Batch job execution tables replace catalog_run_state

The Flyway V27 migration SHALL create Spring Batch's standard schema tables and drop the `catalog_run_state` table.

#### Scenario: V27 migration succeeds

- **GIVEN** an existing database with `catalog_run_state`
- **WHEN** Flyway V27 runs
- **THEN** the 9 Spring Batch tables (`BATCH_JOB_INSTANCE`, `BATCH_JOB_EXECUTION`, etc.) are created and `catalog_run_state` is dropped

### Requirement: SSE progress notifications continue to work

Catalog SSE progress events SHALL be delivered to the connected frontend client during the Spring Batch job execution.

#### Scenario: SSE client receives progress events from Spring Batch job

- **GIVEN** a frontend client connected to the catalog SSE endpoint
- **WHEN** the Spring Batch catalog job processes assets
- **THEN** the SSE stream receives `CatalogChangeNotification` events as each asset is written; the `done` event is sent when the job completes

