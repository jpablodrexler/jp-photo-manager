package com.jpablodrexler.photomanager.infrastructure.service;

import com.jpablodrexler.photomanager.domain.model.User;
import com.jpablodrexler.photomanager.domain.port.out.JwtTokenPort;
import com.jpablodrexler.photomanager.domain.port.out.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class UserServiceImpl {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenPort jwtUtil;

    @Transactional
    public void register(String username, String password) {
        String normalized = username.toLowerCase();
        if (userRepository.findByUsername(normalized).isPresent()) {
            throw new IllegalArgumentException("Username already taken: " + normalized);
        }
        User user = new User();
        user.setUsername(normalized);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setCreatedAt(Instant.now());
        user.setRole("USER");
        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public String authenticate(String username, String password) {
        String normalized = username.toLowerCase();
        User user = userRepository.findByUsername(normalized)
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid credentials");
        }
        return jwtUtil.generateToken(normalized);
    }
}
