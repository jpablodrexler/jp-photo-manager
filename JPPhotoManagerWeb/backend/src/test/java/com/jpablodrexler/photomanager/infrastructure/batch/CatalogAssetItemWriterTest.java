package com.jpablodrexler.photomanager.infrastructure.batch;

import com.jpablodrexler.photomanager.domain.model.Asset;
import com.jpablodrexler.photomanager.domain.model.AssetAudio;
import com.jpablodrexler.photomanager.domain.model.AssetExif;
import com.jpablodrexler.photomanager.domain.model.Folder;
import com.jpablodrexler.photomanager.domain.port.out.AssetAudioRepository;
import com.jpablodrexler.photomanager.domain.port.out.AssetExifRepository;
import com.jpablodrexler.photomanager.domain.port.out.AssetRepository;
import com.jpablodrexler.photomanager.domain.port.out.FolderRepository;
import com.jpablodrexler.photomanager.domain.port.out.StoragePort;
import com.jpablodrexler.photomanager.domain.port.out.ThumbnailPort;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.item.Chunk;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CatalogAssetItemWriterTest {

    @Mock AssetRepository assetRepository;
    @Mock AssetExifRepository assetExifRepository;
    @Mock AssetAudioRepository assetAudioRepository;
    @Mock FolderRepository folderRepository;
    @Mock StoragePort storagePort;
    @Mock ThumbnailPort thumbnailPort;
    @Mock KafkaTemplate<String, Object> kafkaTemplate;

    SimpleMeterRegistry meterRegistry;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
    }

    private CatalogAssetItemWriter newWriter(String userId, String folderPath) {
        return new CatalogAssetItemWriter(1L, userId, folderPath, assetRepository, assetExifRepository,
                assetAudioRepository, folderRepository, storagePort, thumbnailPort, kafkaTemplate, meterRegistry);
    }

    @Test
    void write_folderAlreadyCatalogued_savesAssetAndThumbnail() throws Exception {
        Folder folder = buildFolder(1L, "/photos");
        when(folderRepository.findByPath("/photos")).thenReturn(Optional.of(folder));
        Asset asset = new Asset();
        asset.setFileName("a.jpg");
        when(assetRepository.save(any(Asset.class))).thenAnswer(inv -> {
            Asset a = inv.getArgument(0);
            a.setAssetId(10L);
            return a;
        });
        CatalogBatchItem item = new CatalogBatchItem(asset, new byte[]{1}, null, null);

        CatalogAssetItemWriter sut = newWriter(null, "/photos");
        sut.write(new Chunk<>(List.of(item)));

        verify(folderRepository, never()).save(any());
        verify(thumbnailPort).saveThumbnail("10.bin", new byte[]{1});
        verify(kafkaTemplate).send(eq("job.catalog.progress"), eq("1"), any());
        verify(kafkaTemplate).send(eq("asset.cataloged"), eq("10"), any());
    }

    @Test
    void write_folderNotCatalogued_createsFolderAndNotifies() throws Exception {
        Folder newFolder = buildFolder(2L, "/new");
        when(folderRepository.findByPath("/new")).thenReturn(Optional.empty());
        when(folderRepository.save(any())).thenReturn(newFolder);
        Asset asset = new Asset();
        asset.setFileName("b.jpg");
        when(assetRepository.save(any(Asset.class))).thenAnswer(inv -> {
            Asset a = inv.getArgument(0);
            a.setAssetId(11L);
            return a;
        });
        CatalogBatchItem item = new CatalogBatchItem(asset, new byte[]{2}, null, null);

        CatalogAssetItemWriter sut = newWriter(null, "/new");
        sut.write(new Chunk<>(List.of(item)));

        verify(folderRepository).save(any());
        verify(kafkaTemplate, times(2)).send(eq("job.catalog.progress"), eq("1"), any());
    }

    @Test
    void write_secondChunk_reusesCachedFolder() throws Exception {
        Folder folder = buildFolder(1L, "/photos");
        when(folderRepository.findByPath("/photos")).thenReturn(Optional.of(folder));
        Asset asset1 = new Asset();
        asset1.setFileName("a.jpg");
        Asset asset2 = new Asset();
        asset2.setFileName("b.jpg");
        when(assetRepository.save(any(Asset.class))).thenAnswer(inv -> inv.getArgument(0));

        CatalogAssetItemWriter sut = newWriter(null, "/photos");
        sut.write(new Chunk<>(List.of(new CatalogBatchItem(asset1, new byte[]{1}, null, null))));
        sut.write(new Chunk<>(List.of(new CatalogBatchItem(asset2, new byte[]{2}, null, null))));

        verify(folderRepository, times(1)).findByPath("/photos");
    }

    @Test
    void write_itemWithExif_savesExifWithAssetId() throws Exception {
        Folder folder = buildFolder(1L, "/photos");
        when(folderRepository.findByPath("/photos")).thenReturn(Optional.of(folder));
        Asset asset = new Asset();
        asset.setFileName("a.jpg");
        when(assetRepository.save(any(Asset.class))).thenAnswer(inv -> {
            Asset a = inv.getArgument(0);
            a.setAssetId(20L);
            return a;
        });
        AssetExif exif = new AssetExif();
        CatalogBatchItem item = new CatalogBatchItem(asset, new byte[]{1}, exif, null);

        CatalogAssetItemWriter sut = newWriter(null, "/photos");
        sut.write(new Chunk<>(List.of(item)));

        verify(assetExifRepository).save(exif);
        assertThat(exif.getAssetId()).isEqualTo(20L);
    }

    @Test
    void write_itemWithoutExif_neverSavesExif() throws Exception {
        Folder folder = buildFolder(1L, "/photos");
        when(folderRepository.findByPath("/photos")).thenReturn(Optional.of(folder));
        Asset asset = new Asset();
        asset.setFileName("a.jpg");
        when(assetRepository.save(any(Asset.class))).thenAnswer(inv -> inv.getArgument(0));
        CatalogBatchItem item = new CatalogBatchItem(asset, new byte[]{1}, null, null);

        CatalogAssetItemWriter sut = newWriter(null, "/photos");
        sut.write(new Chunk<>(List.of(item)));

        verify(assetExifRepository, never()).save(any());
    }

    @Test
    void write_itemWithAudio_savesAudioWithAssetId() throws Exception {
        Folder folder = buildFolder(1L, "/music");
        when(folderRepository.findByPath("/music")).thenReturn(Optional.of(folder));
        Asset asset = new Asset();
        asset.setFileName("song.mp3");
        when(assetRepository.save(any(Asset.class))).thenAnswer(inv -> {
            Asset a = inv.getArgument(0);
            a.setAssetId(30L);
            return a;
        });
        AssetAudio audio = AssetAudio.builder().title("Song").build();
        CatalogBatchItem item = new CatalogBatchItem(asset, new byte[]{1}, null, audio);

        CatalogAssetItemWriter sut = newWriter(null, "/music");
        sut.write(new Chunk<>(List.of(item)));

        verify(assetAudioRepository).save(audio);
        assertThat(audio.getAssetId()).isEqualTo(30L);
    }

    @Test
    void write_kafkaSendThrows_doesNotPropagate() throws Exception {
        Folder folder = buildFolder(1L, "/photos");
        when(folderRepository.findByPath("/photos")).thenReturn(Optional.of(folder));
        Asset asset = new Asset();
        asset.setFileName("a.jpg");
        when(assetRepository.save(any(Asset.class))).thenAnswer(inv -> inv.getArgument(0));
        CatalogBatchItem item = new CatalogBatchItem(asset, new byte[]{1}, null, null);
        doThrow(new RuntimeException("kafka down")).when(kafkaTemplate).send(anyString(), anyString(), any());

        CatalogAssetItemWriter sut = newWriter(null, "/photos");

        assertThatCode(() -> sut.write(new Chunk<>(List.of(item)))).doesNotThrowAnyException();
    }

    @Test
    void constructor_validUserId_parsesToUuid() throws Exception {
        UUID userId = UUID.randomUUID();
        Folder folder = buildFolder(1L, "/photos");
        when(folderRepository.findByPath("/photos")).thenReturn(Optional.of(folder));
        Asset asset = new Asset();
        asset.setFileName("a.jpg");
        when(assetRepository.save(any(Asset.class))).thenAnswer(inv -> inv.getArgument(0));
        CatalogBatchItem item = new CatalogBatchItem(asset, new byte[]{1}, null, null);

        CatalogAssetItemWriter sut = newWriter(userId.toString(), "/photos");

        assertThatCode(() -> sut.write(new Chunk<>(List.of(item)))).doesNotThrowAnyException();
    }

    @Test
    void afterStep_staleAssetNotOnDisk_deletedAndNotified() {
        Folder folder = buildFolder(1L, "/photos");
        when(folderRepository.findByPath("/photos")).thenReturn(Optional.of(folder));
        when(storagePort.listFiles("/photos")).thenReturn(List.of("keep.jpg"));

        Asset stale = new Asset();
        stale.setAssetId(5L);
        stale.setFileName("stale.jpg");
        Asset kept = new Asset();
        kept.setAssetId(6L);
        kept.setFileName("keep.jpg");
        when(assetRepository.findByFolder(folder)).thenReturn(List.of(stale, kept));

        CatalogAssetItemWriter sut = newWriter(null, "/photos");
        StepExecution stepExecution = buildStepExecution();

        ExitStatus result = sut.afterStep(stepExecution);

        verify(assetRepository).deleteById(5L);
        verify(assetRepository, never()).deleteById(6L);
        verify(thumbnailPort).deleteThumbnail("5.bin");
        verify(kafkaTemplate).send(eq("asset.deleted"), eq("5"), any());
        assertThat(result).isEqualTo(ExitStatus.COMPLETED);
    }

    @Test
    void afterStep_folderMissing_createsFolderWithoutDeletingAnything() {
        Folder newFolder = buildFolder(9L, "/new");
        when(folderRepository.findByPath("/new")).thenReturn(Optional.empty());
        when(folderRepository.save(any())).thenReturn(newFolder);
        when(storagePort.listFiles("/new")).thenReturn(List.of());
        when(assetRepository.findByFolder(newFolder)).thenReturn(List.of());

        CatalogAssetItemWriter sut = newWriter(null, "/new");
        sut.afterStep(buildStepExecution());

        verify(assetRepository, never()).deleteById(any());
    }

    @Test
    void afterStep_exceptionDuringCleanup_logsAndReturnsExitStatus() {
        when(folderRepository.findByPath("/broken")).thenThrow(new RuntimeException("db down"));

        CatalogAssetItemWriter sut = newWriter(null, "/broken");
        StepExecution stepExecution = buildStepExecution();

        ExitStatus result = sut.afterStep(stepExecution);

        assertThat(result).isEqualTo(ExitStatus.COMPLETED);
    }

    private StepExecution buildStepExecution() {
        JobExecution jobExecution = new JobExecution(1L, new JobParameters());
        StepExecution stepExecution = new StepExecution("catalogWorkerStep0", jobExecution);
        stepExecution.setExitStatus(ExitStatus.COMPLETED);
        return stepExecution;
    }

    private Folder buildFolder(Long id, String path) {
        Folder folder = new Folder();
        folder.setFolderId(id);
        folder.setPath(path);
        return folder;
    }
}
