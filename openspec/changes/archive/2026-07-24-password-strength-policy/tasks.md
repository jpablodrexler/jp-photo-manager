## 1. Dependency

- [x] 1.1 Add `org.passay:passay` to `pom.xml`

## 2. PasswordValidationService

- [x] 2.1 Create `application/service/PasswordValidationService.java` annotated with `@Service`
- [x] 2.2 Configure `PasswordValidator` with: `LengthRule(12, 128)`, `CharacterRule(EnglishCharacterData.UpperCase, 1)`, `CharacterRule(EnglishCharacterData.Digit, 1)`, `CharacterRule(EnglishCharacterData.Special, 1)`
- [x] 2.3 Implement `void validate(String password)`: call `validator.validate(new PasswordData(password))`; if result is invalid, collect failure messages and throw `PasswordPolicyException(violations)`

## 3. PasswordPolicyException

- [x] 3.1 Create `PasswordPolicyException extends ValidationException` with `List<String> violations` field — no `ValidationException` base class exists in this codebase (same gap already documented in `GlobalExceptionHandlerTest`), so `PasswordPolicyException` extends `RuntimeException` directly, matching every other exception in `application/exception/`
- [x] 3.2 Update `GlobalExceptionHandler` 400 handler: if the exception is `PasswordPolicyException`, include `violations` array in the response body (extend `ErrorResponse` or use a subtype) — added a dedicated `PasswordPolicyErrorResponseDto` and `@ExceptionHandler(PasswordPolicyException.class)`

## 4. Integration with user creation and password change

- [x] 4.1 Call `passwordValidationService.validate(password)` in the user creation use case before BCrypt hashing — `CreateUserUseCaseImpl.execute`
- [x] 4.2 Call `passwordValidationService.validate(newPassword)` in the password change use case — `UpdatePasswordUseCaseImpl.execute` (this codebase's only password-change use case is the admin `UpdatePasswordUseCase`; there is no separate self-service "profile" password-change endpoint)

## 5. Backend unit tests

- [x] 5.1 Test that `PasswordValidationService.validate("short1")` throws `PasswordPolicyException` with violations for length, uppercase, and special char
- [x] 5.2 Test that `PasswordValidationService.validate("StrongP@ssw0rd!")` passes without exception
- [x] 5.3 Test that `GlobalExceptionHandler` returns the `violations` array for `PasswordPolicyException`

## 6. Frontend — PasswordStrengthComponent

- [x] 6.1 Create `shared/components/password-strength/password-strength.component.ts` as a standalone component
- [x] 6.2 Declare `@Input() password = ''`; compute rule results on each change: `meetsLength`, `hasUppercase`, `hasDigit`, `hasSpecial` (TypeScript regex checks)
- [x] 6.3 Compute `strengthLevel: 'weak' | 'medium' | 'strong'` based on number of passing rules (0-1 = weak, 2-3 = medium, 4 = strong)
- [x] 6.4 Template: a colour bar (`background: red/yellow/green` based on `strengthLevel`) + a `@for` checklist of the four rules with checkmark/X icons
- [x] 6.5 Use `PasswordStrengthComponent` in the user-admin creation form and the profile password-change form — this codebase has no separate self-service "profile" password form; both password fields (create-user and change-password) live in `UserAdminComponent`, so `PasswordStrengthComponent` is wired into both there

## 7. Testing and Commit

- [x] 7.1 Run backend test suite: `cd JPPhotoManagerWeb/backend && mvn test` — 683 tests, 0 failures, 0 errors, 2 skipped (pre-existing)
- [x] 7.2 Run frontend test suite: `cd JPPhotoManagerWeb/frontend && npm test` — 366 tests, 0 failures
- [ ] 7.3 Commit all changes (only after both test suites pass) — intentionally left undone: this run's operating instructions forbid `git commit`/`git push` at any point
