## 1. Dependency

- [ ] 1.1 Add `org.passay:passay` to `pom.xml`

## 2. PasswordValidationService

- [ ] 2.1 Create `application/service/PasswordValidationService.java` annotated with `@Service`
- [ ] 2.2 Configure `PasswordValidator` with: `LengthRule(12, 128)`, `CharacterRule(EnglishCharacterData.UpperCase, 1)`, `CharacterRule(EnglishCharacterData.Digit, 1)`, `CharacterRule(EnglishCharacterData.Special, 1)`
- [ ] 2.3 Implement `void validate(String password)`: call `validator.validate(new PasswordData(password))`; if result is invalid, collect failure messages and throw `PasswordPolicyException(violations)`

## 3. PasswordPolicyException

- [ ] 3.1 Create `PasswordPolicyException extends ValidationException` with `List<String> violations` field
- [ ] 3.2 Update `GlobalExceptionHandler` 400 handler: if the exception is `PasswordPolicyException`, include `violations` array in the response body (extend `ErrorResponse` or use a subtype)

## 4. Integration with user creation and password change

- [ ] 4.1 Call `passwordValidationService.validate(password)` in the user creation use case before BCrypt hashing
- [ ] 4.2 Call `passwordValidationService.validate(newPassword)` in the password change use case

## 5. Backend unit tests

- [ ] 5.1 Test that `PasswordValidationService.validate("short1")` throws `PasswordPolicyException` with violations for length, uppercase, and special char
- [ ] 5.2 Test that `PasswordValidationService.validate("StrongP@ssw0rd!")` passes without exception
- [ ] 5.3 Test that `GlobalExceptionHandler` returns the `violations` array for `PasswordPolicyException`

## 6. Frontend — PasswordStrengthComponent

- [ ] 6.1 Create `shared/components/password-strength/password-strength.component.ts` as a standalone component
- [ ] 6.2 Declare `@Input() password = ''`; compute rule results on each change: `meetsLength`, `hasUppercase`, `hasDigit`, `hasSpecial` (TypeScript regex checks)
- [ ] 6.3 Compute `strengthLevel: 'weak' | 'medium' | 'strong'` based on number of passing rules (0-1 = weak, 2-3 = medium, 4 = strong)
- [ ] 6.4 Template: a colour bar (`background: red/yellow/green` based on `strengthLevel`) + a `@for` checklist of the four rules with checkmark/X icons
- [ ] 6.5 Use `PasswordStrengthComponent` in the user-admin creation form and the profile password-change form

## 7. Testing and Commit

- [ ] 7.1 Run backend test suite: `cd JPPhotoManagerWeb/backend && mvn test`
- [ ] 7.2 Run frontend test suite: `cd JPPhotoManagerWeb/frontend && npm test`
- [ ] 7.3 Commit all changes (only after both test suites pass)
