package com.jpablodrexler.photomanager.application.usecase.user;

import com.jpablodrexler.photomanager.domain.model.UserSummary;
import com.jpablodrexler.photomanager.domain.model.User;
import com.jpablodrexler.photomanager.domain.port.in.user.CreateUserUseCase;
import com.jpablodrexler.photomanager.domain.port.out.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class CreateUserUseCaseImpl implements CreateUserUseCase {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public UserSummary execute(String username, String password, String role) {
        String normalized = username.toLowerCase();
        if (userRepository.findByUsername(normalized).isPresent()) {
            throw new IllegalArgumentException("Username already taken: " + normalized);
        }
        User user = new User();
        user.setUsername(normalized);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setCreatedAt(Instant.now());
        if (role != null) {
            user.setRole(role);
        }
        User saved = userRepository.save(user);
        return new UserSummary(saved.getId(), saved.getUsername(), saved.getCreatedAt());
    }
}
