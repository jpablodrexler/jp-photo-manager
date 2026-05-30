package com.jpablodrexler.photomanager.infrastructure.service;

import com.jpablodrexler.photomanager.domain.enums.FileType;
import com.jpablodrexler.photomanager.domain.enums.ImageRotation;
import com.jpablodrexler.photomanager.domain.model.Asset;
import com.jpablodrexler.photomanager.domain.model.AssetExif;
import com.jpablodrexler.photomanager.domain.model.ExifMetadata;
import com.jpablodrexler.photomanager.domain.model.Folder;
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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CatalogFolderServiceVideoTest {

    @Mock AssetRepository assetRepository;
    @Mock AssetExifRepository assetExifRepository;
    @Mock AssetAudioRepository assetAudioRepository;
    @Mock FolderRepository folderRepository;
    @Mock StoragePort storageService;
    @Mock ThumbnailPort thumbnailStorageService;
    @Mock AudioMetadataService audioMetadataService;

    @InjectMocks
    CatalogFolderServiceImpl sut;

    @BeforeEach
    void setUp() {
        sut.batchSize = 1000;
    }

    @Test
    void createAsset_videoFile_setsIsVideoTrue() throws IOException {
        Folder folder = buildFolder(1L, "/videos");
        when(folderRepository.findByPath("/videos")).thenReturn(Optional.of(folder));
        when(storageService.isVideoFile("clip.mp4")).thenReturn(true);
        stubVideoAssetCreationOk("/videos/clip.mp4");

        Asset result = sut.createAsset("/videos", "clip.mp4");

        assertThat(result.isVideo()).isTrue();
    }

    @Test
    void createAsset_videoFile_setsFileTypeToVideo() throws IOException {
        Folder folder = buildFolder(1L, "/videos");
        when(folderRepository.findByPath("/videos")).thenReturn(Optional.of(folder));
        when(storageService.isVideoFile("clip.mp4")).thenReturn(true);
        stubVideoAssetCreationOk("/videos/clip.mp4");

        Asset result = sut.createAsset("/videos", "clip.mp4");

        assertThat(result.getFileType()).isEqualTo(FileType.VIDEO);
    }

    @Test
    void createAsset_imageFile_setsIsVideoFalse() throws IOException {
        Folder folder = buildFolder(1L, "/photos");
        when(folderRepository.findByPath("/photos")).thenReturn(Optional.of(folder));
        when(storageService.isVideoFile("photo.jpg")).thenReturn(false);
        stubImageAssetCreationOk("/photos/photo.jpg");

        Asset result = sut.createAsset("/photos", "photo.jpg");

        assertThat(result.isVideo()).isFalse();
    }

    private void stubVideoAssetCreationOk(String filePath) throws IOException {
        when(storageService.getFileSize(filePath)).thenReturn(10_000_000L);
        when(storageService.computeHash(filePath)).thenReturn("videohash");
        when(storageService.getFileCreationDateTime(filePath)).thenReturn(LocalDateTime.of(2024, 1, 1, 0, 0));
        when(storageService.getFileModificationDateTime(filePath)).thenReturn(LocalDateTime.of(2024, 1, 2, 0, 0));
        when(storageService.getImageRotation(filePath)).thenReturn(ImageRotation.ROTATE_0);
        when(storageService.generateThumbnail(eq(filePath), anyInt(), anyInt())).thenReturn(new byte[]{1, 2, 3});
        when(storageService.getExifMetadata(filePath)).thenReturn(
                new ExifMetadata(null, null, null, null, null, null, null, null, null, null, null, null));
        when(assetExifRepository.findByAssetId(anyLong())).thenReturn(Optional.empty());
        when(assetExifRepository.save(any(AssetExif.class))).thenAnswer(inv -> inv.getArgument(0));
        when(assetRepository.save(any())).thenAnswer(inv -> {
            Asset a = inv.getArgument(0);
            a.setAssetId(99L);
            return a;
        });
    }

    private void stubImageAssetCreationOk(String filePath) throws IOException {
        when(storageService.getFileSize(filePath)).thenReturn(2048L);
        when(storageService.computeHash(filePath)).thenReturn("abc123");
        when(storageService.getFileCreationDateTime(filePath)).thenReturn(LocalDateTime.of(2024, 1, 1, 0, 0));
        when(storageService.getFileModificationDateTime(filePath)).thenReturn(LocalDateTime.of(2024, 1, 2, 0, 0));
        when(storageService.getImageRotation(filePath)).thenReturn(ImageRotation.ROTATE_0);
        when(storageService.generateThumbnail(eq(filePath), anyInt(), anyInt())).thenReturn(new byte[]{1, 2, 3});
        when(storageService.getExifMetadata(filePath)).thenReturn(
                new ExifMetadata(null, null, null, null, null, null, null, null, null, null, null, null));
        when(assetExifRepository.findByAssetId(anyLong())).thenReturn(Optional.empty());
        when(assetExifRepository.save(any(AssetExif.class))).thenAnswer(inv -> inv.getArgument(0));
        when(assetRepository.save(any())).thenAnswer(inv -> {
            Asset a = inv.getArgument(0);
            a.setAssetId(99L);
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
