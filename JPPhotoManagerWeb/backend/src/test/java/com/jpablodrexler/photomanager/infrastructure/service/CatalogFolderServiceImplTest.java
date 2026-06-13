package com.jpablodrexler.photomanager.infrastructure.service;

import com.jpablodrexler.photomanager.application.dto.CatalogChangeNotification;
import com.jpablodrexler.photomanager.domain.enums.FileType;
import com.jpablodrexler.photomanager.domain.model.Asset;
import com.jpablodrexler.photomanager.domain.model.AssetAudio;
import com.jpablodrexler.photomanager.domain.model.AssetExif;
import com.jpablodrexler.photomanager.domain.model.AudioMetadata;
import com.jpablodrexler.photomanager.domain.model.Folder;
import com.jpablodrexler.photomanager.domain.enums.ImageRotation;
import com.jpablodrexler.photomanager.domain.enums.ReasonEnum;
import com.jpablodrexler.photomanager.domain.model.ExifMetadata;
import com.jpablodrexler.photomanager.domain.port.out.AssetAudioRepository;
import com.jpablodrexler.photomanager.domain.port.out.AssetExifRepository;
import com.jpablodrexler.photomanager.domain.port.out.AssetRepository;
import com.jpablodrexler.photomanager.domain.port.out.FolderRepository;
import com.jpablodrexler.photomanager.domain.port.out.StoragePort;
import com.jpablodrexler.photomanager.domain.port.out.ThumbnailPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CatalogFolderServiceImplTest {

    private static final Runnable NO_OP_HEARTBEAT = () -> {};

    @Mock
    AssetRepository assetRepository;

    @Mock
    AssetExifRepository assetExifRepository;

    @Mock
    AssetAudioRepository assetAudioRepository;

    @Mock
    FolderRepository folderRepository;

    @Mock
    StoragePort storageService;

    @Mock
    ThumbnailPort thumbnailStorageService;

    @Mock
    AudioMetadataService audioMetadataService;

    @InjectMocks
    CatalogFolderServiceImpl sut;

    @BeforeEach
    void setUp() {
        sut.batchSize = 1000;
    }

    @Test
    void catalogFolder_newFolder_savesFolderToRepository() {
        when(folderRepository.findByPath("/photos")).thenReturn(Optional.empty());
        when(folderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(storageService.listFiles("/photos")).thenReturn(List.of());
        when(assetRepository.findByFolder(any())).thenReturn(List.of());

        sut.catalogFolder("/photos", null, NO_OP_HEARTBEAT, new AtomicInteger(0), 1);

        verify(folderRepository).save(argThat(f -> f.getPath().equals("/photos")));
    }

    @Test
    void catalogFolder_newFolder_notifiesFolderCreatedCallback() {
        when(folderRepository.findByPath("/photos")).thenReturn(Optional.empty());
        when(folderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(storageService.listFiles("/photos")).thenReturn(List.of());
        when(assetRepository.findByFolder(any())).thenReturn(List.of());
        List<CatalogChangeNotification> notifications = new ArrayList<>();

        sut.catalogFolder("/photos", notifications::add, NO_OP_HEARTBEAT, new AtomicInteger(0), 1);

        assertThat(notifications).anyMatch(n -> n.getReason() == ReasonEnum.FOLDER_CREATED
                && "/photos".equals(n.getFolderPath()));
    }

    @Test
    void catalogFolder_existingFolder_doesNotSaveFolder() {
        Folder existing = buildFolder(1L, "/photos");
        when(folderRepository.findByPath("/photos")).thenReturn(Optional.of(existing));
        when(storageService.listFiles("/photos")).thenReturn(List.of());
        when(assetRepository.findByFolder(existing)).thenReturn(List.of());

        sut.catalogFolder("/photos", null, NO_OP_HEARTBEAT, new AtomicInteger(0), 1);

        verify(folderRepository, never()).save(any());
    }

    @Test
    void catalogFolder_newFile_createsAssetAndSavesToRepository() throws IOException {
        Folder folder = buildFolder(1L, "/photos");
        when(folderRepository.findByPath("/photos")).thenReturn(Optional.of(folder));
        when(storageService.listFiles("/photos")).thenReturn(List.of("/photos/new.jpg"));
        when(assetRepository.findByFolder(folder)).thenReturn(List.of());
        stubAssetCreationOk(folder, "/photos/new.jpg");

        sut.catalogFolder("/photos", null, NO_OP_HEARTBEAT, new AtomicInteger(0), 1);

        verify(assetRepository).save(argThat(a -> "new.jpg".equals(a.getFileName())));
    }

    @Test
    void catalogFolder_newFile_notifiesAssetCreatedCallback() throws IOException {
        Folder folder = buildFolder(1L, "/photos");
        when(folderRepository.findByPath("/photos")).thenReturn(Optional.of(folder));
        when(storageService.listFiles("/photos")).thenReturn(List.of("/photos/new.jpg"));
        when(assetRepository.findByFolder(folder)).thenReturn(List.of());
        stubAssetCreationOk(folder, "/photos/new.jpg");
        List<CatalogChangeNotification> notifications = new ArrayList<>();

        sut.catalogFolder("/photos", notifications::add, NO_OP_HEARTBEAT, new AtomicInteger(0), 1);

        assertThat(notifications).anyMatch(n -> n.getReason() == ReasonEnum.ASSET_CREATED);
    }

    @Test
    void catalogFolder_fileAlreadyCatalogued_doesNotCreateAsset() throws IOException {
        Folder folder = buildFolder(1L, "/photos");
        Asset existing = buildAsset(10L, "existing.jpg", folder);
        when(folderRepository.findByPath("/photos")).thenReturn(Optional.of(folder));
        when(storageService.listFiles("/photos")).thenReturn(List.of("/photos/existing.jpg"));
        when(assetRepository.findByFolder(folder)).thenReturn(List.of(existing));

        sut.catalogFolder("/photos", null, NO_OP_HEARTBEAT, new AtomicInteger(0), 1);

        verify(assetRepository, never()).save(any());
    }

    @Test
    void catalogFolder_staleAsset_deletesAssetFromRepository() {
        Folder folder = buildFolder(1L, "/photos");
        Asset stale = buildAsset(10L, "deleted.jpg", folder);
        when(folderRepository.findByPath("/photos")).thenReturn(Optional.of(folder));
        when(storageService.listFiles("/photos")).thenReturn(List.of());
        when(assetRepository.findByFolder(folder)).thenReturn(List.of(stale));

        sut.catalogFolder("/photos", null, NO_OP_HEARTBEAT, new AtomicInteger(0), 1);

        verify(assetRepository).deleteById(stale.getAssetId());
    }

    @Test
    void catalogFolder_staleAsset_deletesThumbnail() {
        Folder folder = buildFolder(1L, "/photos");
        Asset stale = buildAsset(10L, "deleted.jpg", folder);
        when(folderRepository.findByPath("/photos")).thenReturn(Optional.of(folder));
        when(storageService.listFiles("/photos")).thenReturn(List.of());
        when(assetRepository.findByFolder(folder)).thenReturn(List.of(stale));

        sut.catalogFolder("/photos", null, NO_OP_HEARTBEAT, new AtomicInteger(0), 1);

        verify(thumbnailStorageService).deleteThumbnail("10.bin");
    }

    @Test
    void catalogFolder_staleAsset_notifiesAssetDeletedCallback() {
        Folder folder = buildFolder(1L, "/photos");
        Asset stale = buildAsset(10L, "deleted.jpg", folder);
        when(folderRepository.findByPath("/photos")).thenReturn(Optional.of(folder));
        when(storageService.listFiles("/photos")).thenReturn(List.of());
        when(assetRepository.findByFolder(folder)).thenReturn(List.of(stale));
        List<CatalogChangeNotification> notifications = new ArrayList<>();

        sut.catalogFolder("/photos", notifications::add, NO_OP_HEARTBEAT, new AtomicInteger(0), 1);

        assertThat(notifications).anyMatch(n -> n.getReason() == ReasonEnum.ASSET_DELETED);
    }

    @Test
    void catalogFolder_nullCallback_doesNotThrow() {
        Folder folder = buildFolder(1L, "/photos");
        when(folderRepository.findByPath("/photos")).thenReturn(Optional.of(folder));
        when(storageService.listFiles("/photos")).thenReturn(List.of());
        when(assetRepository.findByFolder(folder)).thenReturn(List.of());

        assertThatCode(() -> sut.catalogFolder("/photos", null, NO_OP_HEARTBEAT, new AtomicInteger(0), 1))
                .doesNotThrowAnyException();
    }

    @Test
    void catalogFolder_incrementsProcessedCounterAfterCompletion() {
        Folder folder = buildFolder(1L, "/photos");
        when(folderRepository.findByPath("/photos")).thenReturn(Optional.of(folder));
        when(storageService.listFiles("/photos")).thenReturn(List.of());
        when(assetRepository.findByFolder(folder)).thenReturn(List.of());
        AtomicInteger counter = new AtomicInteger(0);

        sut.catalogFolder("/photos", null, NO_OP_HEARTBEAT, counter, 1);

        assertThat(counter.get()).isEqualTo(1);
    }

    @Test
    void catalogFolder_assetCreationFailure_doesNotThrow() throws IOException {
        Folder folder = buildFolder(1L, "/photos");
        when(folderRepository.findByPath("/photos")).thenReturn(Optional.of(folder));
        when(storageService.listFiles("/photos")).thenReturn(List.of("/photos/bad.jpg"));
        when(assetRepository.findByFolder(folder)).thenReturn(List.of());
        when(storageService.getFileSize("/photos/bad.jpg")).thenReturn(1000L);
        when(storageService.computeHash("/photos/bad.jpg")).thenThrow(new IOException("disk error"));

        assertThatCode(() -> sut.catalogFolder("/photos", null, NO_OP_HEARTBEAT, new AtomicInteger(0), 1))
                .doesNotThrowAnyException();
    }

    @Test
    void catalogFolder_assetCreationFailure_stillIncrementsProcessedCounter() throws IOException {
        Folder folder = buildFolder(1L, "/photos");
        when(folderRepository.findByPath("/photos")).thenReturn(Optional.of(folder));
        when(storageService.listFiles("/photos")).thenReturn(List.of("/photos/bad.jpg"));
        when(assetRepository.findByFolder(folder)).thenReturn(List.of());
        when(storageService.getFileSize("/photos/bad.jpg")).thenReturn(1000L);
        when(storageService.computeHash("/photos/bad.jpg")).thenThrow(new IOException("disk error"));
        AtomicInteger counter = new AtomicInteger(0);

        sut.catalogFolder("/photos", null, NO_OP_HEARTBEAT, counter, 1);

        assertThat(counter.get()).isEqualTo(1);
    }

    @Test
    void catalogFolder_zeroTotalFolders_notifiesAt100Percent() {
        when(folderRepository.findByPath("/photos")).thenReturn(Optional.empty());
        when(folderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(storageService.listFiles("/photos")).thenReturn(List.of());
        when(assetRepository.findByFolder(any())).thenReturn(List.of());
        List<CatalogChangeNotification> notifications = new ArrayList<>();

        sut.catalogFolder("/photos", notifications::add, NO_OP_HEARTBEAT, new AtomicInteger(0), 0);

        assertThat(notifications).allMatch(n -> n.getPercentCompleted() == 100);
    }

    @Test
    void catalogFolder_afterBatchSizeAssets_callsHeartbeat() throws IOException {
        Folder folder = buildFolder(1L, "/photos");
        when(folderRepository.findByPath("/photos")).thenReturn(Optional.of(folder));
        List<String> files = List.of("/photos/a.jpg", "/photos/b.jpg", "/photos/c.jpg");
        when(storageService.listFiles("/photos")).thenReturn(files);
        when(assetRepository.findByFolder(folder)).thenReturn(List.of());
        when(storageService.getFileSize(anyString())).thenReturn(2048L);
        when(storageService.computeHash(anyString())).thenReturn("abc123");
        when(storageService.getFileCreationDateTime(anyString())).thenReturn(LocalDateTime.of(2024, 1, 1, 0, 0));
        when(storageService.getFileModificationDateTime(anyString())).thenReturn(LocalDateTime.of(2024, 1, 2, 0, 0));
        when(storageService.getImageRotation(anyString())).thenReturn(ImageRotation.ROTATE_0);
        when(storageService.generateThumbnail(anyString(), anyInt(), anyInt())).thenReturn(new byte[]{1, 2, 3});
        when(assetRepository.save(any())).thenAnswer(inv -> {
            Asset a = inv.getArgument(0);
            a.setAssetId(99L);
            return a;
        });
        sut.batchSize = 2;
        AtomicInteger heartbeatCallCount = new AtomicInteger(0);
        Runnable heartbeat = heartbeatCallCount::incrementAndGet;

        sut.catalogFolder("/photos", null, heartbeat, new AtomicInteger(0), 1);

        assertThat(heartbeatCallCount.get()).isEqualTo(1);
    }

    @Test
    void createAsset_existingFolder_doesNotSaveFolder() throws IOException {
        Folder folder = buildFolder(1L, "/photos");
        when(folderRepository.findByPath("/photos")).thenReturn(Optional.of(folder));
        stubAssetCreationOk(folder, "/photos/photo.jpg");

        sut.createAsset("/photos", "photo.jpg");

        verify(folderRepository, never()).save(any());
    }

    @Test
    void createAsset_existingFolder_populatesAssetWithCorrectFileNameAndFolder() throws IOException {
        Folder folder = buildFolder(1L, "/photos");
        when(folderRepository.findByPath("/photos")).thenReturn(Optional.of(folder));
        stubAssetCreationOk(folder, "/photos/photo.jpg");

        sut.createAsset("/photos", "photo.jpg");

        ArgumentCaptor<Asset> captor = ArgumentCaptor.forClass(Asset.class);
        verify(assetRepository).save(captor.capture());
        assertThat(captor.getValue().getFileName()).isEqualTo("photo.jpg");
        assertThat(captor.getValue().getFolder()).isEqualTo(folder);
    }

    @Test
    void createAsset_newFolder_createsFolderThenPersistsAsset() throws IOException {
        Folder newFolder = buildFolder(2L, "/new-folder");
        when(folderRepository.findByPath("/new-folder")).thenReturn(Optional.empty());
        when(folderRepository.save(any())).thenReturn(newFolder);
        stubAssetCreationOk(newFolder, "/new-folder/photo.jpg");

        sut.createAsset("/new-folder", "photo.jpg");

        verify(folderRepository).save(argThat(f -> f.getPath().equals("/new-folder")));
        verify(assetRepository).save(any());
    }

    private void stubAssetCreationOk(Folder folder, String filePath) throws IOException {
        when(storageService.getFileSize(filePath)).thenReturn(2048L);
        when(storageService.computeHash(filePath)).thenReturn("abc123");
        when(storageService.getFileCreationDateTime(filePath)).thenReturn(LocalDateTime.of(2024, 1, 1, 0, 0));
        when(storageService.getFileModificationDateTime(filePath)).thenReturn(LocalDateTime.of(2024, 1, 2, 0, 0));
        when(storageService.getImageRotation(filePath)).thenReturn(ImageRotation.ROTATE_0);
        when(storageService.generateThumbnail(eq(filePath), anyInt(), anyInt())).thenReturn(new byte[]{1, 2, 3});
        when(storageService.getExifMetadata(filePath)).thenReturn(
                new ExifMetadata(null, null, null, null, null, null, null, null, null, null, null, null, null));
        when(assetExifRepository.findByAssetId(anyLong())).thenReturn(java.util.Optional.empty());
        when(assetExifRepository.save(any(AssetExif.class))).thenAnswer(inv -> inv.getArgument(0));
        when(assetRepository.save(any())).thenAnswer(inv -> {
            Asset a = inv.getArgument(0);
            a.setAssetId(99L);
            return a;
        });
    }

    @Test
    void catalogFolder_mp3File_routesThroughAudioMetadataService() throws IOException {
        Folder folder = buildFolder(1L, "/music");
        when(folderRepository.findByPath("/music")).thenReturn(Optional.of(folder));
        when(storageService.listFiles("/music")).thenReturn(List.of("/music/song.mp3"));
        when(assetRepository.findByFolder(folder)).thenReturn(List.of());
        stubAudioAssetCreationOk(folder, "/music/song.mp3");

        sut.catalogFolder("/music", null, NO_OP_HEARTBEAT, new AtomicInteger(0), 1);

        ArgumentCaptor<Asset> captor = ArgumentCaptor.forClass(Asset.class);
        verify(assetRepository).save(captor.capture());
        assertThat(captor.getValue().getFileType()).isEqualTo(FileType.AUDIO);
        verify(audioMetadataService).extractAlbumArt(any());
        verify(audioMetadataService).extract(any());
        verify(assetAudioRepository).save(any(AssetAudio.class));
    }

    private void stubAudioAssetCreationOk(Folder folder, String filePath) throws IOException {
        when(storageService.getFileSize(filePath)).thenReturn(4096L);
        when(storageService.computeHash(filePath)).thenReturn("audiohash");
        when(storageService.getFileCreationDateTime(filePath)).thenReturn(LocalDateTime.of(2024, 1, 1, 0, 0));
        when(storageService.getFileModificationDateTime(filePath)).thenReturn(LocalDateTime.of(2024, 1, 2, 0, 0));
        when(audioMetadataService.extractAlbumArt(any())).thenReturn(java.util.Optional.empty());
        when(audioMetadataService.extract(any())).thenReturn(
                new AudioMetadata("Song", "Artist", "Album", 240, 320, 44100));
        when(assetAudioRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(assetRepository.save(any())).thenAnswer(inv -> {
            Asset a = inv.getArgument(0);
            a.setAssetId(42L);
            return a;
        });
    }

    private Folder buildFolder(Long id, String path) {
        Folder folder = new Folder();
        folder.setFolderId(id);
        folder.setPath(path);
        return folder;
    }

    private Asset buildAsset(Long id, String fileName, Folder folder) {
        Asset asset = new Asset();
        asset.setAssetId(id);
        asset.setFileName(fileName);
        asset.setFolder(folder);
        return asset;
    }
}
