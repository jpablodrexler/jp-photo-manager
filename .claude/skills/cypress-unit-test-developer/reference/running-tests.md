# Cypress Unit Test Developer — Running Tests

_Part of the `cypress-unit-test-developer` skill — see `../SKILL.md` for the topic index. Load this file only when your current task touches this topic._

## 15. Running Tests

```bash
# Open Cypress Test Runner (interactive)
npm run cypress:open

# Run headlessly (CI)
npm run cypress:run

# Run a single spec
npx cypress run --component --spec "src/app/features/gallery/gallery.component.cy.ts"

# Check line coverage against the 80% minimum (see code-reviewer skill §19.1)
npm run test:coverage
npm run coverage:check
```

---

