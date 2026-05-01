package com.jpablodrexler.photomanager.infrastructure.service;

import com.jpablodrexler.photomanager.domain.entity.Asset;
import com.jpablodrexler.photomanager.domain.repository.CatalogRunStateRepository;
import com.jpablodrexler.photomanager.domain.service.CatalogFolderService;
import com.jpablodrexler.photomanager.domain.service.StorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CatalogAssetsServiceImplTest {

    @Mock
    CatalogFolderService catalogFolderService;

    @Mock
    StorageService storageService;

    @Mock
    CatalogRunStateRepository catalogRunStateRepository;

    CatalogAssetsServiceImpl sut;

    @BeforeEach
    void setUp() {
        sut = new CatalogAssetsServiceImpl(catalogFolderService, storageService, catalogRunStateRepository, "test-instance-id");
        ReflectionTestUtils.setField(sut, "rootCatalogFolders", "/pictures");
    }

    @Test
    void catalogAssetsAsync_rootDoesNotExist_skipsFolder() throws Exception {
        when(catalogRunStateRepository.tryAcquire(any(), any())).thenReturn(1);
        when(storageService.directoryExists("/pictures")).thenReturn(false);

        sut.catalogAssetsAsync(null).get();

        verify(catalogFolderService, never()).catalogFolder(any(), any(), any(Runnable.class), any(), anyInt());
    }

    @Test
    void catalogAssetsAsync_rootExists_catalogsRootFolder() throws Exception {
        when(catalogRunStateRepository.tryAcquire(any(), any())).thenReturn(1);
        when(storageService.directoryExists("/pictures")).thenReturn(true);
        when(storageService.listSubDirectories("/pictures")).thenReturn(List.of());

        sut.catalogAssetsAsync(null).get();

        verify(catalogFolderService).catalogFolder(eq("/pictures"), any(), any(Runnable.class), any(), eq(1));
    }

    @Test
    void catalogAssetsAsync_rootWithSubFolders_catalogsSubFoldersToo() throws Exception {
        when(catalogRunStateRepository.tryAcquire(any(), any())).thenReturn(1);
        when(storageService.directoryExists("/pictures")).thenReturn(true);
        when(storageService.listSubDirectories("/pictures")).thenReturn(List.of("/pictures/2024"));
        when(storageService.listSubDirectories("/pictures/2024")).thenReturn(List.of());

        sut.catalogAssetsAsync(null).get();

        verify(catalogFolderService).catalogFolder(eq("/pictures"), any(), any(Runnable.class), any(), eq(2));
        verify(catalogFolderService).catalogFolder(eq("/pictures/2024"), any(), any(Runnable.class), any(), eq(2));
    }

    @Test
    void catalogAssetsAsync_multipleRoots_catalogsAll() throws Exception {
        ReflectionTestUtils.setField(sut, "rootCatalogFolders", "/pictures;/downloads");
        when(catalogRunStateRepository.tryAcquire(any(), any())).thenReturn(1);
        when(storageService.directoryExists("/pictures")).thenReturn(true);
        when(storageService.directoryExists("/downloads")).thenReturn(true);
        when(storageService.listSubDirectories(any())).thenReturn(List.of());

        sut.catalogAssetsAsync(null).get();

        verify(catalogFolderService).catalogFolder(eq("/pictures"), any(), any(Runnable.class), any(), eq(2));
        verify(catalogFolderService).catalogFolder(eq("/downloads"), any(), any(Runnable.class), any(), eq(2));
    }

    @Test
    void catalogAssetsAsync_multipleRoots_onlyExistingRootIsCataloged() throws Exception {
        ReflectionTestUtils.setField(sut, "rootCatalogFolders", "/pictures;/missing");
        when(catalogRunStateRepository.tryAcquire(any(), any())).thenReturn(1);
        when(storageService.directoryExists("/pictures")).thenReturn(true);
        when(storageService.directoryExists("/missing")).thenReturn(false);
        when(storageService.listSubDirectories("/pictures")).thenReturn(List.of());

        sut.catalogAssetsAsync(null).get();

        verify(catalogFolderService, times(1)).catalogFolder(any(), any(), any(Runnable.class), any(), anyInt());
        verify(catalogFolderService).catalogFolder(eq("/pictures"), any(), any(Runnable.class), any(), eq(1));
    }

    @Test
    void catalogAssetsAsync_folderCatalogException_logsAndContinuesToNextFolder() throws Exception {
        when(catalogRunStateRepository.tryAcquire(any(), any())).thenReturn(1);
        when(storageService.directoryExists("/pictures")).thenReturn(true);
        when(storageService.listSubDirectories("/pictures")).thenReturn(List.of("/pictures/sub"));
        when(storageService.listSubDirectories("/pictures/sub")).thenReturn(List.of());
        doThrow(new RuntimeException("catalog error"))
                .when(catalogFolderService).catalogFolder(eq("/pictures"), any(), any(Runnable.class), any(), anyInt());

        sut.catalogAssetsAsync(null).get();

        verify(catalogFolderService).catalogFolder(eq("/pictures/sub"), any(), any(Runnable.class), any(), anyInt());
    }

    @Test
    void catalogAssetsAsync_nestedSubFolders_catalogsAllLevels() throws Exception {
        when(catalogRunStateRepository.tryAcquire(any(), any())).thenReturn(1);
        when(storageService.directoryExists("/pictures")).thenReturn(true);
        when(storageService.listSubDirectories("/pictures")).thenReturn(List.of("/pictures/2024"));
        when(storageService.listSubDirectories("/pictures/2024")).thenReturn(List.of("/pictures/2024/Jan"));
        when(storageService.listSubDirectories("/pictures/2024/Jan")).thenReturn(List.of());

        sut.catalogAssetsAsync(null).get();

        verify(catalogFolderService, times(3)).catalogFolder(any(), any(), any(Runnable.class), any(), eq(3));
    }

    @Test
    void catalogAssetsAsync_lockAlreadyHeld_returnsEmptyFuture() throws Exception {
        when(catalogRunStateRepository.tryAcquire(any(), any())).thenReturn(0);

        CompletableFuture<Void> result = sut.catalogAssetsAsync(null);

        assertThat(result).isDone();
        verify(catalogFolderService, never()).catalogFolder(any(), any(), any(Runnable.class), any(), anyInt());
    }

    @Test
    void createAsset_delegatesToCatalogFolderService() {
        Asset expected = new Asset();
        when(catalogFolderService.createAsset("/photos", "photo.jpg")).thenReturn(expected);

        Asset result = sut.createAsset("/photos", "photo.jpg");

        assertThat(result).isEqualTo(expected);
        verify(catalogFolderService).createAsset("/photos", "photo.jpg");
    }

    @Test
    void runCatalog_lockAlreadyHeld_skipsRun() {
        when(catalogRunStateRepository.tryAcquire(any(), any())).thenReturn(0);

        sut.runCatalog();

        verify(catalogFolderService, never()).catalogFolder(any(), any(), any(Runnable.class), any(), anyInt());
    }

    @Test
    void runCatalog_releasesLockAfterSuccess() {
        when(catalogRunStateRepository.tryAcquire(any(), any())).thenReturn(1);
        when(storageService.directoryExists(any())).thenReturn(false);

        sut.runCatalog();

        verify(catalogRunStateRepository).release("test-instance-id");
    }

    @Test
    void runCatalog_releasesLockAfterException() {
        when(catalogRunStateRepository.tryAcquire(any(), any())).thenReturn(1);
        when(storageService.directoryExists(any())).thenThrow(new RuntimeException("disk error"));

        assertThatCode(() -> sut.runCatalog()).doesNotThrowAnyException();

        verify(catalogRunStateRepository).release("test-instance-id");
    }

    @Test
    void doRunCatalog_threadInterrupted_stopsAtNextFolder() throws Exception {
        when(catalogRunStateRepository.tryAcquire(any(), any())).thenReturn(1);
        when(storageService.directoryExists("/pictures")).thenReturn(true);
        when(storageService.listSubDirectories("/pictures")).thenReturn(List.of("/pictures/sub"));
        when(storageService.listSubDirectories("/pictures/sub")).thenReturn(List.of());

        doAnswer(inv -> {
            Thread.currentThread().interrupt();
            return null;
        }).when(catalogFolderService).catalogFolder(eq("/pictures"), any(), any(Runnable.class), any(), anyInt());

        sut.catalogAssetsAsync(null).get();

        verify(catalogFolderService, never()).catalogFolder(eq("/pictures/sub"), any(), any(Runnable.class), any(), anyInt());
        Thread.interrupted();
    }
}
