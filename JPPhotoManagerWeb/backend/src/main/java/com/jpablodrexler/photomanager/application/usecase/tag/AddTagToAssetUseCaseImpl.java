package com.jpablodrexler.photomanager.application.usecase.tag;

import com.jpablodrexler.photomanager.domain.enums.AuditAction;
import com.jpablodrexler.photomanager.domain.enums.AuditEntityType;
import com.jpablodrexler.photomanager.application.exception.AssetNotFoundException;
import com.jpablodrexler.photomanager.domain.model.AuditEvent;
import com.jpablodrexler.photomanager.domain.model.Tag;
import com.jpablodrexler.photomanager.domain.port.in.tag.AddTagToAssetUseCase;
import com.jpablodrexler.photomanager.domain.port.out.AssetRepository;
import com.jpablodrexler.photomanager.domain.port.out.AuditLogRepository;
import com.jpablodrexler.photomanager.domain.port.out.TagRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AddTagToAssetUseCaseImpl implements AddTagToAssetUseCase {

    private final AssetRepository assetRepository;
    private final TagRepository tagRepository;
    private final AuditLogRepository auditLogRepository;

    @Override
    @Transactional
    public void execute(Long assetId, String name, UUID userId) {
        if (!assetRepository.existsById(assetId)) {
            throw new AssetNotFoundException(assetId);
        }

        String normalized = name.toLowerCase(Locale.ROOT).trim();
        Tag tag = tagRepository.findByName(normalized)
                .orElseGet(() -> tagRepository.save(Tag.builder().name(normalized).build()));

        assetRepository.addTagToAsset(assetId, tag.getTagId());

        logAudit(assetId, tag, userId);
    }

    private void logAudit(Long assetId, Tag tag, UUID userId) {
        try {
            Map<String, Object> metadata = new LinkedHashMap<>();
            metadata.put("tagName", tag.getName());
            metadata.put("tagId", tag.getTagId());
            auditLogRepository.log(AuditEvent.builder()
                    .userId(userId)
                    .action(AuditAction.ASSET_TAGGED)
                    .entityType(AuditEntityType.ASSET)
                    .entityId(String.valueOf(assetId))
                    .timestamp(Instant.now())
                    .metadata(metadata)
                    .build());
        } catch (Exception e) {
            log.warn("Failed to write audit log for AssetTagged assetId={}: {}", assetId, e.getMessage());
        }
    }
}
