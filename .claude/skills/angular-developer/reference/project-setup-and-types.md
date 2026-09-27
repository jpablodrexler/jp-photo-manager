# Angular Developer — Project Setup & TypeScript Configuration

_Part of the `angular-developer` skill — see `../SKILL.md` for the topic index. Load this file only when your current task touches this topic._

## 1. Project Setup

### Build System

Use the **Angular CLI** (`@angular/cli`) with this core stack:

| Package             | Version   | Purpose                                 |
| ------------------- | --------- | --------------------------------------- |
| `@angular/core`     | `^19.0.0` | Framework core                          |
| `@angular/material` | `^19.0.0` | Material Design UI components           |
| `@angular/cdk`      | `^19.0.0` | Component Dev Kit (tree, overlay, etc.) |
| `@angular/forms`    | `^19.0.0` | Reactive & template-driven forms        |
| `@angular/router`   | `^19.0.0` | Client-side routing                     |
| `rxjs`              | `~7.8.0`  | Reactive programming                    |
| `typescript`        | `~5.6.0`  | Language                                |

**Dev dependencies:**

| Package                        | Purpose                                                            |
| ------------------------------- | -------------------------------------------------------------------- |
| `cypress`                       | Test runner — both component tests (§16) and the E2E suite          |
| `@angular-devkit/build-angular` | Only for Cypress's webpack-based Angular Component Testing preset — the app itself never uses it for building/serving (see §16) |

There is no Karma/Jasmine in this project — Cypress is the sole test
runner, for both the component/unit layer and the E2E layer. See §16
below and the `cypress-unit-test-developer`/`e2e-suite` skills.

**Key npm scripts:**

```bash
ng serve                         # Dev server (http://localhost:4200)
ng build                         # Development build
ng build --configuration production  # Production build
npm run test                     # cypress run --component — the unit/component test suite
npm run test:e2e                 # cypress run --e2e — the maintained E2E suite (backend must already be running)
ng lint                          # Lint the project
```

To run a single test file: `npx cypress run --component --spec
'src/app/features/gallery/gallery.component.cy.ts'`.

### Dev Proxy

Configure the dev proxy in `proxy.conf.json` to forward API calls to the backend:

```json
{
  "/api": {
    "target": "http://localhost:8080",
    "secure": false,
    "changeOrigin": true
  }
}
```

---

## 2. TypeScript Configuration

**tsconfig.json (base — never relax these):**

```jsonc
{
  "compilerOptions": {
    "target": "ES2022",
    "module": "ES2022",
    "strict": true,
    "noImplicitOverride": true,
    "noPropertyAccessFromIndexSignature": true,
    "noImplicitReturns": true,
    "noFallthroughCasesInSwitch": true,
    "forceConsistentCasingInFileNames": true,
    "moduleResolution": "bundler",
    "baseUrl": "./",
  },
  "angularCompilerOptions": {
    "strictInjectionParameters": true,
    "strictInputAccessModifiers": true,
    "strictTemplates": true,
  },
}
```

All strict flags **must remain enabled**. Never use `any`; prefer typed interfaces or `unknown`.

---

