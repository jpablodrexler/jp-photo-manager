[← Back to README](../README.md)

# Authentication

The application uses **JWT stored in an HttpOnly cookie** (`SameSite=Strict`, `Path=/`), alongside a longer-lived `refreshToken` HttpOnly cookie used only to obtain a new JWT. All `/api/**` endpoints require the `jwt` cookie except the three under `/api/auth/` below. Because the browser attaches cookies automatically to every same-origin request — including `<img src="...">` image loads and the native `EventSource` API — no custom `Authorization` header is needed and there is no risk of token theft via JavaScript.

## Public endpoints

| Method | Path | Description |
|---|---|---|
| `POST` | `/api/auth/login` | Authenticate; sets `jwt` + `refreshToken` HttpOnly cookies and returns `{ "username": "...", "expiresAt": "..." }` |
| `POST` | `/api/auth/refresh` | Reads the `refreshToken` cookie, rotates both cookies, and returns the same response shape as login |
| `POST` | `/api/auth/logout` | Clears both cookies server-side |

`GET /api/auth/me` also exists but is **not** public — it requires the `jwt` cookie and returns the current user's `{ "username": "...", "role": "..." }`.

## JWT flow

```mermaid
sequenceDiagram
    participant User
    participant Angular
    participant API as Spring Boot API
    participant DB as PostgreSQL

    User->>Angular: Navigate to protected route
    Angular->>Angular: authGuard checks localStorage for session
    Angular-->>User: Redirect to /login (no valid session)

    User->>Angular: Submit credentials
    Angular->>API: POST /api/auth/login {username, password}
    API->>DB: Look up user, verify BCrypt hash
    DB-->>API: User record
    API-->>Angular: Set-Cookie jwt=[token] and refreshToken=[token] (HttpOnly, SameSite=Strict) + {username, expiresAt}
    Angular->>API: GET /api/auth/me (cookie sent by browser)
    API-->>Angular: {username, role}
    Angular->>Angular: Store {username, expiresAt, role} in localStorage
    Angular->>Angular: Schedule proactive refresh at (expiresAt − 5 min)
    Angular-->>User: Redirect to /home

    Note over Angular,API: Cookie is sent automatically with every subsequent request

    Angular->>API: GET /api/assets (cookie sent by browser)
    API->>API: JwtAuthenticationFilter validates cookie
    API-->>Angular: 200 OK + data

    Angular->>API: POST /api/auth/logout
    API-->>Angular: Clears jwt and refreshToken cookies (Max-Age=0)
    Angular->>Angular: Clear localStorage + cancel refresh timer
    Angular-->>User: Redirect to /login
```

## Configuration properties

| Property | Default | Description |
|---|---|---|
| `photomanager.jwt-secret` | *(empty — must be set)* | HS256 signing secret (≥ 32 bytes) |
| `photomanager.jwt-expiry-hours` | `24` | JWT access-token validity in hours |
| `photomanager.refresh-token-expiry-days` | `30` | Refresh-token validity in days |

## Generating JWT_SECRET

Generate a cryptographically random 32-byte base64 string using the command for your platform:

**Linux / macOS:**
```bash
openssl rand -base64 32
```

**Windows (PowerShell):**
```powershell
$bytes = New-Object byte[] 32
[System.Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($bytes)
[Convert]::ToBase64String($bytes)
```

## Setup (local development)

1. Copy `src/main/resources/application-local.yml.example` to `src/main/resources/application-local.yml`
2. Generate a secure secret using the command for your platform (see [Generating JWT_SECRET](#generating-jwt_secret)) and paste the output into `photomanager.jwt-secret` in `application-local.yml`

> **Important:** The application **will not start** if `photomanager.jwt-secret` is blank. `application-local.yml` is git-ignored and must never be committed.

## Setup (Docker Compose)

Set `JWT_SECRET` in `JPPhotoManagerWeb/.env` using the command for your platform:

**Linux / macOS:**
```bash
echo "JWT_SECRET=$(openssl rand -base64 32)" >> JPPhotoManagerWeb/.env
```

**Windows (PowerShell):**
```powershell
$bytes = New-Object byte[] 32
[System.Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($bytes)
$secret = [Convert]::ToBase64String($bytes)
Add-Content -Path JPPhotoManagerWeb\.env -Value "JWT_SECRET=$secret"
```

## Configuring multiple catalog root folders

The backend accepts a semicolon-separated list of root directories via the `photomanager.root-catalog-folders` property. Every directory in the list is scanned recursively when the catalog runs.

### Local development (`application-local.yml`)

Add or extend the `root-catalog-folders` key in `src/main/resources/application-local.yml`:

```yaml
photomanager:
  jwt-secret: "…"
  initial-directory: "C:/Users/yourname/Pictures"
  root-catalog-folders: "C:/Users/yourname/Pictures;C:/Users/yourname/OneDrive/ExtraFolder"
```

`initial-directory` controls which folder is shown first when the gallery loads; it can be any one of the roots (or any sub-folder).

### Docker Compose

Each additional directory must be:

1. Declared in `.env`:
   ```
   HOST_IMAGE_DIR=C:/Users/yourname/Pictures
   HOST_IMAGE_DIR_2=C:/Users/yourname/OneDrive/ExtraFolder
   ```

2. Added as a second bind mount in `docker-compose.yml` under the `backend` service:
   ```yaml
   volumes:
     - type: bind
       source: ${HOST_IMAGE_DIR}
       target: /catalog
     - type: bind
       source: ${HOST_IMAGE_DIR_2}
       target: /catalog2
   ```

3. Included in `CATALOG_DIR` (also in `docker-compose.yml`):
   ```yaml
   environment:
     CATALOG_DIR: /catalog;/catalog2
   ```

After editing both files, recreate the backend container and run the catalog:

```bash
docker compose up -d --force-recreate backend
```

Then click **Run Catalog** in the UI. Repeat steps 1–3 for any further directories (`HOST_IMAGE_DIR_3` → `/catalog3`, etc.).

### Cloud storage paths (Google Drive, OneDrive, etc.)

Docker bind mounts only work with real filesystem paths — they cannot reach cloud storage virtual filesystems, regardless of offline-availability settings.

**Google Drive for Desktop (Windows):** maps your Drive to a virtual drive letter (e.g. `G:\`) using the Windows Cloud Files API. Even when a folder is marked *Available offline* the files are cached locally but still served through this virtual layer. Docker Desktop on Windows runs inside a WSL2 Linux VM, which has no access to virtual drive letters — `G:\My Drive\Photos` is invisible from inside the container, so `/catalog3` will appear empty and no assets will be indexed.

**Google Drive via FUSE (Linux/macOS — rclone, google-drive-ocamlfuse, GNOME GVFS):** FUSE mounts live in the host mount namespace and are not propagated into Docker containers by default. The container again sees an empty directory.

In both cases the `CatalogFolderPartitioner` finds the mount point, sees no files, and silently produces no catalog entries — no error is surfaced.

**Workarounds:**

| Approach | Works on | Notes |
|---|---|---|
| Point to the Backup & Sync local folder (`C:\Users\yourname\Google Drive\…`) | Windows | Only the older *Google Backup and Sync* client stores files as plain NTFS files at an addressable path. Google Drive for Desktop does not. |
| `rclone sync gdrive:FolderName /local/mirror` on a schedule | All | Copy files to a real local path first, then set `HOST_IMAGE_DIR_N` to that path. Most reliable option for the current Google Drive for Desktop client. |
| `robocopy G:\My Drive\FolderName C:\local\mirror /MIR` on a schedule | Windows | Windows-native alternative to rclone. |

## Default admin user

On first startup, if no users exist in the database, the application automatically creates a default administrator:

| Username | Password |
|---|---|
| `admin` | `admin` |

**Change this password immediately** after first login using the **User Administration** page (`/admin/users`).

## User Administration

Navigate to **Users** in the navigation bar (or `/admin/users`) to:
- View all users
- Add new users
- Change a user's password
- Delete users

There is no self-registration; all user management is done by an authenticated administrator.

[← Back to README](../README.md)
