# Cypress Unit Test Developer — DOM Interaction Patterns & Assertions Reference

_Part of the `cypress-unit-test-developer` skill — see `../SKILL.md` for the topic index. Load this file only when your current task touches this topic._

## 10. DOM Interaction Patterns

| Action                           | Cypress code                                                                       |
| -------------------------------- | ---------------------------------------------------------------------------------- |
| Click a button                   | `cy.get('button[data-cy="next-page"]').click()`                                    |
| Type into an input               | `cy.get('input').type('search term')`                                              |
| Select a mat-select option       | `cy.get('mat-select').click(); cy.get('mat-option').contains('File Size').click()` |
| Assert text content              | `cy.contains('.thumbnail-name', 'photo.jpg')`                                      |
| Assert element count             | `cy.get('app-thumbnail').should('have.length', 3)`                                 |
| Assert class present             | `cy.get('mat-card').should('have.class', 'selected')`                              |
| Assert class absent              | `cy.get('mat-card').should('not.have.class', 'selected')`                          |
| Assert disabled state            | `cy.get('button').should('be.disabled')`                                           |
| Assert element hidden            | `cy.get('.spinner').should('not.exist')`                                           |
| Trigger Angular change detection | `cy.then(({ fixture }) => fixture.detectChanges())`                                |

Add `data-cy` attributes to templates only when a stable query selector is needed
and there is no other semantic selector available:

```html
<button mat-icon-button data-cy="prev-page" (click)="prevPage()"></button>
```

---

## 11. Assertions Reference

Always use Cypress's Chai-based assertions, **not** Jasmine matchers:

| Assertion        | Cypress (Chai)                                      |
| ---------------- | --------------------------------------------------- |
| Equality         | `expect(val).to.equal('text')`                      |
| Deep equality    | `expect(obj).to.deep.equal({ a: 1 })`               |
| Truthiness       | `expect(val).to.be.true`                            |
| Existence        | `expect(val).to.exist`                              |
| Array length     | `expect(arr).to.have.length(3)`                     |
| Include          | `expect(str).to.include('substring')`               |
| Stub called      | `cy.wrap(stub).should('have.been.called')`          |
| Stub called with | `cy.wrap(stub).should('have.been.calledWith', arg)` |
| Stub call count  | `cy.wrap(stub).should('have.been.calledOnce')`      |

**Testing that a `Promise`-returning method rejects**: never chain
`.then(onFulfilled, onRejected)` directly on `cy.wrap(aRejectingPromise)`
— `cy.wrap()` fails the test the instant the wrapped promise rejects,
before the second callback ever runs. Pre-resolve the rejection into a
plain value first, then wrap *that*:

```typescript
function rejectionOf<T>(promise: Promise<T>): Promise<unknown> {
  return promise.then(
    () => {
      throw new Error("expected the promise to reject, but it resolved");
    },
    (err) => err,
  );
}

cy.wrap(rejectionOf(someMethodThatReturnsAPromise())).should(
  "deep.equal",
  expectedError,
);
```

Most of this app's own service methods return `Observable`s (via
`HttpClient`), not `Promise`s — assert an `Observable`'s error path the
normal way instead, via `req.flush(errorBody, { status, statusText })` and
an `error` callback on `.subscribe(...)`. This pattern only matters for
the minority of methods that genuinely return a `Promise` (e.g. a plain
async utility function, or an `EventSource`-adjacent helper).

---

