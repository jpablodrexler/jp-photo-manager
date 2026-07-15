package com.jpablodrexler.photomanager.application.usecase.asset;

import com.jpablodrexler.photomanager.domain.model.AssetImage;
import com.jpablodrexler.photomanager.domain.enums.AuditAction;
import com.jpablodrexler.photomanager.domain.enums.AuditEntityType;
import com.jpablodrexler.photomanager.domain.model.AuditEvent;
import com.jpablodrexler.photomanager.domain.port.in.asset.GetAssetImageUseCase;
import com.jpablodrexler.photomanager.domain.port.out.AssetRepository;
import com.jpablodrexler.photomanager.domain.port.out.AuditLogRepository;
import com.jpablodrexler.photomanager.domain.port.out.StoragePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.Instant;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class GetAssetImageUseCaseImpl implements GetAssetImageUseCase {

    private final AssetRepository assetRepository;
    private final StoragePort storagePort;
    private final AuditLogRepository auditLogRepository;

    @Override
    @Transactional(readOnly = true)
    public AssetImage execute(Long assetId, UUID userId) throws IOException {
        var asset = assetRepository.findById(assetId)
                .orElseThrow(() -> new NoSuchElementException("Asset not found: " + assetId));
        byte[] bytes = storagePort.readFileBytes(asset.getFullPath());

        logAudit(assetId, userId);

        return new AssetImage(bytes, asset.getFileName(), detectMimeType(bytes));
    }

    private String detectMimeType(byte[] bytes) {
        if (bytes.length >= 3
                && (bytes[0] & 0xFF) == 0xFF
                && (bytes[1] & 0xFF) == 0xD8
                && (bytes[2] & 0xFF) == 0xFF) {
            return "image/jpeg";
        }
        if (bytes.length >= 4
                && (bytes[0] & 0xFF) == 0x89
                && bytes[1] == 'P'
                && bytes[2] == 'N'
                && bytes[3] == 'G') {
            return "image/png";
        }
        if (bytes.length >= 4
                && bytes[0] == 'G'
                && bytes[1] == 'I'
                && bytes[2] == 'F'
                && bytes[3] == '8') {
            return "image/gif";
        }
        return null;
    }

    private void logAudit(Long assetId, UUID userId) {
        try {
            auditLogRepository.log(AuditEvent.builder()
                    .userId(userId)
                    .action(AuditAction.ASSET_VIEWED)
                    .entityType(AuditEntityType.ASSET)
                    .entityId(String.valueOf(assetId))
                    .timestamp(Instant.now())
                    .build());
        } catch (Exception e) {
            log.warn("Failed to write audit log for AssetViewed assetId={}: {}", assetId, e.getMessage());
        }
    }
}
