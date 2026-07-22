[← Back to README](../README.md)

# Architecture

## System Architecture

```mermaid
graph TB
    subgraph browser["Browser"]
        Angular["Angular 19 SPA\nGallery · Albums · Sync · Convert · Duplicates · Recycle Bin\nAnalytics · Audio Player"]
    end

    subgraph backend["Backend — port 8080"]
        SpringBoot["Spring Boot 3 REST API\n(Java 21)"]
        KPL["KafkaProgressListener\n(sse-broadcaster group)"]
        RLF["RateLimitFilter\n(Bucket4j)"]
    end

    subgraph messaging["Messaging — port 9092"]
        Kafka["Apache Kafka\n(KRaft, apache/kafka:3.9.0)\njob.catalog.progress · job.sync.progress\njob.convert.progress · asset.cataloged · asset.deleted"]
    end

    subgraph persistence["Persistence"]
        PG[("PostgreSQL 18\nphotomanager")]
        Mongo[("MongoDB 8\nasset_audit_log")]
        Redis[("Redis 7\nthumbnail + query caches\nrefresh tokens · rate limits")]
        FS[("File System\nimages + thumbnails")]
    end

    Angular -->|"HTTP REST (JSON)"| SpringBoot
    Angular -->|"SSE (EventSource)"| SpringBoot
    SpringBoot -->|"JDBC / JPA (Hibernate)"| PG
    SpringBoot -->|"audit log writes"| Mongo
    SpringBoot -->|"cache/session ops"| Redis
    RLF -->|"token buckets"| Redis
    SpringBoot -->|"File I/O"| FS
    SpringBoot -->|"publish progress + domain events"| Kafka
    Kafka -->|"consume progress events"| KPL
    Kafka -->|"consume domain events"| Mongo
    KPL -->|"SseEmitter.send()"| SpringBoot
```

## Backend Hexagonal Architecture

The backend follows **Hexagonal (Ports and Adapters) Architecture** with strict, unidirectional layer dependencies enforced by package naming.

```mermaid
graph LR
    subgraph WEB["infrastructure/web/"]
        C["Controllers\n(primary adapters)"]
        WDTO["HTTP DTOs\n+ MapStruct mappers"]
    end
    subgraph APP["application/usecase/"]
        UC["Use-case Impls\n(one class per interface\n@Service @Transactional)"]
        ADTO["Application DTOs\n(AssetFilter,\nPaginatedResult…)"]
    end
    subgraph D["domain/"]
        PI["port/in/\n(use-case interfaces)"]
        PO["port/out/\n(repository + service ports)"]
        M["model/\n(pure POJOs)"]
        E["enums/"]
    end
    subgraph INFRA["infrastructure/persistence/ + service/"]
        PA["Persistence Adapters\n(XxxRepositoryImpl)"]
        SA["Service Adapters\n(StorageServiceAdapter,\nJwtTokenAdapter…)"]
        JPA["Spring Data JPA\n+ @Entity classes"]
        EM["MapStruct entity\nmappers"]
    end

    C -->|"calls"| PI
    C --> WDTO
    UC -.->|"implements"| PI
    UC -->|"injects"| PO
    UC --> ADTO
    PA -.->|"implements"| PO
    SA -.->|"implements"| PO
    PA --> JPA
    PA --> EM
    PO --> M
    PI --> M
```

**Dependency flow:** `infrastructure/web → application/usecase → domain ← infrastructure/persistence | infrastructure/service`

The domain layer (`domain/model/`, `domain/port/in/`, `domain/port/out/`) has zero `jakarta.*`, `org.springframework.*`, or infrastructure imports.

Controllers in `infrastructure/web/controller/` delegate directly to use-case interfaces and never touch repositories or service adapters directly.

**Naming conventions:**
- Repository port interfaces: `XxxRepository` (in `domain/port/out/`) → `XxxRepositoryImpl` (in `infrastructure/persistence/adapter/`)
- Service port interfaces: `XxxPort` (in `domain/port/out/`) → `XxxServiceAdapter` (in `infrastructure/service/`)
- All entity↔domain and DTO↔domain conversions go through MapStruct-generated mappers; the `toEntityRef` pattern is used for FK-only references to avoid accidental updates to the referenced row

## Database Schema

```mermaid
erDiagram
    folders {
        bigserial folder_id PK
        text path UK
    }
    assets {
        bigserial asset_id PK
        bigint folder_id FK
        text file_name
        bigint file_size
        integer pixel_width
        integer pixel_height
        text image_rotation
        text hash
        integer rating
        timestamp file_creation_date_time
        timestamp deleted_at
    }
    asset_exif {
        bigint asset_id PK, FK
        text camera_make
        text camera_model
        text lens_model
        text exposure_time
        double f_number
        integer iso_speed
        double focal_length
        timestamp date_taken
        integer width_pixels
        integer height_pixels
        double gps_latitude
        double gps_longitude
        jsonb raw_exif
    }
    users {
        uuid id PK
        varchar username UK
        text password_hash
        varchar role
        timestamp created_at
    }
    albums {
        bigserial album_id PK
        uuid user_id FK
        text name
        text description
        timestamp created_at
    }
    album_assets {
        bigint album_id FK
        bigint asset_id FK
    }
    refresh_tokens {
        bigserial id PK
        uuid user_id FK
        text token_hash
        timestamp expires_at
    }
    search_presets {
        bigserial preset_id PK
        uuid user_id FK
        text name
        text search_criteria
        timestamp created_at
    }
    sync_assets_directories_definitions {
        bigserial id PK
        text source_directory
        text destination_directory
        boolean include_sub_folders
        boolean delete_assets_not_in_source
    }
    convert_assets_directories_definitions {
        bigserial id PK
        text source_directory
        text destination_directory
        boolean include_sub_folders
        boolean delete_assets_not_in_source
    }
    tags {
        serial tag_id PK
        varchar name UK
    }
    asset_tags {
        bigint asset_id FK
        bigint tag_id FK
    }
    user_preferences {
        uuid user_id PK, FK
        varchar theme_mode
        timestamptz updated_at
    }

    folders ||--o{ assets : "contains"
    assets ||--o| asset_exif : "has EXIF"
    users ||--o{ albums : "owns"
    users ||--o{ refresh_tokens : "has"
    users ||--o{ search_presets : "owns"
    users ||--o| user_preferences : "has"
    albums }o--o{ assets : "album_assets"
    assets }o--o{ tags : "asset_tags"
```

The `asset_audit_log` collection (user-action history) lives in MongoDB, not PostgreSQL, so it isn't part of this diagram — see [Persistence](backend.md#persistence).

## Frontend Component Hierarchy

All routes are lazy-loaded via Angular's `loadComponent()`. Every route except `/login` is protected by `authGuard`, which redirects unauthenticated users to `/login`.

```mermaid
graph TD
    App["AppComponent\n(Shell + Navigation Bar)"]

    App --> Login["LoginComponent\n/login — public"]
    App --> Home["HomeComponent\n/home — dashboard"]
    App --> Gallery["GalleryComponent\n/gallery"]
    App --> Sync["SyncComponent\n/sync"]
    App --> Convert["ConvertComponent\n/convert"]
    App --> Duplicates["DuplicatesComponent\n/duplicates"]
    App --> Albums["AlbumsComponent\n/albums"]
    App --> RecycleBin["RecycleBinComponent\n/recycle-bin"]
    App --> UserAdmin["UserAdminComponent\n/admin/users"]
    App --> Analytics["AnalyticsComponent\n/analytics"]

    Gallery --> FolderNav["FolderNavComponent\n(folder tree sidebar)"]
    Gallery --> Thumbnail["ThumbnailComponent\n(shared card)"]
    Gallery --> AudioPlayer["AudioPlayerComponent\n(playback controls)"]
    Albums --> AlbumDetail["AlbumDetailComponent\n/albums/:id"]
    AlbumDetail --> Thumbnail
```

The default route (`/`) redirects to `/home`.

## Project Structure

```
JPPhotoManagerWeb/
├── backend/            # Java 21 + Spring Boot 3 Maven project
│   ├── Dockerfile      # Multi-stage build (Maven → JRE Alpine)
│   └── .dockerignore
├── frontend/           # Angular 19 npm project
│   ├── Dockerfile      # Multi-stage build (Node → Nginx Alpine)
│   ├── nginx.conf      # Serves SPA + reverse-proxies /api to backend
│   └── .dockerignore
├── grafana/provisioning/  # Grafana datasource + dashboard provisioning
├── prometheus.yml         # Prometheus scrape config
├── docker-compose.yml     # Orchestrates db, kafka, redis, mongo, backend,
│                          # frontend, prometheus, and grafana
├── .env.example           # Template for local Docker Compose configuration
├── k8s/                   # Kubernetes manifests (one StatefulSet/Deployment
│   │                      # + Service per docker-compose service)
│   ├── namespace.yaml
│   ├── configmap.yaml
│   ├── secret.yaml.example  # Template — copy to secret.yaml (git-ignored)
│   ├── catalog-volumes.yaml.example  # Template — copy to catalog-volumes.yaml
│   │                                 # (git-ignored); patched onto backend.yaml
│   ├── postgres.yaml         # `db`        → headless Service + StatefulSet
│   ├── kafka.yaml             # `kafka`     → headless Service + StatefulSet
│   ├── redis.yaml             # `redis`     → Service + Deployment
│   ├── mongo.yaml             # `mongo`     → headless Service + StatefulSet
│   ├── backend.yaml           # `backend`   → Service + Deployment + PVC
│   │                          # (no catalog hostPath — see catalog-volumes.yaml)
│   ├── frontend.yaml          # `frontend`  → Service + Deployment
│   ├── prometheus.yaml        # `prometheus`→ Service + Deployment
│   ├── grafana.yaml           # `grafana`   → Service + Deployment + PVC
│   └── ingress.yaml           # Routes external traffic to `frontend`
├── kustomization.yaml     # Entry point: `kubectl apply -k .`
└── scripts/               # Helper shell scripts (all cd to JPPhotoManagerWeb/ on their own)
    ├── build-and-deploy-k8s.sh # Builds images and applies the Kubernetes stack end-to-end
    ├── cleanup-k8s.sh          # Tears down the Kubernetes stack and locally built images
    ├── port-forward-k8s.sh     # Starts background port-forwards for Grafana/PostgreSQL/MongoDB/Kafka
    └── migrate-db.sh           # One-time migration of a host PostgreSQL catalog into Docker Compose
```

[← Back to README](../README.md)
