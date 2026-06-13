package com.jpablodrexler.photomanager.domain.port.in.preference;

import java.util.UUID;

public interface SaveUserPreferenceUseCase {
    void execute(UUID userId, String themeMode);
}
