package com.jpablodrexler.photomanager;

import com.jpablodrexler.photomanager.application.dto.PaginatedData;
import com.jpablodrexler.photomanager.domain.entity.Asset;
import com.jpablodrexler.photomanager.domain.entity.Folder;
import com.jpablodrexler.photomanager.domain.entity.User;
import com.jpablodrexler.photomanager.domain.repository.AssetRepository;
import com.jpablodrexler.photomanager.domain.repository.FolderRepository;
import com.jpablodrexler.photomanager.domain.repository.UserRepository;
import com.jpablodrexler.photomanager.domain.service.AlbumService;
import com.jpablodrexler.photomanager.domain.entity.Album;
import com.jpablodrexler.photomanager.api.exception.AlbumNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.testcontainers.junit.jupiter.EnabledIfDockerAvailable;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
@EnabledIfDockerAvailable
class AlbumServiceIntegrationTest extends PostgresIntegrationTest {

    @Autowired
    AlbumService albumService;

    @Autowired
    UserRepository userRepository;

    @Autowired
    AssetRepository assetRepository;

    @Autowired
    FolderRepository folderRepository;

    private User testUser;
    private Asset asset1;
    private Asset asset2;

    @BeforeEach
    void setUp() {
        assetRepository.deleteAll();
        userRepository.deleteAll();

        testUser = new User();
        testUser.setUsername("testuser_" + System.nanoTime());
        testUser.setPasswordHash("hash");
        testUser.setCreatedAt(Instant.now());
        testUser = userRepository.save(testUser);

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
        Album album = albumService.createAlbum(testUser.getId(), "My Album", "desc");
        assertThat(album.getAlbumId()).isNotNull();

        albumService.addAssets(album.getAlbumId(), testUser.getId(), List.of(asset1.getAssetId(), asset2.getAssetId()));

        PaginatedData<Asset> page = albumService.getAlbumAssets(album.getAlbumId(), testUser.getId(), 0);
        assertThat(page.getTotalItems()).isEqualTo(2);
        assertThat(page.getItems()).extracting(Asset::getAssetId)
                .containsExactlyInAnyOrder(asset1.getAssetId(), asset2.getAssetId());
    }

    @Test
    void addAssets_duplicate_isIdempotent() {
        Album album = albumService.createAlbum(testUser.getId(), "Album", null);
        albumService.addAssets(album.getAlbumId(), testUser.getId(), List.of(asset1.getAssetId()));
        albumService.addAssets(album.getAlbumId(), testUser.getId(), List.of(asset1.getAssetId()));

        PaginatedData<Asset> page = albumService.getAlbumAssets(album.getAlbumId(), testUser.getId(), 0);
        assertThat(page.getTotalItems()).isEqualTo(1);
    }

    @Test
    void removeAsset_decrementCount() {
        Album album = albumService.createAlbum(testUser.getId(), "Album", null);
        albumService.addAssets(album.getAlbumId(), testUser.getId(), List.of(asset1.getAssetId(), asset2.getAssetId()));

        albumService.removeAssets(album.getAlbumId(), testUser.getId(), List.of(asset1.getAssetId()));

        PaginatedData<Asset> page = albumService.getAlbumAssets(album.getAlbumId(), testUser.getId(), 0);
        assertThat(page.getTotalItems()).isEqualTo(1);
        assertThat(page.getItems().get(0).getAssetId()).isEqualTo(asset2.getAssetId());
    }

    @Test
    void deleteAlbum_albumNoLongerFound() {
        Album album = albumService.createAlbum(testUser.getId(), "Temp Album", null);
        Long albumId = album.getAlbumId();

        albumService.deleteAlbum(albumId, testUser.getId());

        Optional<Album> found = albumService.findByIdAndUserId(albumId, testUser.getId());
        assertThat(found).isEmpty();
    }

    @Test
    void getAlbumAssets_unknownAlbum_throwsAlbumNotFoundException() {
        assertThatThrownBy(() -> albumService.getAlbumAssets(99999L, testUser.getId(), 0))
                .isInstanceOf(AlbumNotFoundException.class);
    }
}
