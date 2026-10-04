---
name: angular-developer
description: >
  Angular developer skill for writing Angular 22 applications following the
  JPPhotoManager frontend code style. TRIGGER whenever work touches
  JPPhotoManagerWeb/frontend — including when implementing OpenSpec tasks:
  creating or modifying components, services, models, pipes, or routes in an
  Angular project with a feature-based architecture of
  core → features ← shared. Invoke this skill proactively — do not wait to
  be asked.
metadata:
  scope: [JPPhotoManagerWeb/frontend]
---

# Angular Developer Skill

Write Angular code that follows the conventions and best practices of the
JPPhotoManager frontend project: an Angular 22 / TypeScript 6 application with
standalone components, Angular Material, RxJS, and a feature-based clean
architecture.

## Workflow

Make a todo list and work through it one task at a time.

The detailed rules for each topic live under `reference/`, split so you only
load what today's task actually touches instead of every convention in the
project every time this skill fires. Read the row(s) below matching your
todo items — a component/template task doesn't need the cookie-auth rules,
a service-only change doesn't need viewport verification, etc. — and skip
the rest. When a task spans several topics (the common case for a full
feature), read each relevant file as you reach that part of the work, not
all of them upfront.

| Topic | Read when you're touching… | File |
| ----- | --------------------------- | ---- |
| §1-2 Project setup & TypeScript config | build config, `tsconfig*.json`, the dev proxy | `reference/project-setup-and-types.md` |
| §3-4 Directory structure & naming | deciding where a new file goes, or what to name it | `reference/structure-and-naming.md` |
| §5-6 Standalone components & routing | a new/changed component or route | `reference/components-and-routing.md` |
| §7-8 Services & models | a `core/*.service.ts` or a data model interface | `reference/services-and-models.md` |
| §9-10 Templates & component state (RxJS) | `.html` template syntax, or component-local state/subscriptions | `reference/templates-and-state.md` |
| §11-12 Server-Sent Events & Angular Material usage | an `EventSource` stream, or any Material module/component | `reference/sse-and-material-usage.md` |
| §13-14 CDK Tree (folder navigation) & SCSS/styling | the folder tree component, or any stylesheet | `reference/cdk-tree-and-styling.md` |
| §15-16 Custom pipes & testing (Cypress Component Testing) | a new pipe, or writing/editing a `*.cy.ts` file | `reference/pipes-and-testing.md` |
| §17-18 Code style rules & the multi-step feature pattern | general style questions, or a wizard-style multi-step flow | `reference/code-style-and-multi-step-pattern.md` |
| §19-20 HttpOnly cookie auth & mobile viewport verification | anything reading/writing auth state, or any template/CSS change | `reference/cookie-auth-and-viewport.md` |

---

## Wrap Up

After implementing the requested feature, provide a brief summary covering:

1. Files created or modified and their purpose
2. Any new routes added to `app.routes.ts`
3. Any new Material modules imported
4. How to run the tests for the new code:
   ```bash
   npx cypress run --component --spec 'src/app/<path>/your-new.component.cy.ts'
   ```
5. Any environment or proxy configuration the user should set
6. For any UI/template/CSS change, confirmation that
   `reference/cookie-auth-and-viewport.md`'s §20 mobile viewport check
   (Samsung S23 Ultra, 384×824) was done and what — if anything — it
   caught
