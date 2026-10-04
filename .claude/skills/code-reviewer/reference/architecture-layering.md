# Code Reviewer — Architecture & Layering (Both Sub-Projects)

_Part of the `code-reviewer` skill — see `../SKILL.md` for the topic index, the layer table and the severity legend. Load this file only when your review touches this topic._

## 1. Architecture & Layering (both sub-projects)

### 1.1 Backend dependency flow

The backend uses **hexagonal (ports and adapters)** architecture. Allowed imports:

```
infrastructure/web/      → may import application/usecase/, domain/
application/usecase/     → may import domain/ only
domain/                  → must NOT import application/, infrastructure/
infrastructure/persistence/
infrastructure/service/  → may import domain/ only
```

🔴 Flag any class in `domain/` that imports from `application/`, `infrastructure/`,
or any Spring / JPA annotation.

🔴 Flag any controller that contains business logic instead of delegating
immediately to a use-case interface from `domain/port/in/`.

🔴 Flag any JPA `@Entity` class placed in `domain/` — entities belong in
`infrastructure/persistence/entity/`; domain has pure POJOs in `domain/model/`.

🟡 Flag any use-case implementation that injects a Spring Data JPA interface
directly — it must go through a `domain/port/out/` repository interface.

### 1.2 Backend port / adapter split

The project has three port/adapter pairs; all must follow the naming rules:

| Role | Interface location | Naming | Adapter location | Naming |
|------|--------------------|--------|-----------------|--------|
| Use case (driving) | `domain/port/in/<pkg>/` | `FooUseCase` | `application/usecase/<pkg>/` | `FooUseCaseImpl` |
| Service port (driven) | `domain/port/out/` | `FooPort` | `infrastructure/service/` | `FooServiceAdapter` |
| Repository port (driven) | `domain/port/out/` | `FooRepository` | `infrastructure/persistence/adapter/` | `FooRepositoryImpl` |

🔴 Flag any adapter class that is injected directly instead of its port interface.

🔴 Flag any use-case implementation that injects another use-case implementation
directly — use-cases must be composed via port interfaces only.

🟡 Flag any port interface that lives outside `domain/port/in/` or `domain/port/out/`.

🟡 Flag a port/adapter pair whose adapter does nothing but delegate to
*another* port for the same capability (e.g. `FooAdapter.doThing()` just
calls `barPort.doThing()`) — that's not a real abstraction, it's a
pass-through. The test for whether it should exist is whether the adapter
contributes any logic of its own — **not** whether something currently calls
it. Having production callers doesn't save a pure-delegation wrapper: delete
the port and adapter, and repoint every caller to depend on the port/adapter
that actually implements the behavior instead. Only keep the pair if the
adapter does real, non-trivial work beyond forwarding the call. This is a
cross-cutting antipattern, not backend-specific — the same test applies to
any delegate-only wrapper in either sub-project (see §10.2 for the frontend
form) — see the `HashCalculatorPort` / `AssetHashCalculatorAdapter` and
`AudioPlayerService` entries in §15.

### 1.3 Frontend layer rules

```
core/       → services and models only; no UI components
features/   → page-level smart components; calls core services and shared components
shared/     → pure presentational components and pipes; no service calls
```

🔴 Flag any component in `shared/` that injects or calls a service directly.

🟡 Flag any service placed under `features/` — it belongs in `core/services/`.

---
