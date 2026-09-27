# Java Developer — Layer-by-Layer Patterns — Web, Application & Domain

_Part of the `java-developer` skill — see `../SKILL.md` for the topic index. Load this file only when your current task touches this topic._

## 6. Layer-by-Layer Patterns

### 6.1 Web Layer — Controllers & HTTP DTOs

- **Thin controllers:** inject use-case interfaces and delegate immediately.
- Return `ResponseEntity<T>` with correct HTTP status codes (200, 201, 204, 404, 500).
- Use MapStruct mappers in `infrastructure/web/mapper/` for HTTP DTO ↔ domain model conversions.
- Use `SseEmitter` for streaming progress of long-running operations.
- **DTO placement:** every class in `infrastructure/web/dto/` lives in exactly one of
  `request/`, `response/`, or `shared/` — never directly in `web/dto/`. Classify by usage,
  not by guessing from the name: a class is `request/` only if it is exclusively an incoming
  `@RequestBody`/`@RequestParam` payload; `response/` only if it is exclusively an outgoing
  `ResponseEntity<...>`/return-type payload (including when nested inside another response
  DTO); `shared/` only if the *exact same class* is verified to appear as both, across every
  controller method that references it.

```java
@Tag(name = "Assets", description = "Photo and video asset management")
@RestController
@RequestMapping("/api/assets")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@Slf4j
public class AssetController {

    private final GetAssetsUseCase getAssetsUseCase;
    private final AssetWebMapper assetWebMapper;   // MapStruct

    @Operation(summary = "List assets in a folder")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Paginated asset list"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping
    public ResponseEntity<PaginatedResult<AssetResponseDto>> getAssets(
            @RequestParam String folderPath,
            @RequestParam(defaultValue = "0") int page) {
        PaginatedResult<Asset> result = getAssetsUseCase.execute(folderPath, page);
        return ResponseEntity.ok(assetWebMapper.toDto(result));
    }
}
```

**HTTP response DTO pattern** (in `infrastructure/web/dto/response/`):

```java
@Data
public class AssetResponseDto {
    private Long assetId;
    private String fileName;
    private String thumbnailUrl;
}
```

**HTTP request DTO pattern** (in `infrastructure/web/dto/request/`):

```java
public record RateAssetRequestDto(@Min(0) @Max(5) int rating) {}
```

### 6.2 Application Layer — Use Cases

- One implementation class per use-case interface.
- `@Service @Transactional` owns the transaction boundary.
- Injects only `domain/port/out/` interfaces — no JPA, no Spring MVC.
- Does not contain business logic — orchestrates ports.

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class GetAssetsUseCaseImpl implements GetAssetsUseCase {

    private final AssetRepository assetRepository;   // domain port

    @Override
    @Transactional(readOnly = true)
    public PaginatedResult<Asset> execute(String folderPath, int page) {
        return assetRepository.findFiltered(new AssetFilter(folderPath, page));
    }
}
```

