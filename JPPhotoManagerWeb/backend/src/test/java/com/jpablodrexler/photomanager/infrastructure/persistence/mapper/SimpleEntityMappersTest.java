package com.jpablodrexler.photomanager.infrastructure.persistence.mapper;

import com.jpablodrexler.photomanager.domain.model.*;
import com.jpablodrexler.photomanager.infrastructure.persistence.entity.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SimpleEntityMappersTest {

    @Nested
    class FolderEntityMapperImplTest {

        private final FolderEntityMapper sut = new FolderEntityMapperImpl();

        @Test
        void toDomain_mapsFields() {
            FolderEntity entity = new FolderEntity();
            entity.setFolderId(1L);
            entity.setPath("/photos");

            Folder result = sut.toDomain(entity);

            assertThat(result.getFolderId()).isEqualTo(1L);
            assertThat(result.getPath()).isEqualTo("/photos");
        }

        @Test
        void toDomain_nullEntity_returnsNull() {
            assertThat(sut.toDomain(null)).isNull();
        }

        @Test
        void toEntity_mapsFields() {
            Folder folder = Folder.builder().folderId(2L).path("/photos/2024").build();

            FolderEntity result = sut.toEntity(folder);

            assertThat(result.getFolderId()).isEqualTo(2L);
            assertThat(result.getPath()).isEqualTo("/photos/2024");
        }

        @Test
        void toEntity_nullDomain_returnsNull() {
            assertThat(sut.toEntity(null)).isNull();
        }
    }

    @Nested
    class SyncConfigEntityMapperImplTest {

        private final SyncConfigEntityMapper sut = new SyncConfigEntityMapperImpl();

        @Test
        void toDomain_mapsFields() {
            SyncAssetsDirectoriesDefinitionEntity entity = new SyncAssetsDirectoriesDefinitionEntity();
            entity.setId(1L);
            entity.setSourceDirectory("/src");
            entity.setDestinationDirectory("/dst");
            entity.setIncludeSubFolders(true);
            entity.setDeleteAssetsNotInSource(false);
            entity.setOrder(2);

            SyncDirectoriesDefinition result = sut.toDomain(entity);

            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getSourceDirectory()).isEqualTo("/src");
            assertThat(result.getDestinationDirectory()).isEqualTo("/dst");
            assertThat(result.isIncludeSubFolders()).isTrue();
            assertThat(result.isDeleteAssetsNotInSource()).isFalse();
            assertThat(result.getOrder()).isEqualTo(2);
        }

        @Test
        void toEntity_mapsFields() {
            SyncDirectoriesDefinition domain = SyncDirectoriesDefinition.builder()
                    .sourceDirectory("/src")
                    .destinationDirectory("/dst")
                    .includeSubFolders(false)
                    .deleteAssetsNotInSource(true)
                    .order(1)
                    .build();

            SyncAssetsDirectoriesDefinitionEntity result = sut.toEntity(domain);

            assertThat(result.getSourceDirectory()).isEqualTo("/src");
            assertThat(result.getDestinationDirectory()).isEqualTo("/dst");
            assertThat(result.isIncludeSubFolders()).isFalse();
            assertThat(result.isDeleteAssetsNotInSource()).isTrue();
            assertThat(result.getOrder()).isEqualTo(1);
        }
    }

    @Nested
    class ConvertConfigEntityMapperImplTest {

        private final ConvertConfigEntityMapper sut = new ConvertConfigEntityMapperImpl();

        @Test
        void toDomain_mapsFields() {
            ConvertAssetsDirectoriesDefinitionEntity entity = new ConvertAssetsDirectoriesDefinitionEntity();
            entity.setId(1L);
            entity.setSourceDirectory("/src");
            entity.setDestinationDirectory("/dst");
            entity.setIncludeSubFolders(true);
            entity.setOrder(3);

            ConvertDirectoriesDefinition result = sut.toDomain(entity);

            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getSourceDirectory()).isEqualTo("/src");
            assertThat(result.getDestinationDirectory()).isEqualTo("/dst");
            assertThat(result.isIncludeSubFolders()).isTrue();
            assertThat(result.getOrder()).isEqualTo(3);
        }

        @Test
        void toEntity_mapsFields() {
            ConvertDirectoriesDefinition domain = ConvertDirectoriesDefinition.builder()
                    .sourceDirectory("/src")
                    .destinationDirectory("/dst")
                    .includeSubFolders(true)
                    .order(1)
                    .build();

            ConvertAssetsDirectoriesDefinitionEntity result = sut.toEntity(domain);

            assertThat(result.getSourceDirectory()).isEqualTo("/src");
            assertThat(result.getDestinationDirectory()).isEqualTo("/dst");
            assertThat(result.isIncludeSubFolders()).isTrue();
            assertThat(result.getOrder()).isEqualTo(1);
        }
    }

    @Nested
    class AssetExifEntityMapperImplTest {

        private final AssetExifEntityMapper sut = new AssetExifEntityMapperImpl();

        @Test
        void toDomain_mapsFields() {
            AssetExifEntity entity = new AssetExifEntity();
            entity.setAssetId(5L);
            entity.setCameraMake("Canon");
            entity.setCameraModel("EOS R5");
            entity.setLensModel("RF 85mm");
            entity.setExposureTime("1/500");
            entity.setIsoSpeed(400);
            entity.setFocalLength(85.0);
            entity.setDateTaken(LocalDateTime.of(2024, 6, 15, 10, 30));
            entity.setWidthPixels(4500);
            entity.setHeightPixels(3000);
            entity.setGpsLatitude(-34.6);
            entity.setGpsLongitude(-58.4);

            AssetExif result = sut.toDomain(entity);

            assertThat(result.getAssetId()).isEqualTo(5L);
            assertThat(result.getCameraMake()).isEqualTo("Canon");
            assertThat(result.getCameraModel()).isEqualTo("EOS R5");
            assertThat(result.getLensModel()).isEqualTo("RF 85mm");
            assertThat(result.getExposureTime()).isEqualTo("1/500");
            assertThat(result.getIsoSpeed()).isEqualTo(400);
            assertThat(result.getFocalLength()).isEqualTo(85.0);
            assertThat(result.getWidthPixels()).isEqualTo(4500);
            assertThat(result.getHeightPixels()).isEqualTo(3000);
            assertThat(result.getGpsLatitude()).isEqualTo(-34.6);
            assertThat(result.getGpsLongitude()).isEqualTo(-58.4);
        }

        @Test
        void toDomain_nullEntity_returnsNull() {
            assertThat(sut.toDomain(null)).isNull();
        }
    }

    @Nested
    class UserEntityMapperImplTest {

        private final UserEntityMapper sut = new UserEntityMapperImpl();

        @Test
        void toDomain_mapsFields() {
            UUID id = UUID.randomUUID();
            Instant now = Instant.now();
            UserEntity entity = new UserEntity();
            entity.setId(id);
            entity.setUsername("alice");
            entity.setPasswordHash("hash");
            entity.setCreatedAt(now);
            entity.setRole("ADMIN");

            User result = sut.toDomain(entity);

            assertThat(result.getId()).isEqualTo(id);
            assertThat(result.getUsername()).isEqualTo("alice");
            assertThat(result.getPasswordHash()).isEqualTo("hash");
            assertThat(result.getCreatedAt()).isEqualTo(now);
            assertThat(result.getRole()).isEqualTo("ADMIN");
        }

        @Test
        void toEntity_mapsFields() {
            UUID id = UUID.randomUUID();
            User user = User.builder().id(id).username("bob").passwordHash("hash2").role("USER").build();

            UserEntity result = sut.toEntity(user);

            assertThat(result.getId()).isEqualTo(id);
            assertThat(result.getUsername()).isEqualTo("bob");
            assertThat(result.getPasswordHash()).isEqualTo("hash2");
            assertThat(result.getRole()).isEqualTo("USER");
        }

        @Test
        void toEntityRef_returnsEntityWithOnlyId() {
            UUID id = UUID.randomUUID();
            User user = User.builder().id(id).username("charlie").build();

            UserEntity result = sut.toEntityRef(user);

            assertThat(result.getId()).isEqualTo(id);
            assertThat(result.getUsername()).isNull();
        }

        @Test
        void toEntityRef_nullUser_returnsNull() {
            assertThat(sut.toEntityRef(null)).isNull();
        }
    }

    @Nested
    class AssetEntityMapperImplTest {

        private AssetEntityMapper sut;

        @BeforeEach
        void setUp() {
            AssetEntityMapperImpl impl = new AssetEntityMapperImpl();
            ReflectionTestUtils.setField(impl, "folderEntityMapper", new FolderEntityMapperImpl());
            sut = impl;
        }

        @Test
        void toDomain_mapsAllFields() {
            FolderEntity folderEntity = new FolderEntity();
            folderEntity.setFolderId(1L);
            folderEntity.setPath("/photos");

            AssetEntity entity = new AssetEntity();
            entity.setAssetId(10L);
            entity.setFolder(folderEntity);
            entity.setFileName("img.jpg");
            entity.setFileSize(2048L);
            entity.setRating(3);

            Asset result = sut.toDomain(entity);

            assertThat(result.getAssetId()).isEqualTo(10L);
            assertThat(result.getFolder().getFolderId()).isEqualTo(1L);
            assertThat(result.getFolder().getPath()).isEqualTo("/photos");
            assertThat(result.getFileName()).isEqualTo("img.jpg");
            assertThat(result.getFileSize()).isEqualTo(2048L);
            assertThat(result.getRating()).isEqualTo(3);
        }

        @Test
        void toDomain_nullEntity_returnsNull() {
            assertThat(sut.toDomain(null)).isNull();
        }

        @Test
        void toEntity_mapsAllFields() {
            Folder folder = Folder.builder().folderId(2L).path("/pics").build();
            Asset domain = Asset.builder()
                    .assetId(20L)
                    .folder(folder)
                    .fileName("photo.jpg")
                    .fileSize(512L)
                    .rating(5)
                    .build();

            AssetEntity result = sut.toEntity(domain);

            assertThat(result.getAssetId()).isEqualTo(20L);
            assertThat(result.getFolder().getFolderId()).isEqualTo(2L);
            assertThat(result.getFileName()).isEqualTo("photo.jpg");
            assertThat(result.getFileSize()).isEqualTo(512L);
            assertThat(result.getRating()).isEqualTo(5);
        }
    }

    @Nested
    class AlbumEntityMapperImplTest {

        private final AlbumEntityMapper sut = new AlbumEntityMapperImpl();

        @Test
        void toDomain_mapsFields() {
            UserEntity userEntity = new UserEntity();
            UUID userId = UUID.randomUUID();
            userEntity.setId(userId);

            AlbumEntity entity = new AlbumEntity();
            entity.setAlbumId(1L);
            entity.setUser(userEntity);
            entity.setName("Vacation");
            entity.setDescription("Summer trip");
            entity.setCreatedAt(Instant.now());

            Album result = sut.toDomain(entity);

            assertThat(result.getAlbumId()).isEqualTo(1L);
            assertThat(result.getUserId()).isEqualTo(userId);
            assertThat(result.getName()).isEqualTo("Vacation");
            assertThat(result.getDescription()).isEqualTo("Summer trip");
            assertThat(result.getAssets()).isEmpty();
        }

        @Test
        void toEntity_mapsFields() {
            Album album = Album.builder().albumId(2L).name("Nature").description("Birds").build();

            AlbumEntity result = sut.toEntity(album);

            assertThat(result.getAlbumId()).isEqualTo(2L);
            assertThat(result.getName()).isEqualTo("Nature");
            assertThat(result.getDescription()).isEqualTo("Birds");
            assertThat(result.getUser()).isNull();
        }
    }
}
