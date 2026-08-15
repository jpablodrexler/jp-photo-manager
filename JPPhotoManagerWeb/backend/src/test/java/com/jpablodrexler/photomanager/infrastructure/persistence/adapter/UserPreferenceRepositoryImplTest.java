package com.jpablodrexler.photomanager.infrastructure.persistence.adapter;

import com.jpablodrexler.photomanager.domain.model.UserPreference;
import com.jpablodrexler.photomanager.infrastructure.persistence.entity.UserPreferenceEntity;
import com.jpablodrexler.photomanager.infrastructure.persistence.jpa.JpaUserPreferenceRepository;
import com.jpablodrexler.photomanager.infrastructure.persistence.mapper.UserPreferenceMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserPreferenceRepositoryImplTest {

    @Mock JpaUserPreferenceRepository jpa;
    @Mock UserPreferenceMapper mapper;
    @InjectMocks UserPreferenceRepositoryImpl sut;

    @Test
    void findByUserId_present_returnsMappedDomain() {
        UUID userId = UUID.randomUUID();
        UserPreferenceEntity entity = new UserPreferenceEntity();
        UserPreference domain = UserPreference.builder().userId(userId).themeMode("dark").build();
        when(jpa.findById(userId)).thenReturn(Optional.of(entity));
        when(mapper.toDomain(entity)).thenReturn(domain);

        assertThat(sut.findByUserId(userId)).contains(domain);
    }

    @Test
    void findByUserId_absent_returnsEmpty() {
        UUID userId = UUID.randomUUID();
        when(jpa.findById(userId)).thenReturn(Optional.empty());

        assertThat(sut.findByUserId(userId)).isEmpty();
    }

    @Test
    void save_mapsAndPersists() {
        UUID userId = UUID.randomUUID();
        UserPreference preference = UserPreference.builder().userId(userId).themeMode("light").build();
        UserPreferenceEntity entity = new UserPreferenceEntity();
        when(mapper.toEntity(preference)).thenReturn(entity);

        sut.save(preference);

        verify(jpa).save(entity);
    }
}
