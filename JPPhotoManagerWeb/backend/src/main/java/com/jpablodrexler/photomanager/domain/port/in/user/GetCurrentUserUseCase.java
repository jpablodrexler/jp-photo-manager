package com.jpablodrexler.photomanager.domain.port.in.user;

import com.jpablodrexler.photomanager.domain.model.User;

public interface GetCurrentUserUseCase {
    User execute();
}
