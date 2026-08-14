package com.jpablodrexler.photomanager.infrastructure.batch;

import com.jpablodrexler.photomanager.domain.model.Asset;
import com.jpablodrexler.photomanager.domain.model.Folder;
import com.jpablodrexler.photomanager.domain.port.out.AssetRepository;
import com.jpablodrexler.photomanager.domain.port.out.FolderRepository;
import com.jpablodrexler.photomanager.domain.port.out.StoragePort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CatalogFileItemReaderTest {

    @Mock AssetRepository assetRepository;
    @Mock FolderRepository folderRepository;
    @Mock StoragePort storagePort;

    @Test
    void read_folderNotCatalogued_returnsAllFilesThenNull() {
        when(folderRepository.findByPath("/photos")).thenReturn(Optional.empty());
        when(storagePort.listFiles("/photos")).thenReturn(List.of("a.jpg", "b.jpg"));

        CatalogFileItemReader sut = new CatalogFileItemReader("/photos", assetRepository, folderRepository, storagePort);

        assertThat(sut.read()).isEqualTo(Paths.get("a.jpg"));
        assertThat(sut.read()).isEqualTo(Paths.get("b.jpg"));
        assertThat(sut.read()).isNull();
    }

    @Test
    void read_folderCatalogued_filtersOutAlreadyCataloguedFiles() {
        Folder folder = new Folder();
        folder.setFolderId(1L);
        folder.setPath("/photos");
        Asset existing = new Asset();
        existing.setFileName("a.jpg");

        when(folderRepository.findByPath("/photos")).thenReturn(Optional.of(folder));
        when(assetRepository.findByFolder(folder)).thenReturn(List.of(existing));
        when(storagePort.listFiles("/photos")).thenReturn(List.of("a.jpg", "b.jpg"));

        CatalogFileItemReader sut = new CatalogFileItemReader("/photos", assetRepository, folderRepository, storagePort);

        Path first = sut.read();

        assertThat(first).isEqualTo(Paths.get("b.jpg"));
        assertThat(sut.read()).isNull();
    }

    @Test
    void read_noFilesOnDisk_returnsNullImmediately() {
        when(folderRepository.findByPath("/empty")).thenReturn(Optional.empty());
        when(storagePort.listFiles("/empty")).thenReturn(List.of());

        CatalogFileItemReader sut = new CatalogFileItemReader("/empty", assetRepository, folderRepository, storagePort);

        assertThat(sut.read()).isNull();
    }
}
