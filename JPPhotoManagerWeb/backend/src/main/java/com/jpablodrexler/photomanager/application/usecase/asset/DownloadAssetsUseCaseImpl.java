package com.jpablodrexler.photomanager.application.usecase.asset;

import com.jpablodrexler.photomanager.domain.enums.AuditAction;
import com.jpablodrexler.photomanager.domain.enums.AuditEntityType;
import com.jpablodrexler.photomanager.domain.model.Asset;
import com.jpablodrexler.photomanager.domain.model.AuditEvent;
import com.jpablodrexler.photomanager.domain.port.in.asset.DownloadAssetsUseCase;
import com.jpablodrexler.photomanager.domain.port.out.AssetRepository;
import com.jpablodrexler.photomanager.domain.port.out.AuditLogRepository;
import com.jpablodrexler.photomanager.domain.port.out.StoragePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.OutputStream;
import java.time.Instant;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
@RequiredArgsConstructor
@Slf4j
public class DownloadAssetsUseCaseImpl implements DownloadAssetsUseCase {

    private final AssetRepository assetRepository;
    private final StoragePort storagePort;
    private final AuditLogRepository auditLogRepository;

    @Override
    @Transactional(readOnly = true)
    public void execute(List<Long> assetIds, OutputStream out, UUID userId) throws IOException {
        List<Asset> assets = assetRepository.findAllById(assetIds);

        Set<String> seenNames = new HashSet<>();
        Map<Long, String> entryNameByAssetId = new LinkedHashMap<>();
        for (Asset asset : assets) {
            String name = asset.getFileName();
            if (!seenNames.add(name)) {
                name = asset.getAssetId() + "_" + asset.getFileName();
            }
            entryNameByAssetId.put(asset.getAssetId(), name);
        }

        ZipOutputStream zipOut = new ZipOutputStream(out);
        try {
            for (Asset asset : assets) {
                String entryName = entryNameByAssetId.get(asset.getAssetId());
                try {
                    byte[] bytes = storagePort.readFileBytes(asset.getFullPath());
                    zipOut.putNextEntry(new ZipEntry(entryName));
                    zipOut.write(bytes);
                    zipOut.closeEntry();
                } catch (IOException e) {
                    log.warn("Skipping unreadable asset {}: {}", asset.getAssetId(), e.getMessage());
                }
            }
        } finally {
            zipOut.finish();
        }

        for (Asset asset : assets) {
            logAudit(asset.getAssetId(), userId);
        }
    }

    private void logAudit(Long assetId, UUID userId) {
        try {
            auditLogRepository.log(AuditEvent.builder()
                    .userId(userId)
                    .action(AuditAction.ASSET_DOWNLOADED)
                    .entityType(AuditEntityType.ASSET)
                    .entityId(String.valueOf(assetId))
                    .timestamp(Instant.now())
                    .build());
        } catch (Exception e) {
            log.warn("Failed to write audit log for AssetDownloaded assetId={}: {}", assetId, e.getMessage());
        }
    }
}
