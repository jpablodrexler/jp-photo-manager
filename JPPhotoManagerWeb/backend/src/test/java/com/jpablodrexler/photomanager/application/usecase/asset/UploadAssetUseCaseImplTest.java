package com.jpablodrexler.photomanager.application.usecase.asset;

import com.jpablodrexler.photomanager.application.dto.AssetUploadedEvent;
import com.jpablodrexler.photomanager.application.exception.FolderNotFoundException;
import com.jpablodrexler.photomanager.domain.enums.ProcessingStatus;
import com.jpablodrexler.photomanager.domain.model.Asset;
import com.jpablodrexler.photomanager.domain.model.Folder;
import com.jpablodrexler.photomanager.domain.port.out.AssetRepository;
import com.jpablodrexler.photomanager.domain.port.out.FolderRepository;
import com.jpablodrexler.photomanager.domain.port.out.StoragePort;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UploadAssetUseCaseImplTest {

    @Mock FolderRepository folderRepository;
    @Mock StoragePort storagePort;
    @Mock AssetRepository assetRepository;
    @Mock KafkaTemplate<String, Object> kafkaTemplate;
    @InjectMocks UploadAssetUseCaseImpl sut;

    @BeforeEach
    void initTransactionSynchronization() {
        TransactionSynchronizationManager.initSynchronization();
    }

    @AfterEach
    void clearTransactionSynchronization() {
        TransactionSynchronizationManager.clearSynchronization();
    }

    private void triggerAfterCommit() {
        for (TransactionSynchronization sync : TransactionSynchronizationManager.getSynchronizations()) {
            sync.afterCommit();
        }
    }

    @Test
    void execute_folderNotFound_throwsFolderNotFoundException() {
        when(folderRepository.findByPath("/photos")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sut.execute("/photos", "img.jpg", new byte[]{1}))
                .isInstanceOf(FolderNotFoundException.class);

        verify(assetRepository, never()).save(any());
        verify(kafkaTemplate, never()).send(any(), any(), any());
    }

    @Test
    void execute_folderFound_persistsPendingPlaceholder() throws IOException {
        Folder folder = Folder.builder().folderId(1L).path("/photos").build();
        when(folderRepository.findByPath("/photos")).thenReturn(Optional.of(folder));
        when(storagePort.getFileSize("/photos/img.jpg")).thenReturn(2048L);
        when(storagePort.getFileCreationDateTime("/photos/img.jpg")).thenReturn(LocalDateTime.of(2024, 1, 1, 0, 0));
        when(storagePort.getFileModificationDateTime("/photos/img.jpg")).thenReturn(LocalDateTime.of(2024, 1, 2, 0, 0));
        when(storagePort.isVideoFile("img.jpg")).thenReturn(false);
        when(assetRepository.save(any(Asset.class))).thenAnswer(inv -> {
            Asset a = inv.getArgument(0);
            a.setAssetId(10L);
            return a;
        });

        Asset result = sut.execute("/photos", "img.jpg", new byte[]{1, 2, 3});

        assertThat(result.getAssetId()).isEqualTo(10L);
        assertThat(result.getProcessingStatus()).isEqualTo(ProcessingStatus.PENDING);
        assertThat(result.getHash()).isNull();
        assertThat(result.getThumbnailCreationDateTime()).isNull();
        verify(storagePort).copyFile(any(), eq("/photos/img.jpg"));
    }

    @Test
    void execute_afterCommit_publishesUploadedEventWithFileDetails() throws IOException {
        Folder folder = Folder.builder().folderId(1L).path("/photos").build();
        when(folderRepository.findByPath("/photos")).thenReturn(Optional.of(folder));
        when(storagePort.isVideoFile("img.jpg")).thenReturn(false);
        when(assetRepository.save(any(Asset.class))).thenAnswer(inv -> {
            Asset a = inv.getArgument(0);
            a.setAssetId(10L);
            return a;
        });

        sut.execute("/photos", "img.jpg", new byte[]{1, 2, 3});
        triggerAfterCommit();

        ArgumentCaptor<AssetUploadedEvent> eventCaptor = ArgumentCaptor.forClass(AssetUploadedEvent.class);
        verify(kafkaTemplate).send(eq("asset.uploaded"), eq("10"), eventCaptor.capture());
        AssetUploadedEvent event = eventCaptor.getValue();
        assertThat(event.assetId()).isEqualTo(10L);
        assertThat(event.filePath()).isEqualTo("/photos/img.jpg");
        assertThat(event.folderPath()).isEqualTo("/photos");
        assertThat(event.fileName()).isEqualTo("img.jpg");
    }

    @Test
    void execute_beforeTransactionCommits_doesNotPublishEventYet() throws IOException {
        Folder folder = Folder.builder().folderId(1L).path("/photos").build();
        when(folderRepository.findByPath("/photos")).thenReturn(Optional.of(folder));
        when(storagePort.isVideoFile("img.jpg")).thenReturn(false);
        when(assetRepository.save(any(Asset.class))).thenAnswer(inv -> {
            Asset a = inv.getArgument(0);
            a.setAssetId(10L);
            return a;
        });

        sut.execute("/photos", "img.jpg", new byte[]{1, 2, 3});

        verify(kafkaTemplate, never()).send(any(), any(), any());
    }
}
