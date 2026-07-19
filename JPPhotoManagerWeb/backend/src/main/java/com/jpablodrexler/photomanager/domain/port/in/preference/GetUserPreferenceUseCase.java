package com.jpablodrexler.photomanager.domain.port.in.preference;

import com.jpablodrexler.photomanager.domain.model.UserPreference;

import java.util.UUID;

public interface GetUserPreferenceUseCase {
    UserPreference execute(UUID userId);
}
