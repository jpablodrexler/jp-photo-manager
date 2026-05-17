package com.jpablodrexler.photomanager.application.usecase.asset;

import com.jpablodrexler.photomanager.application.exception.FolderNotFoundException;
import com.jpablodrexler.photomanager.domain.model.Asset;
import com.jpablodrexler.photomanager.domain.model.Folder;
import com.jpablodrexler.photomanager.domain.port.out.CatalogFolderPort;
import com.jpablodrexler.photomanager.domain.port.out.FolderRepository;
import com.jpablodrexler.photomanager.domain.port.out.StoragePort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UploadAssetUseCaseImplTest {

    @Mock FolderRepository folderRepository;
    @Mock StoragePort storagePort;
    @Mock CatalogFolderPort catalogFolderPort;
    @InjectMocks UploadAssetUseCaseImpl sut;

    @Test
    void execute_folderNotFound_throwsFolderNotFoundException() {
        when(folderRepository.existsByPath("/photos")).thenReturn(false);

        assertThatThrownBy(() -> sut.execute("/photos", "img.jpg", new byte[]{1}))
                .isInstanceOf(FolderNotFoundException.class);
    }

    @Test
    void execute_folderFound_copiesFileAndCatalogsAsset() throws IOException {
        Folder folder = Folder.builder().folderId(1L).path("/photos").build();
        Asset asset = Asset.builder().assetId(10L).folder(folder).fileName("img.jpg").build();
        when(folderRepository.existsByPath("/photos")).thenReturn(true);
        when(catalogFolderPort.createAsset(eq("/photos"), eq("img.jpg"))).thenReturn(asset);

        Asset result = sut.execute("/photos", "img.jpg", new byte[]{1, 2, 3});

        assertThat(result).isEqualTo(asset);
        verify(storagePort).copyFile(any(), eq("/photos/img.jpg"));
    }
}
