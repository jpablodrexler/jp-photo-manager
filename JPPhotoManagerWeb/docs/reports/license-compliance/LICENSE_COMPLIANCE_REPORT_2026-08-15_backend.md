# License Compliance Report (backend) — 2026-08-15

**Commit:** a62ecce
**Generated:** 2026-08-15T20:30:04Z
**Scope:** backend Maven dependency tree, excluding test scope (license-maven-plugin add-third-party)

**147 package(s) scanned, 0 need action, 1 previously reviewed and accepted.**

## Previously reviewed and accepted

| Component | License(s) | Why it's accepted |
| --- | --- | --- |
| jaudiotagger (net.jthink:jaudiotagger:3.0.1 - https://bitbucket.org/ijabz/jaudiotagger) | LGPL | LGPLv3. Confirmed compile-scope, unmodified dependency (no vendored/patched jaudiotagger source anywhere in this repo) consumed only through its public API in a single adapter class (infrastructure/service/AudioMetadataService.java, ~15 lines: AudioFileIO.read() plus getters on the returned AudioFile/Tag/Artwork) — exactly the unmodified dynamic-linking case LGPL permits without imposing copyleft on the consuming application. The only realistic permissive alternative (mp3agic, MIT) is MP3/ID3-only, so swapping would risk a functional regression for FLAC/OGG/M4A files versus jaudiotagger's format-agnostic reader, for zero compliance benefit. |

