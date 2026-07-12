package com.jpablodrexler.photomanager.application.usecase.user;

import com.jpablodrexler.photomanager.domain.port.in.user.DeleteUserUseCase;
import com.jpablodrexler.photomanager.domain.port.out.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DeleteUserUseCaseImpl implements DeleteUserUseCase {

    private final UserRepository userRepository;

    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void execute(UUID userId) {
        if (userRepository.findById(userId).isEmpty()) {
            throw new NoSuchElementException("User not found: " + userId);
        }
        userRepository.deleteById(userId);
    }
}
