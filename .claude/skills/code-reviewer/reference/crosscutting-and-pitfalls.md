# Code Reviewer — Cross-cutting Style & Known Pitfalls

_Part of the `code-reviewer` skill — see `../SKILL.md` for the topic index, the layer table and the severity legend. Load this file only when your review touches this topic._

## 14. Cross-cutting: Comments & Code Style

🟢 Flag comments that restate what the code does (e.g., `// increment counter`
before `counter++`). Only comments explaining **why** (a non-obvious
constraint, workaround, or invariant) are kept.

🟢 Flag multi-line comment blocks or Javadoc on internal methods — terse
single-line comments are preferred, and only when necessary.

🟢 Flag large imperative loops that could be replaced by a stream or array
method.

---

## 15. Known Project-Specific Pitfalls

These have caused real bugs in this codebase and deserve extra attention:

| Pitfall                                    | What to look for                                                                                                                                 |
| ------------------------------------------ | ------------------------------------------------------------------------------------------------------------------------------------------------ |
| `@Transactional` self-invocation           | Method annotated `@Transactional` called from same class without going through proxy                                                             |
| JPA entity in `domain/`                    | `@Entity` class placed under `domain/model/` or `domain/port/` — entities belong in `infrastructure/persistence/entity/`                        |
| Adapter injected directly                  | Caller injects `FooRepositoryImpl` or `FooServiceAdapter` instead of the `domain/port/out/` interface                                           |
| Use case bypassing port interface          | Use-case impl injects another use-case impl directly instead of its `domain/port/in/` interface                                                  |
| Missing catalog directory                  | `root-catalog-folders` pointing to a directory that may not exist on all machines; should use `application-local.yml` for machine-specific paths |
| Lazy association outside transaction       | Accessing `asset.getFolder()` or similar after the session is closed                                                                             |
| SSE emitter not completed                  | `SseEmitter` left open after the async operation finishes                                                                                        |
| `@Async` without `@EnableAsync`            | `@Async` has no effect if `@EnableAsync` is missing from the application class                                                                   |
| Cypress Jasmine matchers                   | Using `toBe` / `toEqual` instead of Chai `to.equal` / `to.deep.equal`                                                                            |
| `*ngIf` / `*ngFor` in new templates        | Angular 17+ control flow must be used; directives are legacy                                                                                     |
| SSE + Spring Security async dispatch       | `SecurityFilterChain` missing `.dispatcherTypeMatchers(DispatcherType.ASYNC).permitAll()` → `AuthorizationDeniedException` on the async thread   |
| Token in URL for SSE / image endpoints     | `EventSource` and `<img>` do not send `Authorization` headers; tokens in query params appear in logs — use HttpOnly cookies instead              |
| SecurityConfig circular dependency         | `@Bean UserDetailsService` defined in `SecurityConfig` while `SecurityConfig` injects a filter that depends on it — extract to separate config   |
| JPA delete-then-insert with stale IDs      | `deleteAll()` + `saveAll(incoming)` when incoming entities still have old IDs → Hibernate merges against deleted rows; use `deleteAllInBatch()` + `setId(null)` |
| CORS missing `PATCH`                       | `AppConfig.corsFilter()` `allowedMethods` omitting `"PATCH"` when `@PatchMapping` endpoints exist → 403 on preflight                            |
| Missing OpenAPI annotations on controller  | New `@RestController` added without `@Tag` / `@Operation` / `@ApiResponses` — controller appears in Swagger UI under "default" with no documentation |
| Hand-written mapper                        | Entity ↔ domain model or HTTP DTO ↔ domain model conversion done manually instead of with a MapStruct `@Mapper(componentModel = "spring")`      |
| DTO placed directly in `web/dto/`          | New HTTP DTO added straight to `infrastructure/web/dto/` instead of its `request/`, `response/`, or `shared/` subpackage, or named without the `RequestDto`/`ResponseDto` suffix |
| Delegate-only port/adapter or service      | A port/adapter (backend) or service (frontend) with no logic of its own, just forwarding to another one for the same capability. The keep-or-delete test is whether it contributes its own logic — **not** whether it currently has callers; existing callers just mean they need repointing to the real implementation, not that the wrapper earns a reprieve. Backend incident: `HashCalculatorPort`/`AssetHashCalculatorAdapter` duplicated `StoragePort.computeHash`'s SHA-256 logic; the first fix made the adapter delegate to `StoragePort` instead of deleting it and migrating callers — the pair was pure pass-through and should have been deleted outright, with any real callers repointed to `StoragePort` directly. Frontend incident: `core/services/audio-player.service.ts` was a bare re-export (`export { MediaPlayerService as AudioPlayerService } from './media-player.service'`) with zero importers anywhere in the codebase — deleted outright |
| Method/function past the complexity threshold | `mvn pmd:check` (backend) or `npm run complexity` (frontend) reports something over 15 — see §18 |
| Line coverage below 80%                    | `mvn jacoco:check` (backend) or `npm run coverage:check` (frontend) reports under 80% — see §19 |
| Untyped (`any`) identifier (frontend)      | `npm run type-coverage:report` lists it — see §20 |
| Unused export/file/dependency              | `npm run dead-code:report` (frontend) or `mvn dependency:analyze` (backend) lists it — see §21 |
| Survived mutant (test runs, doesn't assert) | `npm run mutation:report` (frontend) or `bash scripts/mutation-report.sh` (backend) lists it — see §24 |
| `mat-form-field` errors on blur before any submit attempt | An add-row/optional validated field showing its "required" error on focus-then-blur — gate on an explicit "attempted" signal + per-field `[errorStateMatcher]`, not `control.touched` — see §10.4 |
| Outline `mat-form-field` label clipped or truncated | First field in a scrollable container with no `margin-top` (label clipped at the container edge), or a field hard-sized narrower than its `mat-label` (label ellipsised) — see §10.4 |
| Flex row overlaps or a wrapped button row is ragged at mobile width | Not checked at `angular-developer` §20's standard mobile check device — see §10.4 |

---
