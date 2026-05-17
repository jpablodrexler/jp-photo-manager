package com.jpablodrexler.photomanager.application.usecase.asset;

import com.jpablodrexler.photomanager.application.dto.AssetFilter;
import com.jpablodrexler.photomanager.application.dto.AssetImage;
import com.jpablodrexler.photomanager.application.dto.PaginatedResult;
import com.jpablodrexler.photomanager.domain.model.Asset;
import com.jpablodrexler.photomanager.domain.model.AssetExif;
import com.jpablodrexler.photomanager.domain.model.Folder;
import com.jpablodrexler.photomanager.domain.port.out.AssetExifRepository;
import com.jpablodrexler.photomanager.domain.port.out.AssetRepository;
import com.jpablodrexler.photomanager.domain.port.out.StoragePort;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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
            AssetFilter filter = new AssetFilter(1L, null, null, null, null, null, 0, 20, false);
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
        @InjectMocks GetAssetImageUseCaseImpl sut;

        @Test
        void execute_assetFound_returnsAssetImage() throws IOException {
            Long assetId = 1L;
            Folder folder = Folder.builder().path("/photos").build();
            Asset asset = Asset.builder().assetId(assetId).folder(folder).fileName("img.jpg").build();
            byte[] bytes = new byte[]{1, 2, 3};
            when(assetRepository.findById(assetId)).thenReturn(Optional.of(asset));
            when(storagePort.readFileBytes("/photos/img.jpg")).thenReturn(bytes);

            AssetImage result = sut.execute(assetId);

            assertThat(result.bytes()).isEqualTo(bytes);
            assertThat(result.fileName()).isEqualTo("img.jpg");
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
    class RateAssetUseCaseImplTest {

        @Mock AssetRepository assetRepository;
        @InjectMocks RateAssetUseCaseImpl sut;

        @Test
        void execute_assetFound_savesWithRating() {
            Long assetId = 1L;
            Asset asset = buildAsset(assetId);
            when(assetRepository.findById(assetId)).thenReturn(Optional.of(asset));
            when(assetRepository.save(any())).thenReturn(asset);

            sut.execute(assetId, 4);

            assertThat(asset.getRating()).isEqualTo(4);
            verify(assetRepository).save(asset);
        }

        @Test
        void execute_assetNotFound_throwsNoSuchElementException() {
            when(assetRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> sut.execute(99L, 3))
                    .isInstanceOf(NoSuchElementException.class);
        }
    }

    private static Asset buildAsset(Long id) {
        Folder folder = Folder.builder().folderId(1L).path("/photos").build();
        return Asset.builder().assetId(id).folder(folder).fileName("photo.jpg").build();
    }
}
