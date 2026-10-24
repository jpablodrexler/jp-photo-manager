# Feature 71 — webdav-server

**As a** photo library owner, **I want** the catalog mounted as a network drive over WebDAV, **so that** I use my library from the file manager of my operating system.

## Acceptance Criteria
- AC1: Given /webdav/, when a client sends PROPFIND, then the virtual folder tree mirrors the asset folder hierarchy.
- AC2: Given an asset path, when a client sends GET, then the original file is streamed.
- AC3: Given a folder path, when a client sends PUT, then the file enters the same pipeline as the upload; MKCOL creates a folder.
- AC4: Given an asset, when a client sends DELETE, then it goes through the soft-delete path.
- AC5: Given the credentials, when a client connects, then it authenticates with HTTP Basic against the existing users.
- AC6: Given Windows, macOS and Linux clients, when they map the drive, then it works.

## Technical notes

Expose the catalog over WebDAV (`PROPFIND`, `GET`, `PUT`, `DELETE`, `MKCOL`) at `/webdav/` so users can mount the catalog as a network drive on Windows (Map Network Drive), macOS (Finder → Connect to Server), and Linux (`davfs2`); the virtual folder tree mirrors the `assets.folder_path` hierarchy; `GET` on an asset path streams the original file via the existing `StoragePort`; `PUT` to any folder path triggers the same catalog pipeline as drag-and-drop upload (#3); `DELETE` routes through the soft-delete path from `soft-delete-recycle-bin` (#9); implemented with `milton-server-ce` (pure Java WebDAV server, no servlet dependency conflicts) or a custom Spring MVC `WebdavController` handling raw `PROPFIND` XML via JAXB; authentication uses HTTP Basic over the existing `UserDetailsService` — JWT cookies are not compatible with native OS WebDAV clients; no schema change

## Definition of Done

Project-level Definition of Done applies (`definition-of-done.md`). Additional for this feature:
- None
