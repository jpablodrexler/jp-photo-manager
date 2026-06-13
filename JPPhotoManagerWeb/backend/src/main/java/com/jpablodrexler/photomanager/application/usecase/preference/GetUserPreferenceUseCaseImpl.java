package com.jpablodrexler.photomanager.application.usecase.preference;

import com.jpablodrexler.photomanager.domain.model.UserPreference;
import com.jpablodrexler.photomanager.domain.port.in.preference.GetUserPreferenceUseCase;
import com.jpablodrexler.photomanager.domain.port.out.UserPreferenceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetUserPreferenceUseCaseImpl implements GetUserPreferenceUseCase {

    private final UserPreferenceRepository repository;

    @Override
    @Transactional(readOnly = true)
    public UserPreference execute(UUID userId) {
        return repository.findByUserId(userId)
                .orElse(UserPreference.builder()
                        .userId(userId)
                        .themeMode("dark")
                        .build());
    }
}
