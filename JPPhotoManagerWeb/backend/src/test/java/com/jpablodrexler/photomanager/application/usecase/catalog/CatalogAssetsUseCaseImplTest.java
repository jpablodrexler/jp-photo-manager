package com.jpablodrexler.photomanager.application.usecase.catalog;

import com.jpablodrexler.photomanager.domain.port.out.CatalogFolderPort;
import com.jpablodrexler.photomanager.domain.port.out.CatalogStateRepository;
import com.jpablodrexler.photomanager.domain.port.out.StoragePort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CatalogAssetsUseCaseImplTest {

    @Mock CatalogFolderPort catalogFolderPort;
    @Mock StoragePort storagePort;
    @Mock CatalogStateRepository catalogStateRepository;

    CatalogAssetsUseCaseImpl sut;

    private static final String ROOT = "/tmp/photos";

    @BeforeEach
    void setUp() {
        sut = new CatalogAssetsUseCaseImpl(catalogFolderPort, storagePort, catalogStateRepository, "test-instance");
        ReflectionTestUtils.setField(sut, "rootCatalogFolders", ROOT);
    }

    @Test
    void execute_lockNotAcquired_returnsImmediatelyWithoutCataloging() throws Exception {
        when(catalogStateRepository.tryAcquire(anyString(), any(Instant.class))).thenReturn(false);

        CompletableFuture<Void> future = sut.execute(notification -> {});
        future.get();

        verify(catalogFolderPort, never()).catalogFolder(any(), any(), any(), any(), anyInt());
    }

    @Test
    void execute_rootDirectoryNotExist_skipsAndCompletesCleanly() throws Exception {
        when(catalogStateRepository.tryAcquire(anyString(), any(Instant.class))).thenReturn(true);
        when(storagePort.directoryExists(ROOT)).thenReturn(false);

        sut.execute(notification -> {}).get();

        verify(catalogFolderPort, never()).catalogFolder(any(), any(), any(), any(), anyInt());
        verify(catalogStateRepository).markCompleted(eq("test-instance"), any(Instant.class));
        verify(catalogStateRepository).release("test-instance");
    }

    @Test
    void execute_singleFolder_catalogsFolder() throws Exception {
        when(catalogStateRepository.tryAcquire(anyString(), any(Instant.class))).thenReturn(true);
        when(storagePort.directoryExists(ROOT)).thenReturn(true);
        when(storagePort.listSubDirectories(ROOT)).thenReturn(List.of());

        sut.execute(notification -> {}).get();

        verify(catalogFolderPort, times(1)).catalogFolder(eq(ROOT), any(), any(), any(), anyInt());
        verify(catalogStateRepository).markCompleted(eq("test-instance"), any(Instant.class));
        verify(catalogStateRepository).release("test-instance");
    }

    @Test
    void execute_withSubFolders_catalogsAllFolders() throws Exception {
        String sub = ROOT + "/sub";
        when(catalogStateRepository.tryAcquire(anyString(), any(Instant.class))).thenReturn(true);
        when(storagePort.directoryExists(ROOT)).thenReturn(true);
        when(storagePort.listSubDirectories(ROOT)).thenReturn(List.of(sub));
        when(storagePort.listSubDirectories(sub)).thenReturn(List.of());

        sut.execute(notification -> {}).get();

        verify(catalogFolderPort, times(2)).catalogFolder(any(), any(), any(), any(), anyInt());
    }

    @Test
    void execute_folderThrows_continuesAndReleasesLock() throws Exception {
        when(catalogStateRepository.tryAcquire(anyString(), any(Instant.class))).thenReturn(true);
        when(storagePort.directoryExists(ROOT)).thenReturn(true);
        when(storagePort.listSubDirectories(ROOT)).thenReturn(List.of());
        org.mockito.Mockito.doThrow(new RuntimeException("catalog error"))
                .when(catalogFolderPort).catalogFolder(any(), any(), any(), any(), anyInt());

        sut.execute(notification -> {}).get();

        verify(catalogStateRepository).release("test-instance");
    }
}
