package com.jpablodrexler.photomanager.application.usecase.preference;

import com.jpablodrexler.photomanager.domain.model.UserPreference;
import com.jpablodrexler.photomanager.domain.port.in.preference.SaveUserPreferenceUseCase;
import com.jpablodrexler.photomanager.domain.port.out.UserPreferenceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SaveUserPreferenceUseCaseImpl implements SaveUserPreferenceUseCase {

    private final UserPreferenceRepository repository;

    @Override
    @Transactional
    public void execute(UUID userId, String themeMode) {
        repository.save(new UserPreference(userId, themeMode));
    }
}
