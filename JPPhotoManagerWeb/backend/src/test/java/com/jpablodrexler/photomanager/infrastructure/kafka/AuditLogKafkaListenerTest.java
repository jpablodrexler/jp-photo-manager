package com.jpablodrexler.photomanager.infrastructure.kafka;

import com.jpablodrexler.photomanager.application.dto.AssetCatalogedEvent;
import com.jpablodrexler.photomanager.application.dto.AssetDeletedEvent;
import com.jpablodrexler.photomanager.application.dto.CatalogProgressMessage;
import com.jpablodrexler.photomanager.application.dto.ConvertAssetsResult;
import com.jpablodrexler.photomanager.application.dto.ConvertProgressMessage;
import com.jpablodrexler.photomanager.application.dto.SyncAssetsResult;
import com.jpablodrexler.photomanager.application.dto.SyncProgressMessage;
import com.jpablodrexler.photomanager.domain.enums.AuditAction;
import com.jpablodrexler.photomanager.domain.model.AuditEvent;
import com.jpablodrexler.photomanager.domain.port.out.AuditLogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuditLogKafkaListenerTest {

    @Mock AuditLogRepository auditLogRepository;
    @InjectMocks AuditLogKafkaListener sut;

    @Test
    void onAssetCataloged_writesAuditEventWithAssetCatalogedAction() {
        sut.onAssetCataloged(new AssetCatalogedEvent(7L, "/photos/2024", Instant.now()));

        ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditLogRepository).log(captor.capture());
        assertThat(captor.getValue().getAction()).isEqualTo(AuditAction.ASSET_CATALOGED);
        assertThat(captor.getValue().getEntityId()).isEqualTo("7");
    }

    @Test
    void onAssetDeleted_writesAuditEventWithFolderIdAndPermanentMetadata() {
        sut.onAssetDeleted(new AssetDeletedEvent(9L, 3L, "/photos", Instant.now(), true));

        ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditLogRepository).log(captor.capture());
        assertThat(captor.getValue().getAction()).isEqualTo(AuditAction.ASSET_DELETED);
        assertThat(captor.getValue().getEntityId()).isEqualTo("9");
        assertThat(captor.getValue().getMetadata()).containsEntry("folderId", 3L).containsEntry("permanent", true);
    }

    @Test
    void onCatalogProgress_doneFalse_ignoresMessage() {
        sut.onCatalogProgress(CatalogProgressMessage.progress(42L, null));

        verify(auditLogRepository, never()).log(any());
    }

    @Test
    void onCatalogProgress_doneTrue_writesSingleRunLevelAuditEvent() {
        sut.onCatalogProgress(CatalogProgressMessage.done(42L, 3, 120L, 5340L));

        ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditLogRepository).log(captor.capture());
        assertThat(captor.getValue().getAction()).isEqualTo(AuditAction.CATALOG_RUN);
        assertThat(captor.getValue().getEntityId()).isEqualTo("42");
        assertThat(captor.getValue().getMetadata())
                .containsEntry("foldersScanned", 3)
                .containsEntry("assetsAdded", 120L)
                .containsEntry("durationMs", 5340L);
    }

    @Test
    void onSyncProgress_doneFalse_ignoresMessage() {
        sut.onSyncProgress(SyncProgressMessage.progress(8L, "Syncing..."));

        verify(auditLogRepository, never()).log(any());
    }

    @Test
    void onSyncProgress_doneTrue_writesRunLevelAuditEventWithAggregatedResults() {
        SyncAssetsResult result = new SyncAssetsResult("/src", "/dst");
        result.setSyncedCount(5);
        result.setDeletedCount(2);

        sut.onSyncProgress(SyncProgressMessage.done(8L, List.of(result)));

        ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditLogRepository).log(captor.capture());
        assertThat(captor.getValue().getAction()).isEqualTo(AuditAction.SYNC_RUN);
        assertThat(captor.getValue().getEntityId()).isEqualTo("8");
        assertThat(captor.getValue().getMetadata())
                .containsEntry("sourceDir", "/src")
                .containsEntry("targetDir", "/dst")
                .containsEntry("filesCopied", 5)
                .containsEntry("filesDeleted", 2);
    }

    @Test
    void onConvertProgress_doneFalse_ignoresMessage() {
        sut.onConvertProgress(ConvertProgressMessage.progress(11L, "Converting..."));

        verify(auditLogRepository, never()).log(any());
    }

    @Test
    void onConvertProgress_doneTrue_writesRunLevelAuditEventWithAggregatedResults() {
        ConvertAssetsResult result = new ConvertAssetsResult("/src", "/dst");
        result.setConvertedCount(4);

        sut.onConvertProgress(ConvertProgressMessage.done(11L, List.of(result)));

        ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditLogRepository).log(captor.capture());
        assertThat(captor.getValue().getAction()).isEqualTo(AuditAction.CONVERT_RUN);
        assertThat(captor.getValue().getEntityId()).isEqualTo("11");
        assertThat(captor.getValue().getMetadata())
                .containsEntry("sourceDir", "/src")
                .containsEntry("targetDir", "/dst")
                .containsEntry("filesConverted", 4);
    }

    @Test
    void onAssetCataloged_auditWriteThrows_doesNotPropagate() {
        doThrow(new RuntimeException("mongo down")).when(auditLogRepository).log(any());

        assertThatCode(() -> sut.onAssetCataloged(new AssetCatalogedEvent(1L, "/photos", Instant.now())))
                .doesNotThrowAnyException();
    }
}
