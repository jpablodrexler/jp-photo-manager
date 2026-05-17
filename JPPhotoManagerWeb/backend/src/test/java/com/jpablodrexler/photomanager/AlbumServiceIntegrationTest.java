package com.jpablodrexler.photomanager;

import com.jpablodrexler.photomanager.application.dto.AlbumData;
import com.jpablodrexler.photomanager.application.dto.PaginatedResult;
import com.jpablodrexler.photomanager.application.exception.AlbumNotFoundException;
import com.jpablodrexler.photomanager.domain.model.Asset;
import com.jpablodrexler.photomanager.domain.model.Folder;
import com.jpablodrexler.photomanager.domain.model.User;
import com.jpablodrexler.photomanager.domain.port.in.album.*;
import com.jpablodrexler.photomanager.domain.port.out.AlbumRepository;
import com.jpablodrexler.photomanager.domain.port.out.AssetRepository;
import com.jpablodrexler.photomanager.domain.port.out.FolderRepository;
import com.jpablodrexler.photomanager.domain.port.out.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.testcontainers.junit.jupiter.EnabledIfDockerAvailable;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
@EnabledIfDockerAvailable
class AlbumServiceIntegrationTest extends PostgresIntegrationTest {

    @Autowired
    CreateAlbumUseCase createAlbumUseCase;
    @Autowired
    GetAlbumUseCase getAlbumUseCase;
    @Autowired
    DeleteAlbumUseCase deleteAlbumUseCase;
    @Autowired
    AddAssetsToAlbumUseCase addAssetsToAlbumUseCase;
    @Autowired
    RemoveAssetsFromAlbumUseCase removeAssetsFromAlbumUseCase;
    @Autowired
    AlbumRepository albumRepository;
    @Autowired
    UserRepository userRepository;
    @Autowired
    AssetRepository assetRepository;
    @Autowired
    FolderRepository folderRepository;

    private UUID testUserId;
    private Asset asset1;
    private Asset asset2;

    @BeforeEach
    void setUp() {
        assetRepository.findAll().forEach(a -> assetRepository.deleteById(a.getAssetId()));
        userRepository.findAll().forEach(u -> userRepository.deleteById(u.getId()));

        User testUser = new User();
        testUser.setUsername("testuser_" + System.nanoTime());
        testUser.setPasswordHash("hash");
        testUser.setCreatedAt(Instant.now());
        testUser.setRole("USER");
        testUser = userRepository.save(testUser);
        testUserId = testUser.getId();

        Folder folder = new Folder();
        folder.setPath("/test/photos_" + System.nanoTime());
        folder = folderRepository.save(folder);

        asset1 = new Asset();
        asset1.setFolder(folder);
        asset1.setFileName("a.jpg");
        asset1.setFileSize(1000L);
        asset1.setHash("hash1");
        asset1.setThumbnailCreationDateTime(LocalDateTime.now());
        asset1 = assetRepository.save(asset1);

        asset2 = new Asset();
        asset2.setFolder(folder);
        asset2.setFileName("b.jpg");
        asset2.setFileSize(2000L);
        asset2.setHash("hash2");
        asset2.setThumbnailCreationDateTime(LocalDateTime.now());
        asset2 = assetRepository.save(asset2);
    }

    @Test
    void createAlbum_andAddAssets_getAlbumAssetsReturnsCorrectPage() {
        AlbumData album = createAlbumUseCase.execute(testUserId, "My Album", "desc");
        assertThat(album.albumId()).isNotNull();

        addAssetsToAlbumUseCase.execute(album.albumId(), testUserId, List.of(asset1.getAssetId(), asset2.getAssetId()));

        PaginatedResult<Asset> page = getAlbumUseCase.executeAssets(album.albumId(), testUserId, 0);
        assertThat(page.total()).isEqualTo(2);
        assertThat(page.items()).extracting(Asset::getAssetId)
                .containsExactlyInAnyOrder(asset1.getAssetId(), asset2.getAssetId());
    }

    @Test
    void addAssets_duplicate_isIdempotent() {
        AlbumData album = createAlbumUseCase.execute(testUserId, "Album", null);
        addAssetsToAlbumUseCase.execute(album.albumId(), testUserId, List.of(asset1.getAssetId()));
        addAssetsToAlbumUseCase.execute(album.albumId(), testUserId, List.of(asset1.getAssetId()));

        PaginatedResult<Asset> page = getAlbumUseCase.executeAssets(album.albumId(), testUserId, 0);
        assertThat(page.total()).isEqualTo(1);
    }

    @Test
    void removeAsset_decrementCount() {
        AlbumData album = createAlbumUseCase.execute(testUserId, "Album", null);
        addAssetsToAlbumUseCase.execute(album.albumId(), testUserId, List.of(asset1.getAssetId(), asset2.getAssetId()));

        removeAssetsFromAlbumUseCase.execute(album.albumId(), testUserId, List.of(asset1.getAssetId()));

        PaginatedResult<Asset> page = getAlbumUseCase.executeAssets(album.albumId(), testUserId, 0);
        assertThat(page.total()).isEqualTo(1);
        assertThat(page.items().get(0).getAssetId()).isEqualTo(asset2.getAssetId());
    }

    @Test
    void deleteAlbum_albumNoLongerFound() {
        AlbumData album = createAlbumUseCase.execute(testUserId, "Temp Album", null);
        Long albumId = album.albumId();

        deleteAlbumUseCase.execute(albumId, testUserId);

        assertThat(albumRepository.findByIdAndUserId(albumId, testUserId)).isEmpty();
    }

    @Test
    void getAlbumAssets_unknownAlbum_throwsAlbumNotFoundException() {
        assertThatThrownBy(() -> getAlbumUseCase.executeAssets(99999L, testUserId, 0))
                .isInstanceOf(AlbumNotFoundException.class);
    }
}
