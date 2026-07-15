package com.jpablodrexler.photomanager.infrastructure.batch;

import com.jpablodrexler.photomanager.PostgresIntegrationTest;
import com.jpablodrexler.photomanager.domain.model.Asset;
import com.jpablodrexler.photomanager.domain.port.in.catalog.CatalogAssetsUseCase;
import com.jpablodrexler.photomanager.domain.port.out.AssetRepository;
import com.jpablodrexler.photomanager.infrastructure.service.CatalogScheduler;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.kafka.test.context.EmbeddedKafka;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBatchTest
@EmbeddedKafka(partitions = 1, topics = {
    "job.catalog.progress", "job.sync.progress", "job.convert.progress",
    "asset.cataloged", "asset.deleted"
})
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

    @MockitoBean
    CatalogScheduler catalogScheduler;

    @Autowired
    AssetRepository assetRepository;

    @Autowired
    CatalogAssetsUseCase catalogAssetsUseCase;

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
        catalogAssetsUseCase.execute(System.currentTimeMillis()).get(30, TimeUnit.SECONDS);

        List<Asset> assets = assetRepository.findAll();
        assertThat(assets).hasSize(3);
    }

    @Test
    void catalogJob_secondRun_skipsAlreadyCatalogedFiles() throws Exception {
        catalogAssetsUseCase.execute(System.currentTimeMillis()).get(30, TimeUnit.SECONDS);
        catalogAssetsUseCase.execute(System.currentTimeMillis()).get(30, TimeUnit.SECONDS);

        List<Asset> assets = assetRepository.findAll();
        assertThat(assets).hasSize(3);
    }

    @Test
    void catalogJob_completionFuture_completesWhenJobFinishes() throws Exception {
        long runId = System.currentTimeMillis();
        CompletableFuture<Void> future = catalogAssetsUseCase.execute(runId);

        future.get(30, TimeUnit.SECONDS);

        assertThat(future.isDone()).isTrue();
        assertThat(future.isCompletedExceptionally()).isFalse();
    }
}
