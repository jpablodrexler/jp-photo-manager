package com.jpablodrexler.photomanager.infrastructure.persistence.mapper;

import com.jpablodrexler.photomanager.domain.model.Album;
import com.jpablodrexler.photomanager.infrastructure.persistence.entity.AlbumEntity;
import com.jpablodrexler.photomanager.infrastructure.persistence.entity.UserEntity;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AlbumEntityMapperTest {

    private final AlbumEntityMapper sut = new AlbumEntityMapperImpl();

    @Test
    void toDomain_nullEntity_returnsNull() {
        assertThat(sut.toDomain(null)).isNull();
    }

    @Test
    void toDomain_entityWithUser_mapsUserIdAndEmptyAssets() {
        UUID userId = UUID.randomUUID();
        UserEntity user = new UserEntity();
        user.setId(userId);
        AlbumEntity entity = new AlbumEntity();
        entity.setAlbumId(1L);
        entity.setUser(user);
        entity.setName("Trip");
        entity.setDescription("desc");
        entity.setCreatedAt(Instant.parse("2024-01-01T00:00:00Z"));
        entity.setFilterJson("{\"search\":\"sunset\"}");

        Album result = sut.toDomain(entity);

        assertThat(result.getAlbumId()).isEqualTo(1L);
        assertThat(result.getUserId()).isEqualTo(userId);
        assertThat(result.getName()).isEqualTo("Trip");
        assertThat(result.getDescription()).isEqualTo("desc");
        assertThat(result.getFilterJson()).isEqualTo("{\"search\":\"sunset\"}");
        assertThat(result.getAssets()).isEmpty();
    }

    @Test
    void toDomain_entityWithNullUser_mapsNullUserId() {
        AlbumEntity entity = new AlbumEntity();
        entity.setAlbumId(2L);
        entity.setUser(null);
        entity.setName("No user");

        Album result = sut.toDomain(entity);

        assertThat(result.getUserId()).isNull();
    }

    @Test
    void toEntity_nullDomain_returnsNull() {
        assertThat(sut.toEntity(null)).isNull();
    }

    @Test
    void toEntity_domainWithUserId_mapsUserReference() {
        UUID userId = UUID.randomUUID();
        Album domain = Album.builder()
                .albumId(3L)
                .userId(userId)
                .name("Trip")
                .description("desc")
                .createdAt(Instant.parse("2024-01-01T00:00:00Z"))
                .filterJson("{\"search\":\"beach\"}")
                .build();

        AlbumEntity result = sut.toEntity(domain);

        assertThat(result.getAlbumId()).isEqualTo(3L);
        assertThat(result.getUser()).isNotNull();
        assertThat(result.getUser().getId()).isEqualTo(userId);
        assertThat(result.getName()).isEqualTo("Trip");
        assertThat(result.getDescription()).isEqualTo("desc");
        assertThat(result.getFilterJson()).isEqualTo("{\"search\":\"beach\"}");
    }

    @Test
    void toEntity_domainWithNullUserId_mapsNullUserReference() {
        Album domain = Album.builder()
                .albumId(4L)
                .userId(null)
                .name("No user")
                .build();

        AlbumEntity result = sut.toEntity(domain);

        assertThat(result.getUser()).isNull();
    }
}
