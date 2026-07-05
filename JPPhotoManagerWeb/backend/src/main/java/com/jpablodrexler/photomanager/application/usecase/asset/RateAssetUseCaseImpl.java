package com.jpablodrexler.photomanager.application.usecase.asset;

import com.jpablodrexler.photomanager.domain.enums.AuditAction;
import com.jpablodrexler.photomanager.domain.enums.AuditEntityType;
import com.jpablodrexler.photomanager.domain.model.AuditEvent;
import com.jpablodrexler.photomanager.domain.port.in.asset.RateAssetUseCase;
import com.jpablodrexler.photomanager.domain.port.out.AssetRepository;
import com.jpablodrexler.photomanager.domain.port.out.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RateAssetUseCaseImpl implements RateAssetUseCase {

    private final AssetRepository assetRepository;
    private final AuditLogRepository auditLogRepository;

    @Override
    @Transactional
    public void execute(Long assetId, int rating, UUID userId) {
        var asset = assetRepository.findById(assetId)
                .orElseThrow(() -> new NoSuchElementException("Asset not found: " + assetId));
        asset.setRating(rating);
        assetRepository.save(asset);

        logAudit(assetId, rating, userId);
    }

    private void logAudit(Long assetId, int rating, UUID userId) {
        try {
            auditLogRepository.log(AuditEvent.builder()
                    .userId(userId)
                    .action(AuditAction.AssetRated)
                    .entityType(AuditEntityType.ASSET)
                    .entityId(String.valueOf(assetId))
                    .timestamp(Instant.now())
                    .metadata(Map.of("rating", rating))
                    .build());
        } catch (Exception e) {
            log.warn("Failed to write audit log for AssetRated assetId={}: {}", assetId, e.getMessage());
        }
    }
}
