package com.jpablodrexler.photomanager.application.usecase.folder;

import com.jpablodrexler.photomanager.domain.model.CatalogChangeNotification;
import com.jpablodrexler.photomanager.domain.enums.ReasonEnum;
import com.jpablodrexler.photomanager.domain.model.Asset;
import com.jpablodrexler.photomanager.domain.model.Folder;
import com.jpablodrexler.photomanager.domain.port.out.AssetRepository;
import com.jpablodrexler.photomanager.domain.port.out.FolderRepository;
import com.jpablodrexler.photomanager.domain.port.out.StoragePort;
import com.jpablodrexler.photomanager.domain.port.out.ThumbnailPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PruneDeletedFoldersUseCaseImplTest {

    @Mock FolderRepository folderRepository;
    @Mock AssetRepository assetRepository;
    @Mock StoragePort storagePort;
    @Mock ThumbnailPort thumbnailPort;
    @Mock Consumer<CatalogChangeNotification> consumer;
    @Mock PlatformTransactionManager transactionManager;

    @InjectMocks PruneDeletedFoldersUseCaseImpl sut;

    @Test
    void execute_folderExistsOnDisk_isNotDeleted() {
        Folder folder = Folder.builder().folderId(1L).path("/photos/existing").build();
        when(folderRepository.findAll()).thenReturn(List.of(folder));
        when(storagePort.directoryExists("/photos/existing")).thenReturn(true);

        sut.execute(consumer);

        verify(folderRepository, never()).deleteById(any());
        verify(assetRepository, never()).deleteById(any());
        verify(consumer, never()).accept(any());
    }

    @Test
    void execute_folderMissingFromDisk_deletesAssetsAndFolder() {
        Folder folder = Folder.builder().folderId(10L).path("/photos/deleted").build();
        Asset asset1 = Asset.builder().assetId(101L).build();
        Asset asset2 = Asset.builder().assetId(102L).build();

        when(folderRepository.findAll()).thenReturn(List.of(folder));
        when(storagePort.directoryExists("/photos/deleted")).thenReturn(false);
        when(assetRepository.findByFolder(folder)).thenReturn(List.of(asset1, asset2));

        sut.execute(consumer);

        verify(thumbnailPort).deleteThumbnail("101.bin");   // assetId + ".bin"
        verify(assetRepository).deleteById(101L);
        verify(thumbnailPort).deleteThumbnail("102.bin");
        verify(assetRepository).deleteById(102L);

        verify(folderRepository).deleteById(10L);
    }

    @Test
    void execute_folderMissingFromDisk_sendsFolderDeletedNotification() {
        Folder folder = Folder.builder().folderId(10L).path("/photos/deleted").build();
        when(folderRepository.findAll()).thenReturn(List.of(folder));
        when(storagePort.directoryExists("/photos/deleted")).thenReturn(false);
        when(assetRepository.findByFolder(folder)).thenReturn(List.of());

        sut.execute(consumer);

        ArgumentCaptor<CatalogChangeNotification> captor = ArgumentCaptor.forClass(CatalogChangeNotification.class);
        verify(consumer).accept(captor.capture());
        assertThat(captor.getValue().getReason()).isEqualTo(ReasonEnum.FOLDER_DELETED);
        assertThat(captor.getValue().getFolderPath()).isEqualTo("/photos/deleted");
    }

    @Test
    void execute_nullConsumer_doesNotThrow() {
        Folder folder = Folder.builder().folderId(10L).path("/photos/deleted").build();
        when(folderRepository.findAll()).thenReturn(List.of(folder));
        when(storagePort.directoryExists("/photos/deleted")).thenReturn(false);
        when(assetRepository.findByFolder(folder)).thenReturn(List.of());

        sut.execute(null);

        verify(folderRepository).deleteById(10L);
    }

    @Test
    void execute_mixedFolders_onlyDeletesMissingOnes() {
        Folder existing = Folder.builder().folderId(1L).path("/photos/exists").build();
        Folder missing = Folder.builder().folderId(2L).path("/photos/gone").build();
        when(folderRepository.findAll()).thenReturn(List.of(existing, missing));
        when(storagePort.directoryExists("/photos/exists")).thenReturn(true);
        when(storagePort.directoryExists("/photos/gone")).thenReturn(false);
        when(assetRepository.findByFolder(missing)).thenReturn(List.of());

        sut.execute(consumer);

        verify(folderRepository, never()).deleteById(1L);
        verify(folderRepository).deleteById(2L);
    }

    @Test
    void execute_folderMissingFromDisk_deletesDbRecordsBeforeThumbnails() {
        Folder folder = Folder.builder().folderId(1L).path("/photos/gone").build();
        Asset asset = Asset.builder().assetId(10L).folder(folder).fileName("a.jpg").build();
        when(folderRepository.findAll()).thenReturn(List.of(folder));
        when(storagePort.directoryExists("/photos/gone")).thenReturn(false);
        when(assetRepository.findByFolder(folder)).thenReturn(List.of(asset));

        sut.execute(consumer);

        InOrder order = inOrder(assetRepository, folderRepository, thumbnailPort);
        order.verify(assetRepository).deleteById(10L);
        order.verify(folderRepository).deleteById(1L);
        order.verify(thumbnailPort).deleteThumbnail(asset.getThumbnailBlobName());
    }

    @Test
    void execute_dbDeletionFails_doesNotDeleteThumbnail() {
        Folder folder = Folder.builder().folderId(1L).path("/photos/gone").build();
        Asset asset = Asset.builder().assetId(10L).folder(folder).fileName("a.jpg").build();
        when(folderRepository.findAll()).thenReturn(List.of(folder));
        when(storagePort.directoryExists("/photos/gone")).thenReturn(false);
        when(assetRepository.findByFolder(folder)).thenReturn(List.of(asset));
        doThrow(new RuntimeException("db unavailable")).when(assetRepository).deleteById(10L);

        assertThatThrownBy(() -> sut.execute(consumer))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("db unavailable");

        verify(thumbnailPort, never()).deleteThumbnail(any());
        verify(folderRepository, never()).deleteById(any());
    }
}
