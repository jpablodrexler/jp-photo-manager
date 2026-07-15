package com.jpablodrexler.photomanager.application.usecase.tag;

import com.jpablodrexler.photomanager.domain.enums.AuditAction;
import com.jpablodrexler.photomanager.domain.enums.AuditEntityType;
import com.jpablodrexler.photomanager.domain.model.AuditEvent;
import com.jpablodrexler.photomanager.domain.model.Tag;
import com.jpablodrexler.photomanager.domain.port.in.tag.BulkRemoveTagUseCase;
import com.jpablodrexler.photomanager.domain.port.out.AssetRepository;
import com.jpablodrexler.photomanager.domain.port.out.AuditLogRepository;
import com.jpablodrexler.photomanager.domain.port.out.TagRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class BulkRemoveTagUseCaseImpl implements BulkRemoveTagUseCase {

    private final AssetRepository assetRepository;
    private final TagRepository tagRepository;
    private final AuditLogRepository auditLogRepository;

    @Override
    @Transactional
    public void execute(List<Long> assetIds, String name, UUID userId) {
        String normalized = name.toLowerCase(Locale.ROOT).trim();
        Optional<Tag> tagOpt = tagRepository.findByName(normalized);
        if (tagOpt.isEmpty()) {
            return;
        }
        Tag tag = tagOpt.get();

        for (Long assetId : assetIds) {
            assetRepository.removeTagFromAsset(assetId, tag.getTagId());
            logAudit(assetId, tag, userId);
        }

        // Delete the tag if no assets reference it after the bulk removal
        if (!tagRepository.isUsedByOtherAssets(tag.getTagId(), -1L)) {
            tagRepository.deleteById(tag.getTagId());
        }
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
