// Shared helpers for the release-e2e-suite specs under cypress/e2e/release/.
//
// Unlike cypress/e2e/gallery.cy.ts (which mocks every backend call via
// cy.intercept and seeds localStorage directly), every spec here drives a
// real login form against a real backend — no interception, no session
// seeding. These are meant to run against the live Kubernetes deployment
// (see the e2e-testing and release-e2e-suite skills), not the mocked dev
// fixtures the rest of cypress/e2e/ uses.

// Real admin credentials for the live deployment under test — never
// hardcoded here. Supply them via cypress.env.json (gitignored — see
// .gitignore) or `--env E2E_ADMIN_USERNAME=...,E2E_ADMIN_PASSWORD=...`.
// The seeded DataInitializer default (admin/admin) is intentionally NOT
// used as a fallback: a real deployment's admin password is expected to
// have been changed already (DataInitializer logs exactly that warning on
// first boot), so silently falling back to it would either fail loudly
// against a real deployment anyway or, worse, mask a deployment that was
// never actually hardened.
export const ADMIN_USERNAME: string = Cypress.env('E2E_ADMIN_USERNAME');
export const ADMIN_PASSWORD: string = Cypress.env('E2E_ADMIN_PASSWORD');

if (!ADMIN_USERNAME || !ADMIN_PASSWORD) {
  throw new Error(
    'E2E_ADMIN_USERNAME / E2E_ADMIN_PASSWORD are not set. Provide them via ' +
      'JPPhotoManagerWeb/frontend/cypress.env.json (gitignored) or --env — see ' +
      'the release-e2e-suite skill.',
  );
}

// The dedicated, emptyDir-backed catalog root reserved for this suite's
// generated test images (see k8s/backend.yaml / k8s/configmap.yaml) — never
// point these specs at /catalog, /catalog2, or /catalog3, which are real,
// machine-specific personal directories.
export const E2E_CATALOG_ROOT = '/e2e-catalog';
export const TRIP_FOLDER = `${E2E_CATALOG_ROOT}/trip`;
export const EVENTS_FOLDER = `${E2E_CATALOG_ROOT}/events`;
export const SYNCED_FOLDER = `${E2E_CATALOG_ROOT}/synced`;
export const CONVERTED_FOLDER = `${E2E_CATALOG_ROOT}/converted`;

export function realLogin(username = ADMIN_USERNAME, password = ADMIN_PASSWORD): void {
  cy.visit('/login');
  cy.get('input[formControlName="username"]').type(username);
  cy.get('input[formControlName="password"]').type(password);
  cy.get('button[type="submit"]').click();
  cy.url().should('not.include', '/login');
}

export function logout(): void {
  cy.get('button').contains('Logout').click();
  cy.url().should('include', '/login');
}

// Navigates to a folder inside the gallery via the `folder` query param —
// this is how a real deep link (e.g. clicking a Home dashboard "recent
// photo" tile) opens the gallery pre-scoped to a folder, and critically it
// works even for a folder that has never been catalogued yet (the component
// just assigns the string to currentFolder, no tree lookup — see
// GalleryComponent.ngOnInit). That's what makes it possible to open the
// upload drop-zone for a brand-new subfolder under /e2e-catalog before any
// catalog run has ever indexed it.
export function visitGalleryFolder(folderPath: string): void {
  cy.visit(`/gallery?folder=${encodeURIComponent(folderPath)}`);
  cy.get('.current-folder').should('contain.text', folderPath);
}

// Uploads fixture files (paths relative to cypress/fixtures) into the given
// folder via the real drop-zone file input and DropZoneComponent's actual
// upload pipeline (multipart POST to /api/assets/upload, then an SSE
// subscription per file until it reaches 'done'/'failed') — waits for every
// upload to reach a visible terminal status in the UI.
//
// `folderPath` must already exist as a real Folder row — UploadAssetUseCaseImpl
// looks it up with `folderRepository.findByPath(...).orElseThrow(...)` and
// never creates one (confirmed while building this suite: uploading into a
// brand-new, never-cataloged subfolder like /e2e-catalog/trip fails with a
// 404 "Folder not found in catalog", even though the drop-zone itself
// happily renders for any currentFolder string — see GalleryComponent's
// `folder` query-param handling in visitGalleryFolder below, which sets
// currentFolder with no existence check at all). E2E_CATALOG_ROOT itself is
// safe to upload into directly since it's the configured catalog root and
// already has a Folder row; see moveAssetsToFolder for how this suite gets
// files into real *subfolders* despite that constraint.
export function uploadFixturesToFolder(folderPath: string, fixturePaths: string[]): void {
  visitGalleryFolder(folderPath);
  cy.get('input[type="file"]').selectFile(
    fixturePaths.map((p) => `cypress/fixtures/${p}`),
    { force: true },
  );
  // Every queued item must leave the "uploading"/"processing" state — either
  // done or (surfacing a real failure) error — before the caller proceeds.
  // Per-file async processing (hash/EXIF/thumbnail via Kafka — see
  // kafka-events-conventions) is real, Kafka-consumer-driven work, not an
  // instant local computation; a generous timeout avoids flaking under
  // whatever backlog a busy broker has built up from repeated suite runs.
  cy.get('.upload-item', { timeout: 30000 }).should('have.length', fixturePaths.length);
  fixturePaths.forEach((_, i) => {
    cy.get('.upload-item').eq(i).find('.status-icon', { timeout: 60000 }).should('exist');
  });
  cy.get('.upload-item .status-error').should('not.exist');
}

// Moves already-cataloged assets (matched by file name, within
// E2E_CATALOG_ROOT) into a destination subfolder that may not exist yet —
// via a real POST /api/assets/move call, which (unlike Upload) *does* create
// the destination Folder row on the fly (MoveAssetsUseCaseImpl.execute →
// FolderRepository.findOrCreateByPath — see java-developer §6.8). Goes
// through cy.request() rather than the gallery's "Move to folder" dialog
// because that dialog's folder picker only lets you select an *existing*
// tree node (FolderPickerDialogComponent embeds the real folder-nav tree
// with no free-text path input) — a real UI limitation, not a test
// shortcut, and the only way to reach a real, already-fixed backend code
// path (this exact atomic find-or-create was this session's earlier
// gallery-folder-duplication fix) that the UI simply doesn't expose a click
// path to. This is still exercising real, production Move logic — just
// invoked directly rather than through the one dialog that can't drive it.
export function moveAssetsToFolder(fileNames: string[], destinationFolderPath: string): void {
  cy.request(
    'GET',
    `/api/assets?folderPath=${encodeURIComponent(E2E_CATALOG_ROOT)}&page=0&sort=FILE_NAME`,
  ).then((response) => {
    const items = response.body.items as Array<{ assetId: number; fileName: string }>;
    const assetIds = fileNames.map((name) => {
      const match = items.find((item) => item.fileName === name);
      if (!match) {
        throw new Error(`moveAssetsToFolder: no asset named "${name}" found in ${E2E_CATALOG_ROOT}`);
      }
      return match.assetId;
    });
    cy.request('POST', '/api/assets/move', {
      assetIds,
      destinationFolderPath,
      preserveOriginal: false,
    }).its('status').should('eq', 200);
  });
}
