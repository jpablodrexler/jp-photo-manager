package com.jpablodrexler.photomanager.infrastructure.batch;

import com.jpablodrexler.photomanager.PostgresIntegrationTest;
import com.jpablodrexler.photomanager.domain.model.Asset;
import com.jpablodrexler.photomanager.domain.port.in.catalog.CatalogAssetsUseCase;
import com.jpablodrexler.photomanager.domain.port.out.AssetRepository;
import com.jpablodrexler.photomanager.domain.port.out.ThumbnailPort;
import com.jpablodrexler.photomanager.infrastructure.service.CatalogScheduler;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Reproduces a real, multi-folder catalog scan under genuine partition concurrency (unlike
 * {@link CatalogBatchIntegrationTest}, which only ever exercises a single folder / single
 * partition) to check whether a file in one folder can ever end up persisted with another
 * file's thumbnail bytes. Each folder gets a distinct, recognizable solid-color image so a
 * cross-partition mix-up is directly observable by decoding the persisted thumbnail.
 */
@SpringBatchTest
@EmbeddedKafka(partitions = 1, topics = {
    "job.catalog.progress", "job.sync.progress", "job.convert.progress",
    "asset.cataloged", "asset.deleted"
})
@WithMockUser(roles = "ADMIN")
class CatalogBatchConcurrencyIntegrationTest extends PostgresIntegrationTest {

    static final Path tempDir;

    // fileName (without extension) -> solid fill color; one distinct subfolder per entry so
    // CatalogFolderPartitioner creates one partition per color, and the grid size below is
    // set to match so every partition actually runs concurrently rather than queueing through
    // a smaller thread pool.
    private static final Map<String, Color> COLOR_FOLDERS = new LinkedHashMap<>();

    static {
        try {
            tempDir = Files.createTempDirectory("catalog-batch-concurrency-test");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        COLOR_FOLDERS.put("red", new Color(220, 20, 20));
        COLOR_FOLDERS.put("green", new Color(20, 220, 20));
        COLOR_FOLDERS.put("blue", new Color(20, 20, 220));
        COLOR_FOLDERS.put("yellow", new Color(220, 220, 20));
        COLOR_FOLDERS.put("magenta", new Color(220, 20, 220));
        COLOR_FOLDERS.put("cyan", new Color(20, 220, 220));
        COLOR_FOLDERS.put("orange", new Color(230, 140, 20));
        COLOR_FOLDERS.put("purple", new Color(120, 20, 200));
    }

    @DynamicPropertySource
    static void overrideCatalogFolder(DynamicPropertyRegistry registry) {
        registry.add("photomanager.root-catalog-folders", () -> tempDir.toAbsolutePath().toString());
        registry.add("photomanager.catalog-partition-grid-size", () -> String.valueOf(COLOR_FOLDERS.size()));
    }

    @MockitoBean
    CatalogScheduler catalogScheduler;

    @Autowired
    AssetRepository assetRepository;

    @Autowired
    ThumbnailPort thumbnailPort;

    @Autowired
    CatalogAssetsUseCase catalogAssetsUseCase;

    @BeforeAll
    static void createColorFolders() throws IOException {
        for (Map.Entry<String, Color> entry : COLOR_FOLDERS.entrySet()) {
            Path folder = Files.createDirectories(tempDir.resolve(entry.getKey()));
            writeSolidColorPng(folder.resolve(entry.getKey() + ".png"), entry.getValue());
        }
    }

    private static void writeSolidColorPng(Path target, Color color) throws IOException {
        BufferedImage image = new BufferedImage(64, 64, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setColor(color);
        g.fillRect(0, 0, 64, 64);
        g.dispose();
        ImageIO.write(image, "png", target.toFile());
    }

    @Test
    void catalogJob_manyFoldersConcurrently_everyAssetGetsItsOwnThumbnail() throws Exception {
        catalogAssetsUseCase.execute(System.currentTimeMillis(), null).get(60, TimeUnit.SECONDS);

        List<Asset> assets = assetRepository.findAll();
        assertThat(assets).hasSize(COLOR_FOLDERS.size());

        for (Asset asset : assets) {
            String colorName = asset.getFileName().substring(0, asset.getFileName().lastIndexOf('.'));
            Color expected = COLOR_FOLDERS.get(colorName);
            assertThat(expected)
                    .as("asset %s (id=%d) has an unexpected file name", asset.getFileName(), asset.getAssetId())
                    .isNotNull();

            byte[] thumbnailBytes = thumbnailPort.loadThumbnail(asset.getThumbnailBlobName());
            assertThat(thumbnailBytes)
                    .as("asset %s (id=%d) has no thumbnail stored", asset.getFileName(), asset.getAssetId())
                    .isNotNull();

            BufferedImage decoded = ImageIO.read(new ByteArrayInputStream(thumbnailBytes));
            Color actual = new Color(decoded.getRGB(decoded.getWidth() / 2, decoded.getHeight() / 2));

            assertThat(colorDistance(actual, expected))
                    .as("asset %s (id=%d, blob=%s) should be %s but its stored thumbnail decodes to %s"
                            + " — this means a different folder's file wrote this asset's thumbnail",
                            asset.getFileName(), asset.getAssetId(), asset.getThumbnailBlobName(), expected, actual)
                    .isLessThan(60);
        }
    }

    private static double colorDistance(Color a, Color b) {
        int dr = a.getRed() - b.getRed();
        int dg = a.getGreen() - b.getGreen();
        int db = a.getBlue() - b.getBlue();
        return Math.sqrt(dr * dr + dg * dg + db * db);
    }
}
