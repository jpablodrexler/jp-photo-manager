# Feature 49 — auto-tagging

During cataloging, automatically apply tags derived from EXIF data: the year from `dateTaken` and camera make normalised to lowercase (e.g. `canon`, `sony`, `apple`); tags are written through the existing `asset_tags` table and tag infrastructure; auto-applied tags are indistinguishable from manual ones and can be removed by the user; no new schema required beyond what the tag feature already provides
