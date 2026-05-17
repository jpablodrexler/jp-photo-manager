package com.jpablodrexler.photomanager.domain.port.in.user;

import com.jpablodrexler.photomanager.application.dto.UserSummary;
import java.util.List;

public interface ListUsersUseCase {
    List<UserSummary> execute();
}
