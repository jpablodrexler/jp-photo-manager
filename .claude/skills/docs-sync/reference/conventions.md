<!-- VENDORED from shared/conventions.md — do not edit here; edit the source and run `python3 tools/vendor.py`. -->
# Kit conventions

Where the backlog-kit skills read and write. Every project that uses the kit looks the same; a project keeps only the files it needs, and says in this section of its `CLAUDE.md` where it differs. `CLAUDE.md` must carry this table under the heading `## Kit conventions`, with the "Checked against" column filled in for the project.

## Kit conventions

| Path | What it holds | Checked against |
| ---- | ------------- | --------------- |
| `docs/backlog/` | the backlog: `features-planned.md`, `features-implemented.md`, `bugs-open.md`, `bugs-fixed.md`, plus one brief per item in `features/` and `bugs/`, the optional `definition-of-done.md` checklist and the optional `estimation-anchors.md` (finished features as examples of each story-point value) (the folder is the one setting: `[paths] backlog_dir` in `.backlog-kit.toml`) | — |
| `docs/decisions/` | architecture decision records, numbered and append-only (`decision-record`) | — |
| `docs/explorations/` | notes of ideas still being explored, offered by `features-next` | — |
| `docs/reports/` | dated reports of the reviewers and the batch runs; usually gitignored | — |
| `docs/architecture.md` | system and component diagrams, the data-model diagram, the project structure, and an `Architecture review checks` section (what `architecture-reviewer` checks in this project) | the directory tree, the data schema and the routes or entry points |
| `docs/features.md` | what a user can actually reach today (never the planned backlog) | what is really reachable: the routes, screens and endpoints |
| `docs/product.md` | optional: what the product is for and who uses it, and a `Product scope review checks` section (the examples `product-scope-roaster` tests) | `docs/features.md` and the backlog tables |
| `docs/frontend.md`, `docs/backend.md` | per side the project has: technologies, structure, routing or endpoints, data flow, running, building, testing and deploying, and the `Code review checks`, `Spec compliance checks` and `Dependency upgrade checks` sections (what `code-reviewer`, `spec-compliance-check` and `dependency-upgrade` read on that side) | the code of that side, its manifests and its build configuration |
| `docs/database.md` | tables, access rules, migration history, the database workflow, and a `Database review checks` section (what `database-reviewer` checks in this project) | the migrations and the database configuration, read in order |
| `docs/security.md` | the authentication flow, guards or filters, the session model, access control, and a `Security review checks` section (what `security-reviewer` checks in this project) | the authentication and access-control code and its configuration, and the dependency manifests |
| `docs/<topic>.md` | any other reference topic gets its own file | the code the topic describes |
| `CLAUDE.md` | the lean quick-start for an agent: commands, repo layout, short fixed-pointer subsections into `docs/`, and this section | its commands against the manifests and task files, its layout against the real top-level directories, its documentation paragraph against which `docs/*.md` exist |
| `README.md` | a short intro and the documentation index into `docs/` | which `docs/*.md` exist and what each covers |

The "Checked against" column says, in a phrase, what `docs-sync` compares each doc with. It is a pointer, not a list of paths: the doc itself names the code it describes, and `docs-sync` also searches the docs for the path, folder and names of every changed file. A project writes its own phrase where it differs (the kit's `docs/conventions.md` has a filled-in example).

The `docs` folder is the one that holds the backlog folder, and the project root is its parent: `docs/…`, `CLAUDE.md` and `README.md` in the table are relative to it. With the default `docs/backlog` that is the repository root; a repository whose product lives in a subfolder (`backlog_dir = "app/docs/backlog"`) keeps its `CLAUDE.md`, `README.md` and `docs/` in `app/`, and the repository root may carry its own, unrelated ones.

The decisions, explorations and reports folders sit next to the backlog folder: with `backlog_dir = "app/docs/backlog"` they are `app/docs/decisions`, `app/docs/explorations` and `app/docs/reports`.
