package com.jpablodrexler.photomanager.application.exception;

import lombok.Getter;

import java.util.List;

/**
 * Thrown when a password fails the application's minimum complexity policy.
 *
 * <p>The design references an "existing {@code ValidationException}" base class, but no such
 * class exists in this codebase (the same gap noted in {@code GlobalExceptionHandlerTest});
 * this extends {@link RuntimeException} directly, matching the pattern used by every other
 * exception in this package (e.g. {@link UnsupportedAssetTypeException}).
 */
@Getter
public class PasswordPolicyException extends RuntimeException {

    private final List<String> violations;

    public PasswordPolicyException(List<String> violations) {
        super("Password does not meet requirements");
        this.violations = violations;
    }
}
