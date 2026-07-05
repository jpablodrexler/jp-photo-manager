package com.jpablodrexler.photomanager;

import com.jpablodrexler.photomanager.domain.model.Asset;
import com.jpablodrexler.photomanager.domain.model.Folder;
import com.jpablodrexler.photomanager.domain.port.out.AssetRepository;
import com.jpablodrexler.photomanager.domain.port.out.FolderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.EnabledIfDockerAvailable;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
@EnabledIfDockerAvailable
class HomeStatsIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18");

    @Container
    @ServiceConnection
    static MongoDBContainer mongo = new MongoDBContainer("mongo:8");

    @Autowired
    MockMvc mockMvc;

    @Autowired
    AssetRepository assetRepository;

    @Autowired
    FolderRepository folderRepository;

    @Autowired
    CacheManager cacheManager;

    @BeforeEach
    void setUp() {
        assetRepository.findAll().forEach(a -> assetRepository.deleteById(a.getAssetId()));
        // GetHomeStatsUseCase now caches its result under "home-stats" (maximumSize=1), and the
        // Spring test context is reused across test methods in this class, so a value cached by
        // an earlier test would otherwise leak into later tests since this setup mutates data
        // directly through the repository rather than through a cache-evicting use case.
        cacheManager.getCache("home-stats").clear();
    }

    @Test
    void getHomeStats_emptyLibrary_returnsZeroedStats() throws Exception {
        mockMvc.perform(get("/api/home/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalFileSize").value(0))
                .andExpect(jsonPath("$.duplicateCount").value(0))
                .andExpect(jsonPath("$.topFolders").isArray())
                .andExpect(jsonPath("$.topFolders").isEmpty())
                .andExpect(jsonPath("$.recentAssets").isArray())
                .andExpect(jsonPath("$.recentAssets").isEmpty());
    }

    @Test
    void getHomeStats_withAssets_returnsTotalFileSize() throws Exception {
        Folder folder = new Folder();
        folder.setPath("/test/home-stats-size-" + System.nanoTime());
        folder = folderRepository.save(folder);

        saveAsset(folder, "a.jpg", 1_000_000L, "hash-a", LocalDateTime.now());
        saveAsset(folder, "b.jpg", 2_000_000L, "hash-b", LocalDateTime.now());

        mockMvc.perform(get("/api/home/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalFileSize").value(3_000_000L));
    }

    @Test
    void getHomeStats_withDuplicateHashes_returnsDuplicateCount() throws Exception {
        Folder folder = new Folder();
        folder.setPath("/test/home-stats-dupes-" + System.nanoTime());
        folder = folderRepository.save(folder);

        String sharedHash = "same-hash-" + System.nanoTime();
        saveAsset(folder, "a.jpg", 100L, sharedHash, LocalDateTime.now());
        saveAsset(folder, "b.jpg", 100L, sharedHash, LocalDateTime.now());
        saveAsset(folder, "c.jpg", 100L, sharedHash, LocalDateTime.now());
        saveAsset(folder, "unique.jpg", 100L, "unique-hash-" + System.nanoTime(), LocalDateTime.now());

        mockMvc.perform(get("/api/home/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.duplicateCount").value(3));
    }

    @Test
    void getHomeStats_withMultipleFolders_returnsTopFoldersByAssetCount() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        Folder folderA = new Folder();
        folderA.setPath("/test/home-stats-top-a-" + suffix);
        folderA = folderRepository.save(folderA);

        Folder folderB = new Folder();
        folderB.setPath("/test/home-stats-top-b-" + suffix);
        folderB = folderRepository.save(folderB);

        String pathA = folderA.getPath();
        String pathB = folderB.getPath();

        for (int i = 0; i < 3; i++) {
            saveAsset(folderA, "a" + i + ".jpg", 100L, "hash-a-" + suffix + "-" + i, LocalDateTime.now());
        }
        saveAsset(folderB, "b1.jpg", 100L, "hash-b-" + suffix + "-1", LocalDateTime.now());

        mockMvc.perform(get("/api/home/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.topFolders").isArray())
                .andExpect(jsonPath("$.topFolders[0].path").value(pathA))
                .andExpect(jsonPath("$.topFolders[0].assetCount").value(3));
    }

    @Test
    void getHomeStats_withAssets_returnsRecentAssetsWithThumbnailUrls() throws Exception {
        Folder folder = new Folder();
        folder.setPath("/test/home-stats-recent-" + System.nanoTime());
        folder = folderRepository.save(folder);

        Asset saved = saveAsset(folder, "recent.jpg", 500L, "hash-recent-" + System.nanoTime(), LocalDateTime.now());

        mockMvc.perform(get("/api/home/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recentAssets").isArray())
                .andExpect(jsonPath("$.recentAssets[0].fileName").value("recent.jpg"))
                .andExpect(jsonPath("$.recentAssets[0].folderPath").value(folder.getPath()))
                .andExpect(jsonPath("$.recentAssets[0].thumbnailUrl").value("/api/assets/" + saved.getAssetId() + "/thumbnail"));
    }

    private Asset saveAsset(Folder folder, String fileName, long fileSize, String hash, LocalDateTime thumbnailCreationDateTime) {
        Asset asset = new Asset();
        asset.setFolder(folder);
        asset.setFileName(fileName);
        asset.setFileSize(fileSize);
        asset.setHash(hash);
        asset.setThumbnailCreationDateTime(thumbnailCreationDateTime);
        return assetRepository.save(asset);
    }
}
