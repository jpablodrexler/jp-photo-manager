package com.jpablodrexler.photomanager.config;

import com.jpablodrexler.photomanager.domain.model.User;
import com.jpablodrexler.photomanager.domain.port.out.UserAuthPort;
import com.jpablodrexler.photomanager.domain.port.out.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DataInitializerTest {

    @Mock UserRepository userRepository;
    @Mock UserAuthPort userAuthPort;
    @InjectMocks DataInitializer sut;

    @Test
    void seedDefaultAdmin_noExistingUsers_registersAndPromotesAdmin() {
        when(userRepository.count()).thenReturn(0L);
        User admin = new User();
        admin.setUsername("admin");
        admin.setRole("USER");
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));
        when(userRepository.save(admin)).thenReturn(admin);

        sut.seedDefaultAdmin();

        verify(userAuthPort).register("admin", "admin");
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getRole()).isEqualTo("ADMIN");
    }

    @Test
    void seedDefaultAdmin_existingUsers_doesNotRegisterAdmin() {
        when(userRepository.count()).thenReturn(1L);

        sut.seedDefaultAdmin();

        verify(userAuthPort, never()).register(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString());
        verify(userRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void seedDefaultAdmin_registeredButNotFound_doesNotSave() {
        when(userRepository.count()).thenReturn(0L);
        when(userRepository.findByUsername("admin")).thenReturn(Optional.empty());

        sut.seedDefaultAdmin();

        verify(userAuthPort).register("admin", "admin");
        verify(userRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }
}
