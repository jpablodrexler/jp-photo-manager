package com.jpablodrexler.photomanager.infrastructure.kafka;

import com.jpablodrexler.photomanager.application.dto.AssetCatalogedEvent;
import com.jpablodrexler.photomanager.application.dto.AssetDeletedEvent;
import com.jpablodrexler.photomanager.application.dto.CatalogProgressMessage;
import com.jpablodrexler.photomanager.application.dto.ConvertAssetsResult;
import com.jpablodrexler.photomanager.application.dto.ConvertProgressMessage;
import com.jpablodrexler.photomanager.application.dto.SyncAssetsResult;
import com.jpablodrexler.photomanager.application.dto.SyncProgressMessage;
import com.jpablodrexler.photomanager.domain.enums.AuditAction;
import com.jpablodrexler.photomanager.domain.enums.AuditEntityType;
import com.jpablodrexler.photomanager.domain.model.AuditEvent;
import com.jpablodrexler.photomanager.domain.port.out.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Durably persists already-published lifecycle and job-completion events to the audit trail.
 * Uses a dedicated, explicit consumer group ({@code audit-log-writer}) rather than the per-instance
 * group used by {@link KafkaProgressListener} so that each event is written exactly once across
 * all running application instances (Kafka's consumer-group partition assignment guarantees this).
 * Intermediate ({@code done=false}) progress messages are ignored; only the final completion
 * message per run produces an audit entry.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AuditLogKafkaListener {

    private static final String CONSUMER_GROUP = "audit-log-writer";

    private final AuditLogRepository auditLogRepository;

    @KafkaListener(topics = "asset.cataloged", groupId = CONSUMER_GROUP,
            containerFactory = "kafkaListenerContainerFactory")
    public void onAssetCataloged(AssetCatalogedEvent event) {
        writeAuditEvent(AuditEvent.builder()
                .action(AuditAction.ASSET_CATALOGED)
                .entityType(AuditEntityType.ASSET)
                .entityId(String.valueOf(event.assetId()))
                .timestamp(event.timestamp() != null ? event.timestamp() : Instant.now())
                .build());
    }

    @KafkaListener(topics = "asset.deleted", groupId = CONSUMER_GROUP,
            containerFactory = "kafkaListenerContainerFactory")
    public void onAssetDeleted(AssetDeletedEvent event) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("folderId", event.folderId());
        metadata.put("permanent", event.permanent());

        writeAuditEvent(AuditEvent.builder()
                .action(AuditAction.ASSET_DELETED)
                .entityType(AuditEntityType.ASSET)
                .entityId(String.valueOf(event.assetId()))
                .timestamp(event.timestamp() != null ? event.timestamp() : Instant.now())
                .metadata(metadata)
                .build());
    }

    @KafkaListener(topics = "job.catalog.progress", groupId = CONSUMER_GROUP,
            containerFactory = "kafkaListenerContainerFactory")
    public void onCatalogProgress(CatalogProgressMessage message) {
        if (!message.done()) {
            return;
        }

        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("foldersScanned", message.foldersScanned());
        metadata.put("assetsAdded", message.assetsAdded());
        metadata.put("durationMs", message.durationMs());

        writeAuditEvent(AuditEvent.builder()
                .action(AuditAction.CATALOG_RUN)
                .entityType(AuditEntityType.CATALOG_RUN)
                .entityId(String.valueOf(message.runId()))
                .timestamp(Instant.now())
                .metadata(metadata)
                .build());
    }

    @KafkaListener(topics = "job.sync.progress", groupId = CONSUMER_GROUP,
            containerFactory = "kafkaListenerContainerFactory")
    public void onSyncProgress(SyncProgressMessage message) {
        if (!message.done()) {
            return;
        }

        writeAuditEvent(AuditEvent.builder()
                .action(AuditAction.SYNC_RUN)
                .entityType(AuditEntityType.SYNC_RUN)
                .entityId(String.valueOf(message.runId()))
                .timestamp(Instant.now())
                .metadata(buildSyncMetadata(message.results()))
                .build());
    }

    @KafkaListener(topics = "job.convert.progress", groupId = CONSUMER_GROUP,
            containerFactory = "kafkaListenerContainerFactory")
    public void onConvertProgress(ConvertProgressMessage message) {
        if (!message.done()) {
            return;
        }

        writeAuditEvent(AuditEvent.builder()
                .action(AuditAction.CONVERT_RUN)
                .entityType(AuditEntityType.CONVERT_RUN)
                .entityId(String.valueOf(message.runId()))
                .timestamp(Instant.now())
                .metadata(buildConvertMetadata(message.results()))
                .build());
    }

    private Map<String, Object> buildSyncMetadata(List<SyncAssetsResult> results) {
        if (results == null || results.isEmpty()) {
            return Map.of();
        }
        String sourceDir = results.stream()
                .map(SyncAssetsResult::getSourceDirectory)
                .distinct()
                .collect(Collectors.joining(";"));
        String targetDir = results.stream()
                .map(SyncAssetsResult::getDestinationDirectory)
                .distinct()
                .collect(Collectors.joining(";"));
        int filesCopied = results.stream().mapToInt(SyncAssetsResult::getSyncedCount).sum();
        int filesDeleted = results.stream().mapToInt(SyncAssetsResult::getDeletedCount).sum();

        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("sourceDir", sourceDir);
        metadata.put("targetDir", targetDir);
        metadata.put("filesCopied", filesCopied);
        metadata.put("filesDeleted", filesDeleted);
        return metadata;
    }

    private Map<String, Object> buildConvertMetadata(List<ConvertAssetsResult> results) {
        if (results == null || results.isEmpty()) {
            return Map.of();
        }
        String sourceDir = results.stream()
                .map(ConvertAssetsResult::getSourceDirectory)
                .distinct()
                .collect(Collectors.joining(";"));
        String targetDir = results.stream()
                .map(ConvertAssetsResult::getDestinationDirectory)
                .distinct()
                .collect(Collectors.joining(";"));
        int filesConverted = results.stream().mapToInt(ConvertAssetsResult::getConvertedCount).sum();

        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("sourceDir", sourceDir);
        metadata.put("targetDir", targetDir);
        metadata.put("filesConverted", filesConverted);
        return metadata;
    }

    private void writeAuditEvent(AuditEvent event) {
        try {
            auditLogRepository.log(event);
        } catch (Exception e) {
            log.warn("Failed to write audit log entry for action={} entityId={}: {}",
                    event.getAction(), event.getEntityId(), e.getMessage());
        }
    }
}
