package com.jpablodrexler.photomanager;

import com.jpablodrexler.photomanager.domain.model.Asset;
import com.jpablodrexler.photomanager.domain.port.in.asset.DeleteAssetsUseCase;
import com.jpablodrexler.photomanager.domain.port.out.AssetExifRepository;
import com.jpablodrexler.photomanager.domain.port.out.CatalogFolderPort;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.testcontainers.junit.jupiter.EnabledIfDockerAvailable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
@EnabledIfDockerAvailable
class ExifMetadataIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18");

    @Container
    @ServiceConnection
    static MongoDBContainer mongo = new MongoDBContainer("mongo:8");

    @TempDir
    static Path tempDir;

    static Path fixtureDir;

    @Autowired
    MockMvc mockMvc;

    @Autowired
    CatalogFolderPort catalogFolderService;

    @Autowired
    AssetExifRepository assetExifRepository;

    @Autowired
    DeleteAssetsUseCase deleteAssetsUseCase;

    @BeforeAll
    static void copyFixture() throws Exception {
        fixtureDir = tempDir.resolve("photos");
        Files.createDirectories(fixtureDir);
        try (InputStream in = ExifMetadataIntegrationTest.class.getClassLoader()
                .getResourceAsStream("fixtures/test-with-exif.jpg")) {
            assertThat(in).as("JPEG fixture not found").isNotNull();
            Files.copy(in, fixtureDir.resolve("test-with-exif.jpg"), StandardCopyOption.REPLACE_EXISTING);
        }
        Files.createDirectories(Path.of("/tmp/thumbnails"));
    }

    @Test
    void catalogedJpeg_getExifEndpoint_returnsCameraMake() throws Exception {
        Asset asset = catalogFolderService.createAsset(fixtureDir.toString(), "test-with-exif.jpg");

        mockMvc.perform(get("/api/assets/" + asset.getAssetId() + "/exif"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cameraMake").value("TestCamera"))
                .andExpect(jsonPath("$.cameraModel").value("ModelX"));
    }

    @Test
    void getExifEndpoint_unknownAsset_returns404() throws Exception {
        mockMvc.perform(get("/api/assets/999999/exif"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void purgeAsset_removesMongoExifDocument() throws Exception {
        Asset asset = catalogFreshAsset("purge", "purge-target.jpg");
        assertThat(assetExifRepository.findByAssetId(asset.getAssetId())).isPresent();

        deleteAssetsUseCase.execute(new Long[]{asset.getAssetId()}, true);

        assertThat(assetExifRepository.findByAssetId(asset.getAssetId())).isEmpty();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void softDeleteAsset_keepsMongoExifDocument() throws Exception {
        Asset asset = catalogFreshAsset("soft-delete", "soft-delete-target.jpg");
        assertThat(assetExifRepository.findByAssetId(asset.getAssetId())).isPresent();

        deleteAssetsUseCase.execute(new Long[]{asset.getAssetId()}, false);

        assertThat(assetExifRepository.findByAssetId(asset.getAssetId())).isPresent();
    }

    /**
     * Catalogs a fresh copy of the EXIF fixture in its own isolated subfolder (named after
     * {@code testId}), avoiding interference from other tests cataloging/deleting assets in this
     * shared Spring context.
     */
    private Asset catalogFreshAsset(String testId, String fileName) throws Exception {
        Path folder = tempDir.resolve("photos-" + testId);
        Files.createDirectories(folder);
        try (InputStream in = ExifMetadataIntegrationTest.class.getClassLoader()
                .getResourceAsStream("fixtures/test-with-exif.jpg")) {
            Files.copy(in, folder.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);
        }

        return catalogFolderService.createAsset(folder.toString(), fileName);
    }
}
