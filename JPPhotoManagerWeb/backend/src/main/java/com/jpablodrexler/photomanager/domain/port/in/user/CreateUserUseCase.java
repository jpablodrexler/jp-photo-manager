package com.jpablodrexler.photomanager.domain.port.in.user;

import com.jpablodrexler.photomanager.application.dto.UserSummary;

public interface CreateUserUseCase {
    UserSummary execute(String username, String password, String role);
}
