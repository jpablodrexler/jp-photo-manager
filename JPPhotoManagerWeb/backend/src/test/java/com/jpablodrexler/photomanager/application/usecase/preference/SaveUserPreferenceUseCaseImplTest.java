package com.jpablodrexler.photomanager.application.usecase.preference;

import com.jpablodrexler.photomanager.domain.model.UserPreference;
import com.jpablodrexler.photomanager.domain.port.out.UserPreferenceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SaveUserPreferenceUseCaseImplTest {

    @Mock UserPreferenceRepository repository;
    @InjectMocks SaveUserPreferenceUseCaseImpl sut;

    @Test
    void execute_validInput_delegatesToRepositoryWithCorrectValues() {
        UUID userId = UUID.randomUUID();

        sut.execute(userId, "light");

        ArgumentCaptor<UserPreference> captor = ArgumentCaptor.forClass(UserPreference.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(userId);
        assertThat(captor.getValue().getThemeMode()).isEqualTo("light");
    }
}
