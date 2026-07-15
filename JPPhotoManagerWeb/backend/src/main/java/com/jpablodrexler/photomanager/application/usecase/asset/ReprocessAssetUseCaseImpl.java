package com.jpablodrexler.photomanager.application.usecase.asset;

import com.jpablodrexler.photomanager.application.dto.AssetUploadedEvent;
import com.jpablodrexler.photomanager.domain.enums.ProcessingStatus;
import com.jpablodrexler.photomanager.domain.model.Asset;
import com.jpablodrexler.photomanager.domain.port.in.asset.ReprocessAssetUseCase;
import com.jpablodrexler.photomanager.domain.port.out.AssetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class ReprocessAssetUseCaseImpl implements ReprocessAssetUseCase {

    private final AssetRepository assetRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void execute(Long assetId) {
        Asset asset = assetRepository.findById(assetId)
                .orElseThrow(() -> new NoSuchElementException("Asset not found: " + assetId));

        // Targeted column update rather than mutate-then-save(): avoids round-tripping (and
        // potentially going stale against) the full entity while the three stage processors may
        // already be concurrently updating their own columns for this asset.
        assetRepository.updateProcessingStatus(assetId, ProcessingStatus.PROCESSING);

        String filePath = asset.getFolder().getPath() + "/" + asset.getFileName();
        AssetUploadedEvent event = new AssetUploadedEvent(asset.getAssetId(), filePath,
                asset.getFolder().getPath(), asset.getFileName());
        // Deferred until commit: a consumer that reads asset.uploaded before this transaction
        // commits would race against the PROCESSING status update's visibility.
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                kafkaTemplate.send("asset.uploaded", String.valueOf(event.assetId()), event);
            }
        });
    }
}
