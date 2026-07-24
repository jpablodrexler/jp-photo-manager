package com.jpablodrexler.photomanager.infrastructure.web.dto.response;

import java.util.List;

/**
 * Error response body for a {@code PasswordPolicyException}: the standard error fields plus a
 * per-rule {@code violations} list so the frontend can highlight exactly which rules failed.
 */
public record PasswordPolicyErrorResponseDto(String timestamp, int status, String error, String message,
                                               List<String> violations) {
}
