package com.jpablodrexler.photomanager.application.usecase.preference;

import com.jpablodrexler.photomanager.domain.model.UserPreference;
import com.jpablodrexler.photomanager.domain.port.out.UserPreferenceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetUserPreferenceUseCaseImplTest {

    @Mock UserPreferenceRepository repository;
    @InjectMocks GetUserPreferenceUseCaseImpl sut;

    @Test
    void execute_preferenceFound_returnsStoredValue() {
        UUID userId = UUID.randomUUID();
        UserPreference stored = new UserPreference(userId, "light");
        when(repository.findByUserId(userId)).thenReturn(Optional.of(stored));

        UserPreference result = sut.execute(userId);

        assertThat(result.getThemeMode()).isEqualTo("light");
        assertThat(result.getUserId()).isEqualTo(userId);
    }

    @Test
    void execute_noPreference_returnsDefaultDark() {
        UUID userId = UUID.randomUUID();
        when(repository.findByUserId(userId)).thenReturn(Optional.empty());

        UserPreference result = sut.execute(userId);

        assertThat(result.getThemeMode()).isEqualTo("dark");
        assertThat(result.getUserId()).isEqualTo(userId);
    }
}
