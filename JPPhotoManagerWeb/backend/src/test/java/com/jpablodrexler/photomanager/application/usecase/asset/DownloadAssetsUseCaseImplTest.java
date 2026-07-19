package com.jpablodrexler.photomanager.application.usecase.asset;

import com.jpablodrexler.photomanager.domain.model.Asset;
import com.jpablodrexler.photomanager.domain.model.Folder;
import com.jpablodrexler.photomanager.domain.port.out.AssetRepository;
import com.jpablodrexler.photomanager.domain.port.out.AuditLogRepository;
import com.jpablodrexler.photomanager.domain.port.out.StoragePort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipInputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DownloadAssetsUseCaseImplTest {

    @Mock AssetRepository assetRepository;
    @Mock StoragePort storagePort;
    @Mock AuditLogRepository auditLogRepository;
    @InjectMocks DownloadAssetsUseCaseImpl sut;

    private static final UUID USER_ID = UUID.randomUUID();

    @Test
    void execute_singleAsset_writesZipWithOneEntry() throws IOException {
        Asset asset = buildAsset(1L, "photo.jpg");
        when(assetRepository.findAllById(List.of(1L))).thenReturn(List.of(asset));
        when(storagePort.readFileBytes("/photos/photo.jpg")).thenReturn(new byte[]{10, 20, 30});

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        sut.execute(List.of(1L), out, USER_ID);

        ZipInputStream zin = new ZipInputStream(new java.io.ByteArrayInputStream(out.toByteArray()));
        assertThat(zin.getNextEntry()).isNotNull();
        assertThat(zin.getNextEntry()).isNull();
    }

    @Test
    void execute_singleAsset_logsAssetDownloadedAuditEvent() throws IOException {
        Asset asset = buildAsset(1L, "photo.jpg");
        when(assetRepository.findAllById(List.of(1L))).thenReturn(List.of(asset));
        when(storagePort.readFileBytes("/photos/photo.jpg")).thenReturn(new byte[]{10, 20, 30});

        sut.execute(List.of(1L), new ByteArrayOutputStream(), USER_ID);

        verify(auditLogRepository).log(any());
    }

    @Test
    void execute_auditLogThrows_doesNotPropagate() throws IOException {
        Asset asset = buildAsset(1L, "photo.jpg");
        when(assetRepository.findAllById(List.of(1L))).thenReturn(List.of(asset));
        when(storagePort.readFileBytes("/photos/photo.jpg")).thenReturn(new byte[]{10, 20, 30});
        doThrow(new RuntimeException("mongo down")).when(auditLogRepository).log(any());

        assertThatCode(() -> sut.execute(List.of(1L), new ByteArrayOutputStream(), USER_ID))
                .doesNotThrowAnyException();
    }

    @Test
    void execute_duplicateFileNames_prefixesSecondWithAssetId() throws IOException {
        Asset a1 = buildAsset(1L, "photo.jpg");
        Asset a2 = buildAsset(2L, "photo.jpg");
        when(assetRepository.findAllById(List.of(1L, 2L))).thenReturn(List.of(a1, a2));
        when(storagePort.readFileBytes(anyString())).thenReturn(new byte[]{1});

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        sut.execute(List.of(1L, 2L), out, USER_ID);

        ZipInputStream zin = new ZipInputStream(new java.io.ByteArrayInputStream(out.toByteArray()));
        String name1 = zin.getNextEntry().getName();
        String name2 = zin.getNextEntry().getName();
        assertThat(name1).isEqualTo("photo.jpg");
        assertThat(name2).isEqualTo("2_photo.jpg");
    }

    @Test
    void execute_fileReadThrows_skipsEntryAndFinishesZip() throws IOException {
        Asset asset = buildAsset(1L, "bad.jpg");
        when(assetRepository.findAllById(List.of(1L))).thenReturn(List.of(asset));
        when(storagePort.readFileBytes(anyString())).thenThrow(new IOException("read error"));

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        assertThatCode(() -> sut.execute(List.of(1L), out, USER_ID)).doesNotThrowAnyException();
        assertThat(out.size()).isGreaterThan(0);
    }

    private static Asset buildAsset(Long id, String fileName) {
        Folder folder = Folder.builder().folderId(1L).path("/photos").build();
        return Asset.builder().assetId(id).folder(folder).fileName(fileName).build();
    }
}
