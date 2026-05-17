package com.jpablodrexler.photomanager.domain.port.in.user;

import java.util.UUID;

public interface DeleteUserUseCase {
    void execute(UUID userId);
}
