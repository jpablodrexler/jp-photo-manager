package com.jpablodrexler.photomanager.infrastructure.service;

import com.jpablodrexler.photomanager.application.dto.UserSummary;
import com.jpablodrexler.photomanager.domain.repository.UserRepository;
import com.jpablodrexler.photomanager.domain.service.UserAdminService;
import com.jpablodrexler.photomanager.domain.service.UserService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserAdminServiceImpl implements UserAdminService {

    private final UserRepository userRepository;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(readOnly = true)
    public List<UserSummary> listUsers() {
        return userRepository.findAll().stream()
                .map(u -> new UserSummary(u.getId(), u.getUsername(), u.getCreatedAt()))
                .toList();
    }

    @Override
    @Transactional
    public UserSummary createUser(String username, String password) {
        userService.register(username, password);
        return userRepository.findByUsername(username.toLowerCase())
                .map(u -> new UserSummary(u.getId(), u.getUsername(), u.getCreatedAt()))
                .orElseThrow();
    }

    @Override
    @Transactional
    public void updatePassword(UUID id, String newPassword) {
        var user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + id));
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void deleteUser(UUID id) {
        if (!userRepository.existsById(id)) {
            throw new EntityNotFoundException("User not found: " + id);
        }
        userRepository.deleteById(id);
    }
}
