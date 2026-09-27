# Angular Developer — HttpOnly Cookie Authentication & Mobile Viewport Verification

_Part of the `angular-developer` skill — see `../SKILL.md` for the topic index. Load this file only when your current task touches this topic._

## 19. HttpOnly Cookie Authentication

When the backend uses HttpOnly cookie JWT (the correct pattern for browser apps), the Angular side stores only session metadata in `localStorage` — never the token itself. The cookie is invisible to JavaScript and sent automatically by the browser.

**AuthService — store metadata only:**

```typescript
const SESSION_KEY = 'photomanager_session';

interface LoginResponse { username: string; expiresAt: string; }
interface Session { username: string; expiresAt: number; }

login(username: string, password: string): Observable<void> {
  return this.http.post<LoginResponse>('/api/auth/login', { username, password }).pipe(
    tap(resp => {
      const session: Session = { username: resp.username, expiresAt: new Date(resp.expiresAt).getTime() };
      localStorage.setItem(SESSION_KEY, JSON.stringify(session));
    }),
    map(() => undefined)
  );
}

logout(): void {
  this.http.post('/api/auth/logout', {}).subscribe();   // clears the cookie server-side
  this.clearSession();
}

clearSession(): void {
  localStorage.removeItem(SESSION_KEY);
}

isLoggedIn(): boolean {
  const raw = localStorage.getItem(SESSION_KEY);
  if (!raw) return false;
  const session: Session = JSON.parse(raw);
  return session.expiresAt > Date.now();
}
```

**Auth interceptor — handle 401 only (no header injection):**

```typescript
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  return next(req).pipe(
    catchError(error => {
      if (error instanceof HttpErrorResponse && error.status === 401
          && !req.url.includes('/api/auth/login')) {
        inject(AuthService).clearSession();
        inject(Router).navigateByUrl('/login');
      }
      return throwError(() => error);
    })
  );
};
```

There is no `Authorization` header to add. The cookie is attached by the browser for all same-origin requests (HttpClient, `<img>`, `EventSource`) without any extra code.

**Dev proxy — no changes needed:** the Angular dev proxy (`proxy.conf.json`) forwards all cookies with `/api` requests to `localhost:8080` automatically, because all requests are same-origin from the browser's perspective.

---

## 20. Mobile Viewport Verification

Any change that touches a component's template or stylesheet (new UI, a
layout tweak, a row/card that gains or loses content) must be visually
checked at a phone-sized viewport before it's considered done. A
comfortably-wide handset preset (390–430px) is not enough on its own —
real layout bugs (overlapping text, a squeezed row colliding with a
sibling element) can hide at that width and only show up on a device
meaningfully narrower than it.

**Standard check device — Samsung Galaxy S23 Ultra:**

| | value |
| --- | --- |
| CSS viewport | 384 × 824 |
| Device pixel ratio | 3.75x |
| Physical resolution | 1440 × 3088 (~500ppi) |

Verify at this size (in addition to, not instead of, any other viewport
the change specifically calls for):

- **Chrome DevTools:** open the device toolbar (Ctrl/Cmd+Shift+M), and
  either pick "Galaxy S23 Ultra" from the device list if your Chrome
  version has it, or add a custom device with the dimensions above.
- **Cypress** (component tests or a scratch/E2E spec): `cy.viewport(384,
  824)` before asserting on layout.

What to actually look for at this width, not just that the page renders:

- A flex/grid row that packs several pieces (an icon, a name, secondary
  text, a count/badge, one or more action buttons) with no `flex-wrap` —
  check whether the squeezed text column wraps cleanly onto its own
  line(s) rather than colliding with a sibling element. `min-width: 0` on
  a flex child lets its text wrap internally, but does nothing to stop it
  visually overlapping a fixed-width sibling if the row itself doesn't
  wrap or restructure.
- A non-wrapping action-button group forcing horizontal scroll.
- A wrapping action-button group whose wrapped buttons have no shared
  width rule — each one sizes to its own icon+label content, so a short
  label ends up visibly narrower than a long one once each occupies its
  own line, producing a ragged column. Give wrapped buttons a uniform
  width — stacking each one full-width is the simplest correct fix.
- Any container given a fixed pixel width rather than a relative one.

This check is part of finishing the change, the same way running the
component test for new code is — not a separate, optional pass.

---

