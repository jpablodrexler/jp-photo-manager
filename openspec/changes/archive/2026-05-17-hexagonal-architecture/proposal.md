## Why

The backend currently follows a layered architecture that mixes framework concerns into the domain layer. JPA annotations (`@Entity`, `@Table`, `@Id`) are embedded directly in domain entities; repository interfaces extend `JpaRepository` and import Spring Data types; and the single `PhotoManagerFacade` orchestrator imports both Spring MVC types (`MultipartFile`, `Page`) and API-layer DTOs. These leaks make the business logic tightly coupled to the Spring and JPA frameworks, making it hard to test in isolation, hard to reason about boundaries, and hard to replace infrastructure components.

Hexagonal Architecture (Ports & Adapters) solves this by placing a strict inversion-of-control boundary at every point where the domain touches the outside world — whether that is an HTTP request, a database, a file system, or a scheduled job.

## What Changes

- **Domain layer becomes pure Java** — entities are replaced by plain POJO domain models; JPA entities move to the infrastructure layer.
- **Repositories become port interfaces** — the Spring Data repository interfaces move out of the domain; they are replaced by plain Java interfaces (`port/out/`) that express only what the domain needs. Spring Data adapters in the infrastructure implement those interfaces.
- **The fat `PhotoManagerFacade` is decomposed** into focused use-case interfaces (`port/in/`) — one interface per cohesive operation group — with Spring-free implementation classes in the `application/usecase/` package.
- **The `api/` package is reorganised** into `infrastructure/web/` to make clear that HTTP controllers are an adapter, not a peer layer alongside the domain.
- **Package structure is reorganised** to reflect hexagonal conventions: `domain/`, `application/usecase/`, `infrastructure/persistence/`, `infrastructure/web/`, `infrastructure/service/`, `infrastructure/config/`.

## Capabilities

### New Capabilities
None — this is a structural refactor. All existing features are preserved.

### Modified Capabilities
- `hexagonal-architecture`: The backend package structure and dependency graph are reorganised to conform to the Ports & Adapters (Hexagonal Architecture) pattern. The domain layer becomes framework-free; all framework dependencies are isolated in the infrastructure layer.

## Impact

- **Backend only:** No changes to the REST API surface, database schema, or frontend.
- **No breaking API changes:** All existing endpoints, request/response shapes, and authentication behaviour are preserved.
- **Tests:** Existing unit tests will need import paths updated; domain unit tests will gain the ability to test use cases without any Spring or JPA mocking.
- **CLAUDE.md and README.md:** Architecture diagrams and package descriptions are updated to document the new structure.
