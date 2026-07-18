package com.jpablodrexler.photomanager.application.usecase.asset;

import com.jpablodrexler.photomanager.PostgresIntegrationTest;
import com.jpablodrexler.photomanager.domain.enums.ProcessingStatus;
import com.jpablodrexler.photomanager.domain.model.Asset;
import com.jpablodrexler.photomanager.domain.model.Folder;
import com.jpablodrexler.photomanager.domain.port.in.asset.ReprocessAssetUseCase;
import com.jpablodrexler.photomanager.domain.port.in.asset.UploadAssetUseCase;
import com.jpablodrexler.photomanager.domain.port.out.AssetExifRepository;
import com.jpablodrexler.photomanager.domain.port.out.AssetRepository;
import com.jpablodrexler.photomanager.domain.port.out.FolderRepository;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.kafka.test.context.EmbeddedKafka;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end verification of the kafka-async-upload pipeline: upload persists a PENDING placeholder,
 * publishes AssetUploadedEvent, and the three independent consumer groups (asset-hash-processor /
 * asset-exif-processor / asset-thumbnail-processor) each populate their stage, converging on
 * processing_status = COMPLETED.
 */
@EmbeddedKafka(partitions = 1, topics = {
    "job.catalog.progress", "job.sync.progress", "job.convert.progress",
    "asset.cataloged", "asset.deleted", "asset.uploaded", "job.upload.progress"
})
@WithMockUser(roles = "ADMIN")
class UploadKafkaPipelineIntegrationTest extends PostgresIntegrationTest {

    @TempDir
    static Path tempDir;

    static Path catalogFolder;

    @Autowired
    UploadAssetUseCase uploadAssetUseCase;

    @Autowired
    ReprocessAssetUseCase reprocessAssetUseCase;

    @Autowired
    FolderRepository folderRepository;

    @Autowired
    AssetRepository assetRepository;

    @Autowired
    AssetExifRepository assetExifRepository;

    @BeforeAll
    static void setUp() throws Exception {
        catalogFolder = tempDir.resolve("upload-kafka-test");
        Files.createDirectories(catalogFolder);
        Files.createDirectories(Path.of("/tmp/thumbnails"));
    }

    @Test
    void uploadAsset_endToEnd_reachesCompletedWithHashExifAndThumbnail() throws Exception {
        Path testFolder = catalogFolder.resolve("end-to-end");
        Files.createDirectories(testFolder);
        Folder folder = new Folder();
        folder.setPath(testFolder.toString());
        folder = folderRepository.save(folder);

        byte[] jpegBytes;
        try (InputStream in = UploadKafkaPipelineIntegrationTest.class.getClassLoader()
                .getResourceAsStream("fixtures/test-with-exif.jpg")) {
            assertThat(in).as("JPEG fixture not found").isNotNull();
            jpegBytes = in.readAllBytes();
        }

        Asset placeholder = uploadAssetUseCase.execute(testFolder.toString(), "uploaded.jpg", "image/jpeg", jpegBytes);
        assertThat(placeholder.getProcessingStatus()).isEqualTo(ProcessingStatus.PENDING);
        assertThat(placeholder.getHash()).isNull();

        Asset completed = pollUntilCompleted(placeholder.getAssetId(), Duration.ofSeconds(15));

        assertThat(completed.getProcessingStatus()).isEqualTo(ProcessingStatus.COMPLETED);
        assertThat(completed.getHash()).isNotBlank();
        assertThat(completed.getThumbnailCreationDateTime()).isNotNull();
        assertThat(completed.getHashCompletedAt()).isNotNull();
        assertThat(completed.getExifCompletedAt()).isNotNull();
        assertThat(completed.getThumbnailCompletedAt()).isNotNull();
        assertThat(assetExifRepository.findByAssetId(completed.getAssetId())).isPresent();
    }

    @Test
    void reprocessAsset_afterCompletion_remainsCompletedWithConsistentData() throws Exception {
        Path testFolder = catalogFolder.resolve("reprocess");
        Files.createDirectories(testFolder);
        Folder folder = new Folder();
        folder.setPath(testFolder.toString());
        folder = folderRepository.save(folder);

        byte[] jpegBytes;
        try (InputStream in = UploadKafkaPipelineIntegrationTest.class.getClassLoader()
                .getResourceAsStream("fixtures/test-with-exif.jpg")) {
            jpegBytes = in.readAllBytes();
        }

        Asset placeholder = uploadAssetUseCase.execute(testFolder.toString(), "reprocess-me.jpg", "image/jpeg", jpegBytes);
        Asset completed = pollUntilCompleted(placeholder.getAssetId(), Duration.ofSeconds(15));
        String originalHash = completed.getHash();

        reprocessAssetUseCase.execute(completed.getAssetId());

        Asset reprocessed = pollUntilCompleted(completed.getAssetId(), Duration.ofSeconds(15));
        assertThat(reprocessed.getProcessingStatus()).isEqualTo(ProcessingStatus.COMPLETED);
        assertThat(reprocessed.getHash()).isEqualTo(originalHash);
    }

    private Asset pollUntilCompleted(Long assetId, Duration timeout) throws InterruptedException {
        Instant deadline = Instant.now().plus(timeout);
        while (Instant.now().isBefore(deadline)) {
            Optional<Asset> asset = assetRepository.findById(assetId);
            if (asset.isPresent() && asset.get().getProcessingStatus() == ProcessingStatus.COMPLETED) {
                return asset.get();
            }
            Thread.sleep(200);
        }
        throw new AssertionError("Asset " + assetId + " did not reach COMPLETED within " + timeout);
    }
}
