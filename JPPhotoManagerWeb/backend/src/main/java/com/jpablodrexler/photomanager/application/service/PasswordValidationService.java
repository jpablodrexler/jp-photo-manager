package com.jpablodrexler.photomanager.application.service;

import com.jpablodrexler.photomanager.application.exception.PasswordPolicyException;
import org.passay.CharacterRule;
import org.passay.EnglishCharacterData;
import org.passay.LengthRule;
import org.passay.PasswordData;
import org.passay.PasswordValidator;
import org.passay.RuleResult;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Enforces the application's minimum password complexity policy using Passay: at least 12
 * characters, one uppercase letter, one digit, and one special character. The rules are
 * hardcoded (not user-configurable) — password policy is a security decision, not an
 * end-user preference.
 */
@Service
public class PasswordValidationService {

    private final PasswordValidator validator = new PasswordValidator(List.of(
            new LengthRule(12, 128),
            new CharacterRule(EnglishCharacterData.UpperCase, 1),
            new CharacterRule(EnglishCharacterData.Digit, 1),
            new CharacterRule(EnglishCharacterData.Special, 1)
    ));

    /**
     * Validates the given password against the policy.
     *
     * @throws PasswordPolicyException if the password fails one or more rules; the exception
     *                                  carries a human-readable message per failing rule.
     */
    public void validate(String password) {
        RuleResult result = validator.validate(new PasswordData(password));
        if (!result.isValid()) {
            List<String> violations = validator.getMessages(result);
            throw new PasswordPolicyException(violations);
        }
    }
}
