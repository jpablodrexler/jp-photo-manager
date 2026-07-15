package com.jpablodrexler.photomanager.application.usecase.asset;

import com.jpablodrexler.photomanager.domain.model.AssetFilter;
import com.jpablodrexler.photomanager.domain.model.AssetImage;
import com.jpablodrexler.photomanager.domain.model.AssetStreamInfo;
import com.jpablodrexler.photomanager.domain.model.PaginatedResult;
import com.jpablodrexler.photomanager.domain.model.Asset;
import com.jpablodrexler.photomanager.domain.model.AssetExif;
import com.jpablodrexler.photomanager.domain.model.Folder;
import com.jpablodrexler.photomanager.domain.port.out.AssetExifRepository;
import com.jpablodrexler.photomanager.domain.port.out.AssetRepository;
import com.jpablodrexler.photomanager.domain.port.out.AuditLogRepository;
import com.jpablodrexler.photomanager.domain.port.out.StoragePort;
import com.jpablodrexler.photomanager.domain.port.out.ThumbnailPort;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SimpleAssetUseCasesTest {

    @Nested
    @ExtendWith(MockitoExtension.class)
    class GetAssetsUseCaseImplTest {

        @Mock AssetRepository assetRepository;
        @InjectMocks GetAssetsUseCaseImpl sut;

        @Test
        void execute_delegatesToRepository() {
            AssetFilter filter = new AssetFilter(1L, null, null, null, null, null, 0, 20, false, null);
            PaginatedResult<Asset> expected = new PaginatedResult<>(List.of(), 0L, 0, 20);
            when(assetRepository.findFiltered(filter)).thenReturn(expected);

            PaginatedResult<Asset> result = sut.execute(filter);

            assertThat(result).isEqualTo(expected);
            verify(assetRepository).findFiltered(filter);
        }
    }

    @Nested
    @ExtendWith(MockitoExtension.class)
    class GetAssetExifUseCaseImplTest {

        @Mock AssetRepository assetRepository;
        @Mock AssetExifRepository assetExifRepository;
        @InjectMocks GetAssetExifUseCaseImpl sut;

        @Test
        void execute_assetFound_exifFound_returnsExif() {
            Long assetId = 1L;
            Asset asset = buildAsset(assetId);
            AssetExif exif = AssetExif.builder().assetId(assetId).cameraMake("Canon").build();
            when(assetRepository.findById(assetId)).thenReturn(Optional.of(asset));
            when(assetExifRepository.findByAssetId(assetId)).thenReturn(Optional.of(exif));

            AssetExif result = sut.execute(assetId);

            assertThat(result).isEqualTo(exif);
        }

        @Test
        void execute_assetFound_exifNotFound_returnsNull() {
            Long assetId = 2L;
            when(assetRepository.findById(assetId)).thenReturn(Optional.of(buildAsset(assetId)));
            when(assetExifRepository.findByAssetId(assetId)).thenReturn(Optional.empty());

            AssetExif result = sut.execute(assetId);

            assertThat(result).isNull();
        }

        @Test
        void execute_assetNotFound_throwsNoSuchElementException() {
            when(assetRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> sut.execute(99L))
                    .isInstanceOf(NoSuchElementException.class);
        }
    }

    @Nested
    @ExtendWith(MockitoExtension.class)
    class GetAssetImageUseCaseImplTest {

        @Mock AssetRepository assetRepository;
        @Mock StoragePort storagePort;
        @Mock AuditLogRepository auditLogRepository;
        @InjectMocks GetAssetImageUseCaseImpl sut;

        @Test
        void execute_assetFound_returnsAssetImage() throws IOException {
            Long assetId = 1L;
            Folder folder = Folder.builder().path("/photos").build();
            Asset asset = Asset.builder().assetId(assetId).folder(folder).fileName("img.jpg").build();
            byte[] bytes = new byte[]{1, 2, 3};
            when(assetRepository.findById(assetId)).thenReturn(Optional.of(asset));
            when(storagePort.readFileBytes("/photos/img.jpg")).thenReturn(bytes);

            AssetImage result = sut.execute(assetId, null);

            assertThat(result.bytes()).isEqualTo(bytes);
            assertThat(result.fileName()).isEqualTo("img.jpg");
        }

        @Test
        void execute_jpegMagicBytes_returnsJpegMimeType() throws IOException {
            Long assetId = 1L;
            Folder folder = Folder.builder().path("/photos").build();
            Asset asset = Asset.builder().assetId(assetId).folder(folder).fileName("img.jpg").build();
            when(assetRepository.findById(assetId)).thenReturn(Optional.of(asset));
            when(storagePort.readFileBytes("/photos/img.jpg"))
                    .thenReturn(new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF});

            AssetImage result = sut.execute(assetId, null);

            assertThat(result.mimeType()).isEqualTo("image/jpeg");
        }

        @Test
        void execute_pngMagicBytes_returnsPngMimeType() throws IOException {
            Long assetId = 1L;
            Folder folder = Folder.builder().path("/photos").build();
            Asset asset = Asset.builder().assetId(assetId).folder(folder).fileName("img.png").build();
            when(assetRepository.findById(assetId)).thenReturn(Optional.of(asset));
            when(storagePort.readFileBytes("/photos/img.png"))
                    .thenReturn(new byte[]{(byte) 0x89, 'P', 'N', 'G', '\r', '\n', (byte) 0x1A, '\n'});

            AssetImage result = sut.execute(assetId, null);

            assertThat(result.mimeType()).isEqualTo("image/png");
        }

        @Test
        void execute_gifMagicBytes_returnsGifMimeType() throws IOException {
            Long assetId = 1L;
            Folder folder = Folder.builder().path("/photos").build();
            Asset asset = Asset.builder().assetId(assetId).folder(folder).fileName("anim.gif").build();
            when(assetRepository.findById(assetId)).thenReturn(Optional.of(asset));
            when(storagePort.readFileBytes("/photos/anim.gif"))
                    .thenReturn(new byte[]{'G', 'I', 'F', '8', '9', 'a'});

            AssetImage result = sut.execute(assetId, null);

            assertThat(result.mimeType()).isEqualTo("image/gif");
        }

        @Test
        void execute_unknownMagicBytes_returnsNullMimeType() throws IOException {
            Long assetId = 1L;
            Folder folder = Folder.builder().path("/photos").build();
            Asset asset = Asset.builder().assetId(assetId).folder(folder).fileName("disguised.jpg").build();
            when(assetRepository.findById(assetId)).thenReturn(Optional.of(asset));
            when(storagePort.readFileBytes("/photos/disguised.jpg")).thenReturn(new byte[]{1, 2, 3});

            AssetImage result = sut.execute(assetId, null);

            assertThat(result.mimeType()).isNull();
        }

        @Test
        void execute_assetFound_logsAssetViewedAuditEvent() throws IOException {
            Long assetId = 1L;
            Folder folder = Folder.builder().path("/photos").build();
            Asset asset = Asset.builder().assetId(assetId).folder(folder).fileName("img.jpg").build();
            when(assetRepository.findById(assetId)).thenReturn(Optional.of(asset));
            when(storagePort.readFileBytes("/photos/img.jpg")).thenReturn(new byte[]{1});

            sut.execute(assetId, null);

            verify(auditLogRepository).log(any());
        }

        @Test
        void execute_auditLogThrows_doesNotPropagate() throws IOException {
            Long assetId = 1L;
            Folder folder = Folder.builder().path("/photos").build();
            Asset asset = Asset.builder().assetId(assetId).folder(folder).fileName("img.jpg").build();
            when(assetRepository.findById(assetId)).thenReturn(Optional.of(asset));
            when(storagePort.readFileBytes("/photos/img.jpg")).thenReturn(new byte[]{1});
            doThrow(new RuntimeException("mongo down")).when(auditLogRepository).log(any());

            assertThatCode(() -> sut.execute(assetId, null)).doesNotThrowAnyException();
        }

        @Test
        void execute_assetNotFound_throwsNoSuchElementException() {
            when(assetRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> sut.execute(99L, null))
                    .isInstanceOf(NoSuchElementException.class);
        }
    }

    @Nested
    @ExtendWith(MockitoExtension.class)
    class GetAssetThumbnailUseCaseImplTest {

        @Mock ThumbnailPort thumbnailPort;
        @InjectMocks GetAssetThumbnailUseCaseImpl sut;

        @Test
        void execute_thumbnailExists_returnsBytes() {
            byte[] bytes = new byte[]{1, 2, 3};
            when(thumbnailPort.loadThumbnail("42.bin")).thenReturn(bytes);

            byte[] result = sut.execute(42L);

            assertThat(result).isEqualTo(bytes);
        }

        @Test
        void execute_thumbnailMissing_returnsNull() {
            when(thumbnailPort.loadThumbnail("99.bin")).thenReturn(null);

            byte[] result = sut.execute(99L);

            assertThat(result).isNull();
        }
    }

    @Nested
    @ExtendWith(MockitoExtension.class)
    class StreamAssetUseCaseImplTest {

        @Mock AssetRepository assetRepository;
        @Mock StoragePort storagePort;
        @InjectMocks StreamAssetUseCaseImpl sut;

        @Test
        void execute_assetAndFileExist_returnsStreamInfoWithFileSize() {
            Long assetId = 1L;
            Folder folder = Folder.builder().path("/media").build();
            Asset asset = Asset.builder().assetId(assetId).folder(folder).fileName("clip.mp4").build();
            when(assetRepository.findById(assetId)).thenReturn(Optional.of(asset));
            when(storagePort.fileExists("/media/clip.mp4")).thenReturn(true);
            when(storagePort.getFileSize("/media/clip.mp4")).thenReturn(2048L);

            AssetStreamInfo result = sut.execute(assetId);

            assertThat(result.asset()).isEqualTo(asset);
            assertThat(result.fileSize()).isEqualTo(2048L);
        }

        @Test
        void execute_assetNotFound_throwsNoSuchElementException() {
            when(assetRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> sut.execute(99L))
                    .isInstanceOf(NoSuchElementException.class);
        }

        @Test
        void execute_fileMissingOnDisk_throwsNoSuchElementException() {
            Long assetId = 1L;
            Folder folder = Folder.builder().path("/media").build();
            Asset asset = Asset.builder().assetId(assetId).folder(folder).fileName("clip.mp4").build();
            when(assetRepository.findById(assetId)).thenReturn(Optional.of(asset));
            when(storagePort.fileExists("/media/clip.mp4")).thenReturn(false);

            assertThatThrownBy(() -> sut.execute(assetId))
                    .isInstanceOf(NoSuchElementException.class);
        }
    }

    @Nested
    @ExtendWith(MockitoExtension.class)
    class RateAssetUseCaseImplTest {

        @Mock AssetRepository assetRepository;
        @Mock AuditLogRepository auditLogRepository;
        @InjectMocks RateAssetUseCaseImpl sut;

        @Test
        void execute_assetFound_savesWithRating() {
            Long assetId = 1L;
            Asset asset = buildAsset(assetId);
            when(assetRepository.findById(assetId)).thenReturn(Optional.of(asset));
            when(assetRepository.save(any())).thenReturn(asset);

            sut.execute(assetId, 4, null);

            assertThat(asset.getRating()).isEqualTo(4);
            verify(assetRepository).save(asset);
        }

        @Test
        void execute_assetFound_logsAssetRatedAuditEvent() {
            Long assetId = 1L;
            Asset asset = buildAsset(assetId);
            when(assetRepository.findById(assetId)).thenReturn(Optional.of(asset));
            when(assetRepository.save(any())).thenReturn(asset);

            sut.execute(assetId, 4, null);

            verify(auditLogRepository).log(any());
        }

        @Test
        void execute_auditLogThrows_doesNotPropagate() {
            Long assetId = 1L;
            Asset asset = buildAsset(assetId);
            when(assetRepository.findById(assetId)).thenReturn(Optional.of(asset));
            when(assetRepository.save(any())).thenReturn(asset);
            doThrow(new RuntimeException("mongo down")).when(auditLogRepository).log(any());

            assertThatCode(() -> sut.execute(assetId, 4, null)).doesNotThrowAnyException();
        }

        @Test
        void execute_assetNotFound_throwsNoSuchElementException() {
            when(assetRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> sut.execute(99L, 3, null))
                    .isInstanceOf(NoSuchElementException.class);
        }
    }

    private static Asset buildAsset(Long id) {
        Folder folder = Folder.builder().folderId(1L).path("/photos").build();
        return Asset.builder().assetId(id).folder(folder).fileName("photo.jpg").build();
    }
}
