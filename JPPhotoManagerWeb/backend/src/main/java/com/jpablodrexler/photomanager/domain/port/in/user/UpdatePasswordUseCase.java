package com.jpablodrexler.photomanager.domain.port.in.user;

import java.util.UUID;

public interface UpdatePasswordUseCase {
    void execute(UUID userId, String newPassword);
}
