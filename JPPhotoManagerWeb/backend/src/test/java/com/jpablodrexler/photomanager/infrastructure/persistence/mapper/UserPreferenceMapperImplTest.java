package com.jpablodrexler.photomanager.infrastructure.persistence.mapper;

import com.jpablodrexler.photomanager.domain.model.UserPreference;
import com.jpablodrexler.photomanager.infrastructure.persistence.entity.UserPreferenceEntity;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class UserPreferenceMapperImplTest {

    private final UserPreferenceMapper sut = new UserPreferenceMapperImpl();

    @Test
    void toDomain_mapsFields() {
        UUID userId = UUID.randomUUID();
        UserPreferenceEntity entity = new UserPreferenceEntity();
        entity.setUserId(userId);
        entity.setThemeMode("dark");

        UserPreference result = sut.toDomain(entity);

        assertThat(result.getUserId()).isEqualTo(userId);
        assertThat(result.getThemeMode()).isEqualTo("dark");
    }

    @Test
    void toDomain_nullEntity_returnsNull() {
        assertThat(sut.toDomain(null)).isNull();
    }

    @Test
    void toEntity_mapsFields() {
        UUID userId = UUID.randomUUID();
        UserPreference domain = UserPreference.builder().userId(userId).themeMode("light").build();

        UserPreferenceEntity result = sut.toEntity(domain);

        assertThat(result.getUserId()).isEqualTo(userId);
        assertThat(result.getThemeMode()).isEqualTo("light");
    }

    @Test
    void toEntity_nullDomain_returnsNull() {
        assertThat(sut.toEntity(null)).isNull();
    }
}
