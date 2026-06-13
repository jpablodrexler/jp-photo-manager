package com.jpablodrexler.photomanager.domain.port.out;

import com.jpablodrexler.photomanager.domain.model.UserPreference;

import java.util.Optional;
import java.util.UUID;

public interface UserPreferenceRepository {
    Optional<UserPreference> findByUserId(UUID userId);
    void save(UserPreference preference);
}
