package com.jpablodrexler.photomanager.application.usecase.tag;

import com.jpablodrexler.photomanager.domain.enums.AuditAction;
import com.jpablodrexler.photomanager.domain.enums.AuditEntityType;
import com.jpablodrexler.photomanager.application.exception.TagNotFoundException;
import com.jpablodrexler.photomanager.domain.model.AuditEvent;
import com.jpablodrexler.photomanager.domain.model.Tag;
import com.jpablodrexler.photomanager.domain.port.in.tag.RemoveTagFromAssetUseCase;
import com.jpablodrexler.photomanager.domain.port.out.AssetRepository;
import com.jpablodrexler.photomanager.domain.port.out.AssetSearchCachePort;
import com.jpablodrexler.photomanager.domain.port.out.AuditLogRepository;
import com.jpablodrexler.photomanager.domain.port.out.TagRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RemoveTagFromAssetUseCaseImpl implements RemoveTagFromAssetUseCase {

    private final AssetRepository assetRepository;
    private final TagRepository tagRepository;
    private final AuditLogRepository auditLogRepository;
    private final AssetSearchCachePort assetSearchCachePort;

    @Override
    @Transactional
    @CacheEvict(cacheNames = "tags", key = "'all'")
    public void execute(Long assetId, String name, UUID userId) {
        String normalized = name.toLowerCase(Locale.ROOT).trim();
        Tag tag = tagRepository.findByName(normalized)
                .orElseThrow(() -> new TagNotFoundException(normalized));

        int removed = assetRepository.removeTagFromAsset(assetId, tag.getTagId());
        if (removed == 0) {
            throw new NoSuchElementException("Tag '" + normalized + "' is not assigned to asset " + assetId);
        }

        if (!tagRepository.isUsedByOtherAssets(tag.getTagId(), assetId)) {
            tagRepository.deleteById(tag.getTagId());
        }

        // A tag change can alter this folder's tag-filtered "assets" search-cache entries just as
        // much as a catalog/delete event does, but no Kafka event exists for tag mutations — evict
        // synchronously here instead of leaving stale results until the 5-minute TTL expires.
        assetRepository.findById(assetId).ifPresent(asset ->
                assetSearchCachePort.evictFolder(asset.getFolder() != null ? asset.getFolder().getFolderId() : null));

        logAudit(assetId, tag, userId);
    }

    private void logAudit(Long assetId, Tag tag, UUID userId) {
        try {
            Map<String, Object> metadata = new LinkedHashMap<>();
            metadata.put("tagName", tag.getName());
            metadata.put("tagId", tag.getTagId());
            auditLogRepository.log(AuditEvent.builder()
                    .userId(userId)
                    .action(AuditAction.ASSET_UNTAGGED)
                    .entityType(AuditEntityType.ASSET)
                    .entityId(String.valueOf(assetId))
                    .timestamp(Instant.now())
                    .metadata(metadata)
                    .build());
        } catch (Exception e) {
            log.warn("Failed to write audit log for AssetUntagged assetId={}: {}", assetId, e.getMessage());
        }
    }
}
