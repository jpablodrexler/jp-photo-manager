# Definition of Done

Every feature meets all of these before it is archived. A brief's own `## Definition of Done` section lists only what is extra for that feature.

- [ ] Every acceptance criterion in the brief is met and has evidence: a passing test, or a manual check recorded in the spec-compliance report.
- [ ] Backend changes have unit tests (`mvn test`) and, where they touch persistence or messaging, integration tests (`mvn verify -Pintegration-tests`); line coverage stays at or above 80% (`mvn jacoco:check`).
- [ ] Frontend changes have Cypress component tests and the suite passes; line coverage stays at or above 80% (`npm run coverage:check`).
- [ ] The code review has no open 🔴/🟡 findings.
- [ ] A new or changed Flyway migration has a database review with no open findings and applies cleanly on an empty database.
- [ ] Security-sensitive changes (authentication, authorization, user input, dependencies) have a security review with no open findings.
- [ ] A user-facing flow was verified end to end (the mocked E2E tier at least; the real-backend tier when the flow depends on the API).
- [ ] Both builds succeed (`mvn clean package` and the frontend production build).
- [ ] A change to a REST endpoint or a configuration property is reflected in `docs/backend.md` and `docs/curl-reference.md`; the other reference docs (`features`, `frontend`, `database`, `security`, `architecture`) are in sync with the code (`docs-sync`).
- [ ] A non-obvious architectural decision is recorded as an ADR in `docs/decisions/`.
- [ ] The feature is moved to `features-implemented.md` with its brief and archived spec linked.
