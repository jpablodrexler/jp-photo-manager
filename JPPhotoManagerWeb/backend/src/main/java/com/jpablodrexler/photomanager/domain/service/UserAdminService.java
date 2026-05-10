package com.jpablodrexler.photomanager.domain.service;

import com.jpablodrexler.photomanager.application.dto.UserSummary;

import java.util.List;
import java.util.UUID;

public interface UserAdminService {

    List<UserSummary> listUsers();

    UserSummary createUser(String username, String password);

    void updatePassword(UUID id, String newPassword);

    void deleteUser(UUID id);
}
