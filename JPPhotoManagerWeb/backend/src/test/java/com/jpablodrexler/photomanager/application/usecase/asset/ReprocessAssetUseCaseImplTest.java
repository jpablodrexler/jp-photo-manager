package com.jpablodrexler.photomanager.application.usecase.asset;

import com.jpablodrexler.photomanager.application.dto.AssetUploadedEvent;
import com.jpablodrexler.photomanager.domain.enums.ProcessingStatus;
import com.jpablodrexler.photomanager.domain.model.Asset;
import com.jpablodrexler.photomanager.domain.model.Folder;
import com.jpablodrexler.photomanager.domain.port.out.AssetRepository;
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

import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReprocessAssetUseCaseImplTest {

    @Mock AssetRepository assetRepository;
    @Mock KafkaTemplate<String, Object> kafkaTemplate;
    @InjectMocks ReprocessAssetUseCaseImpl sut;

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

    private Asset buildAsset() {
        Folder folder = Folder.builder().folderId(1L).path("/photos").build();
        return Asset.builder().assetId(10L).folder(folder).fileName("img.jpg").build();
    }

    @Test
    void execute_assetNotFound_throwsNoSuchElementException() {
        when(assetRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sut.execute(99L))
                .isInstanceOf(NoSuchElementException.class);

        verify(assetRepository, never()).updateProcessingStatus(any(), any());
        verify(kafkaTemplate, never()).send(any(), any(), any());
    }

    @Test
    void execute_assetFound_marksProcessingAndPublishesEventAfterCommit() {
        when(assetRepository.findById(10L)).thenReturn(Optional.of(buildAsset()));

        sut.execute(10L);
        triggerAfterCommit();

        verify(assetRepository).updateProcessingStatus(10L, ProcessingStatus.PROCESSING);

        ArgumentCaptor<AssetUploadedEvent> eventCaptor = ArgumentCaptor.forClass(AssetUploadedEvent.class);
        verify(kafkaTemplate).send(eq("asset.uploaded"), eq("10"), eventCaptor.capture());
        AssetUploadedEvent event = eventCaptor.getValue();
        assertThat(event.assetId()).isEqualTo(10L);
        assertThat(event.filePath()).isEqualTo("/photos/img.jpg");
    }

    @Test
    void execute_beforeTransactionCommits_doesNotPublishEventYet() {
        when(assetRepository.findById(10L)).thenReturn(Optional.of(buildAsset()));

        sut.execute(10L);

        verify(kafkaTemplate, never()).send(any(), any(), any());
    }
}
