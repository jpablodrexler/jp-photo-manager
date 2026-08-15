package com.jpablodrexler.photomanager.application.service;

import com.jpablodrexler.photomanager.application.exception.PasswordPolicyException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

class PasswordValidationServiceTest {

    private final PasswordValidationService sut = new PasswordValidationService();

    @Test
    void validate_shortWeakPassword_throwsPasswordPolicyExceptionWithViolationsForEachFailingRule() {
        PasswordPolicyException thrown = catchThrowableOfType(() -> sut.validate("short1"), PasswordPolicyException.class);

        assertThat(thrown).isNotNull();
        assertThat(thrown.getViolations()).hasSizeGreaterThanOrEqualTo(3);
        assertThat(thrown.getViolations()).anyMatch(v -> v.toLowerCase().contains("12"));
        assertThat(thrown.getViolations()).anyMatch(v -> v.toLowerCase().contains("uppercase"));
        assertThat(thrown.getViolations()).anyMatch(v -> v.toLowerCase().contains("special"));
    }

    @Test
    void validate_strongPassword_doesNotThrow() {
        assertThatCode(() -> sut.validate("StrongP@ssw0rd!"))
                .doesNotThrowAnyException();
    }
}
