package com.jpablodrexler.photomanager.infrastructure.persistence.mapper;

import com.jpablodrexler.photomanager.domain.model.SearchPreset;
import com.jpablodrexler.photomanager.infrastructure.persistence.entity.SearchPresetEntity;
import com.jpablodrexler.photomanager.infrastructure.persistence.entity.UserEntity;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SearchPresetEntityMapperImplTest {

    private final SearchPresetEntityMapper sut = new SearchPresetEntityMapperImpl();

    @Test
    void toDomain_entityWithUser_mapsUserIdFromNestedUser() {
        UUID userId = UUID.randomUUID();
        UserEntity user = new UserEntity();
        user.setId(userId);
        Instant createdAt = Instant.parse("2026-01-01T00:00:00Z");

        SearchPresetEntity entity = new SearchPresetEntity();
        entity.setPresetId(1L);
        entity.setUser(user);
        entity.setName("Sunsets");
        entity.setFilterJson("{\"search\":\"sunset\"}");
        entity.setCreatedAt(createdAt);

        SearchPreset result = sut.toDomain(entity);

        assertThat(result.getPresetId()).isEqualTo(1L);
        assertThat(result.getUserId()).isEqualTo(userId);
        assertThat(result.getName()).isEqualTo("Sunsets");
        assertThat(result.getFilterJson()).isEqualTo("{\"search\":\"sunset\"}");
        assertThat(result.getCreatedAt()).isEqualTo(createdAt);
    }

    @Test
    void toDomain_entityWithNullUser_leavesUserIdNull() {
        SearchPresetEntity entity = new SearchPresetEntity();
        entity.setPresetId(2L);
        entity.setName("Empty");

        SearchPreset result = sut.toDomain(entity);

        assertThat(result.getUserId()).isNull();
    }

    @Test
    void toDomain_nullEntity_returnsNull() {
        assertThat(sut.toDomain(null)).isNull();
    }

    @Test
    void toEntity_domainWithUserId_buildsUserRefWithOnlyId() {
        UUID userId = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-01-02T00:00:00Z");
        SearchPreset domain = SearchPreset.builder()
                .presetId(3L)
                .userId(userId)
                .name("Beaches")
                .filterJson("{\"search\":\"beach\"}")
                .createdAt(createdAt)
                .build();

        SearchPresetEntity result = sut.toEntity(domain);

        assertThat(result.getPresetId()).isEqualTo(3L);
        assertThat(result.getUser()).isNotNull();
        assertThat(result.getUser().getId()).isEqualTo(userId);
        assertThat(result.getName()).isEqualTo("Beaches");
        assertThat(result.getFilterJson()).isEqualTo("{\"search\":\"beach\"}");
        assertThat(result.getCreatedAt()).isEqualTo(createdAt);
    }

    @Test
    void toEntity_domainWithNullUserId_leavesUserNull() {
        SearchPreset domain = SearchPreset.builder().presetId(4L).name("NoUser").build();

        SearchPresetEntity result = sut.toEntity(domain);

        assertThat(result.getUser()).isNull();
    }

    @Test
    void toEntity_nullDomain_returnsNull() {
        assertThat(sut.toEntity(null)).isNull();
    }
}
