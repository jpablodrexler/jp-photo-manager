package com.jpablodrexler.photomanager;

import com.jpablodrexler.photomanager.domain.model.Asset;
import com.jpablodrexler.photomanager.domain.port.out.AssetRepository;
import com.jpablodrexler.photomanager.domain.service.CatalogFolderService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

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

    @TempDir
    static Path tempDir;

    static Path fixtureDir;

    @Autowired
    MockMvc mockMvc;

    @Autowired
    CatalogFolderService catalogFolderService;

    @Autowired
    AssetRepository assetRepository;

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
        catalogFolderService.catalogFolder(
                fixtureDir.toString(), null, () -> {}, new AtomicInteger(0), 1);

        List<Asset> assets = assetRepository.findAll();
        Asset asset = assets.stream()
                .filter(a -> "test-with-exif.jpg".equals(a.getFileName()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Asset not found after cataloging"));

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
}
