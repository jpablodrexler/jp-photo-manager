package com.jpablodrexler.photomanager.infrastructure.service;

import com.jpablodrexler.photomanager.domain.enums.FileType;
import com.jpablodrexler.photomanager.domain.model.Asset;
import com.jpablodrexler.photomanager.domain.model.AssetAudio;
import com.jpablodrexler.photomanager.domain.model.AssetExif;
import com.jpablodrexler.photomanager.domain.model.AudioMetadata;
import com.jpablodrexler.photomanager.domain.model.Folder;
import com.jpablodrexler.photomanager.domain.enums.ImageRotation;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CatalogFolderServiceAdapterTest {

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
    CatalogFolderServiceAdapter sut;

    @BeforeEach
    void setUp() {
        sut.batchSize = 1000;
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

    @Test
    void createAsset_mp3File_routesThroughAudioMetadataService() throws IOException {
        Folder folder = buildFolder(1L, "/music");
        when(folderRepository.findByPath("/music")).thenReturn(Optional.of(folder));
        when(storageService.isAudioFile("song.mp3")).thenReturn(true);
        stubAudioAssetCreationOk(folder, "/music/song.mp3");

        sut.createAsset("/music", "song.mp3");

        ArgumentCaptor<Asset> captor = ArgumentCaptor.forClass(Asset.class);
        verify(assetRepository).save(captor.capture());
        assertThat(captor.getValue().getFileType()).isEqualTo(FileType.AUDIO);
        verify(audioMetadataService).extractAlbumArt(any());
        verify(audioMetadataService).extract(any());
        verify(assetAudioRepository).save(any(AssetAudio.class));
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
}
