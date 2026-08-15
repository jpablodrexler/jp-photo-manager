package com.jpablodrexler.photomanager.domain.model;

import java.time.Instant;

public record SessionInfo(long id, String deviceHint, Instant lastUsedAt, boolean current) {
}
