// Shared session-seeding helper for the mocked E2E smoke tier
// (cypress/e2e/mocked/**) - every spec in this tier needs the same
// localStorage-based auth bypass gallery.cy.ts established first. Centralized
// here so the 'photomanager_session' key/shape (and the 'role' field the
// guard's own AuthService.isAdmin() reads, which the tier's original
// gallery.cy.ts fixture omitted since it never exercised admin-gated UI)
// stays in one place instead of being copy-pasted into every spec file.
const SESSION_KEY = 'photomanager_session';

export function visitWithSession(path: string, role: 'ADMIN' | 'USER' = 'ADMIN'): void {
  cy.visit(path, {
    onBeforeLoad(win) {
      win.localStorage.setItem(
        SESSION_KEY,
        JSON.stringify({ username: 'admin', expiresAt: Date.now() + 3_600_000, role }),
      );
    },
  });
}
