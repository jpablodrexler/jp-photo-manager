package com.jpablodrexler.photomanager.application.usecase.recycle;

import com.jpablodrexler.photomanager.application.dto.PaginatedResult;
import com.jpablodrexler.photomanager.domain.model.Asset;
import com.jpablodrexler.photomanager.domain.model.Folder;
import com.jpablodrexler.photomanager.domain.port.out.AssetRepository;
import com.jpablodrexler.photomanager.domain.port.out.StoragePort;
import com.jpablodrexler.photomanager.domain.port.out.ThumbnailPort;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RecycleBinUseCasesTest {

    @Nested
    @ExtendWith(MockitoExtension.class)
    class GetDeletedAssetsUseCaseImplTest {

        @Mock AssetRepository assetRepository;
        @InjectMocks GetDeletedAssetsUseCaseImpl sut;

        @Test
        void execute_returnsPaginatedResult() {
            PaginatedResult<Asset> expected = new PaginatedResult<>(List.of(), 0L, 0, 100);
            when(assetRepository.findDeleted(0, 100)).thenReturn(expected);

            PaginatedResult<Asset> result = sut.execute(0);

            assertThat(result).isEqualTo(expected);
        }
    }

    @Nested
    @ExtendWith(MockitoExtension.class)
    class PurgeAssetsUseCaseImplTest {

        @Mock AssetRepository assetRepository;
        @Mock StoragePort storagePort;
        @Mock ThumbnailPort thumbnailPort;
        @InjectMocks PurgeAssetsUseCaseImpl sut;

        @Test
        void execute_specificIds_purgesThoseAssets() throws IOException {
            Asset asset = buildAsset(1L);
            when(assetRepository.findAllById(List.of(1L))).thenReturn(List.of(asset));

            sut.execute(List.of(1L));

            verify(storagePort).deleteFile("/photos/photo1.jpg");
            verify(thumbnailPort).deleteThumbnail("1.bin");
            verify(assetRepository).deleteById(1L);
        }

        @Test
        void execute_emptyIds_purgesAllDeleted() throws IOException {
            Asset asset = buildAsset(2L);
            when(assetRepository.findAllDeleted()).thenReturn(List.of(asset));

            sut.execute(List.of());

            verify(assetRepository).deleteById(2L);
        }

        @Test
        void execute_fileDeleteFails_continuesPurge() throws IOException {
            Asset asset = buildAsset(3L);
            when(assetRepository.findAllById(List.of(3L))).thenReturn(List.of(asset));
            doThrow(new IOException("disk full")).when(storagePort).deleteFile(any());

            sut.execute(List.of(3L));

            verify(thumbnailPort).deleteThumbnail("3.bin");
            verify(assetRepository).deleteById(3L);
        }

        private Asset buildAsset(Long id) {
            Folder folder = Folder.builder().folderId(1L).path("/photos").build();
            return Asset.builder().assetId(id).folder(folder).fileName("photo" + id + ".jpg").build();
        }
    }

    @Nested
    @ExtendWith(MockitoExtension.class)
    class RestoreAssetsUseCaseImplTest {

        @Mock AssetRepository assetRepository;
        @InjectMocks RestoreAssetsUseCaseImpl sut;

        @Test
        void execute_clearsDeletedAtAndSaves() {
            Asset asset = buildDeletedAsset(1L);
            when(assetRepository.findAllById(List.of(1L))).thenReturn(List.of(asset));
            when(assetRepository.save(any())).thenReturn(asset);

            sut.execute(List.of(1L));

            ArgumentCaptor<Asset> captor = ArgumentCaptor.forClass(Asset.class);
            verify(assetRepository).save(captor.capture());
            assertThat(captor.getValue().getDeletedAt()).isNull();
        }

        private Asset buildDeletedAsset(Long id) {
            Folder folder = Folder.builder().folderId(1L).path("/photos").build();
            return Asset.builder().assetId(id).folder(folder).fileName("photo.jpg")
                    .deletedAt(LocalDateTime.now()).build();
        }
    }
}
