[← Back to README](../README.md)

# Product

What this product is for and who uses it, and what the `product-scope-roaster` skill tests in this project's scope.

## What it is

A web edition of JP Photo Manager: it catalogs the photo and video folders of one or more people, browses them as a gallery, finds duplicates, syncs and converts files, groups them in albums, and keeps a recycle bin, with real-time progress for the long operations. Everything a user can reach today is in [Features](features.md), and what is planned is in `backlog/features-planned.md`.

## Product scope review checks

Examples already visible in this project's scope, for the roaster to test on their merits (do not assume they hold). The five dimensions are the skill's own; each example belongs to one of them.

### 1. Merge candidates
Two or more features (shipped, planned, or one of each) that are close enough
in purpose or interaction shape that a user would reasonably expect one
screen, one concept, one place to look — not two entry points they have to
learn are actually different things. Look for a near-mirror pair across the
whole scope. Patterns already visible in this backlog worth testing: planned
`database-backup` (#44) and `asset-backup` (#61) — a user hears "backup" once;
two features with two different meanings of it is a naming and discovery
problem before it is anything else. Planned `email-notifications` (#48) and
`notification-center` (#54) both tell a user that the same background
operations (catalog, sync, convert, backup) finished — two notification
surfaces for the same events, with no stated relationship between them.
Planned `image-comparison-viewer` (#50) next to the shipped duplicate-group
side-by-side comparison. Judge each on its merits; don't assume they hold.

### 2. Split candidates
A feature (shipped or planned) whose scope has grown, or is proposed to grow,
past what a user would recognize as "one thing" — usually visible as a single
area quietly accumulating unrelated sub-capabilities. The shipped Gallery
section is the first place to look: thumbnail grid, viewer, folder tree,
timeline, search/filter/sort, rating, tagging, EXIF, move/copy/rename, crop,
upload, ZIP download, add-to-album, soft delete — would a new user recognize
all of that as "browsing my photos," or has it become several products sharing
one screen? Also flag a single planned row whose own description reads as
multiple unrelated deliverables bolted together — a row-level split candidate,
not just an area-level one.

### 3. Missing functionality — gaps in an existing story
Somewhere an area tells half a story: it solves the "find it" half but not the
"and fix it" half, or it covers every sibling case but one. Ground each gap in
the feature's own stated non-goals and deferrals where possible — if a doc says
something is deliberately out of scope, ask "was that the right call, or is
that the thing a real user will ask for first?" Look for it in the planned
backlog too: a planned row that exists *because* a shipped feature is
incomplete (`image-rotation-viewer` (#30) — portrait photos showing sideways in
the shipped viewer — is a fix to a shipped area, not a new capability) tells
you where the shipped story has a hole. Ask what a person would reasonably
expect to do next after each shipped capability and whether the scope lets them.

### 4. Cross-feature consistency & everyday friction
Patterns a user would learn once and then expect to hold everywhere, that
instead quietly differ feature to feature — each instance individually
defensible, the sum of them a trap:
- **Delete reversibility with no visible signal.** Gallery deletes land in the
  Recycle Bin and can be restored; check what Albums, Duplicates, Sync, and any
  planned deleting feature (a WebDAV mount's deletes, an auto-resolve cleanup)
  do. A user who has restored one deleted photo will assume the same safety net
  exists everywhere, and find out otherwise only by losing a file.
- **Edit models that differ.** Shipped Crop saves in place; planned
  `asset-image-editor` (#56) saves a new asset with an optional replace. A user
  who learns one will be surprised by the other.
- **Scattered "where do I do X"** — where background operations (catalog,
  sync, convert, any future backup or video job) are started, where their
  progress and history are seen, and where related settings live. Count the
  places a user has to look.
- Any other "I learned the pattern here, it's different two features over"
  case surfaced while reading — name the two features and the exact behavioral
  difference.

### 5. Scope coherence — what is this product actually for?
Step back from individual features and ask whether the backlog as a whole is
still one coherent thing or is drifting into "whatever seemed buildable next."
Compare the user-facing rows (rating, tagging, comparison, event grouping,
quality scoring) against the rows a user would never see or choose — planned
`kafka-catalog-coordination` (#77) and `mongodb-user-preferences` (#74) change
nothing a photo-manager user can observe — and against the large new product
surfaces (`video-from-images` (#58), `webdav-server` (#71), `archive-support`
(#60)). Say plainly whether the roadmap still reads as "help me find, organize
and protect my photos" or has started reading as "build a media platform," and
name which specific planned rows are the evidence either way.

[← Back to README](../README.md)
