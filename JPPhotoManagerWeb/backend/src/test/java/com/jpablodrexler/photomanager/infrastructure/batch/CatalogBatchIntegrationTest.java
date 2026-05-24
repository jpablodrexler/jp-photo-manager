package com.jpablodrexler.photomanager.infrastructure.batch;

import com.jpablodrexler.photomanager.PostgresIntegrationTest;
import com.jpablodrexler.photomanager.application.dto.CatalogChangeNotification;
import com.jpablodrexler.photomanager.domain.enums.ReasonEnum;
import com.jpablodrexler.photomanager.domain.model.Asset;
import com.jpablodrexler.photomanager.domain.port.in.catalog.CatalogAssetsUseCase;
import com.jpablodrexler.photomanager.domain.port.out.AssetRepository;
import com.jpablodrexler.photomanager.infrastructure.service.CatalogScheduler;
import com.jpablodrexler.photomanager.infrastructure.service.SseNotificationRegistry;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBatchTest
class CatalogBatchIntegrationTest extends PostgresIntegrationTest {

    static final Path tempDir;

    static {
        try {
            tempDir = Files.createTempDirectory("catalog-batch-test");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @DynamicPropertySource
    static void overrideCatalogFolder(DynamicPropertyRegistry registry) {
        registry.add("photomanager.root-catalog-folders", () -> tempDir.toAbsolutePath().toString());
    }

    @MockBean
    CatalogScheduler catalogScheduler;

    @Autowired
    AssetRepository assetRepository;

    @Autowired
    CatalogAssetsUseCase catalogAssetsUseCase;

    @Autowired
    SseNotificationRegistry sseNotificationRegistry;

    @BeforeAll
    static void createTestImages() throws IOException, URISyntaxException {
        URL resource = CatalogBatchIntegrationTest.class.getClassLoader().getResource("fixtures/test-with-exif.jpg");
        Path source = Paths.get(resource.toURI());
        Files.copy(source, tempDir.resolve("photo1.jpg"), StandardCopyOption.REPLACE_EXISTING);
        Files.copy(source, tempDir.resolve("photo2.jpg"), StandardCopyOption.REPLACE_EXISTING);
        Files.copy(source, tempDir.resolve("photo3.jpg"), StandardCopyOption.REPLACE_EXISTING);
    }

    @BeforeEach
    void cleanState() {
        assetRepository.findAll().forEach(a -> assetRepository.deleteById(a.getAssetId()));
    }

    @Test
    void catalogJob_withThreeImages_createsThreeAssets() throws Exception {
        catalogAssetsUseCase.execute(null).get();

        List<Asset> assets = assetRepository.findAll();
        assertThat(assets).hasSize(3);
    }

    @Test
    void catalogJob_secondRun_skipsAlreadyCatalogedFiles() throws Exception {
        catalogAssetsUseCase.execute(null).get();

        catalogAssetsUseCase.execute(null).get();

        List<Asset> assets = assetRepository.findAll();
        assertThat(assets).hasSize(3);
    }

    @Test
    void catalogJob_sseNotificationsReceived() throws Exception {
        List<CatalogChangeNotification> notifications = new CopyOnWriteArrayList<>();

        catalogAssetsUseCase.execute(notifications::add).get();

        long assetCreatedCount = notifications.stream()
                .filter(n -> n.getReason() == ReasonEnum.ASSET_CREATED)
                .count();
        assertThat(assetCreatedCount).isEqualTo(3);
    }
}
