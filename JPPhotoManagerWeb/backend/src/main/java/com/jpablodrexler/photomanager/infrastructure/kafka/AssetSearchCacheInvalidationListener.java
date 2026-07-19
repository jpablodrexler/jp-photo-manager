package com.jpablodrexler.photomanager.infrastructure.kafka;

import com.jpablodrexler.photomanager.application.dto.AssetCatalogedEvent;
import com.jpablodrexler.photomanager.application.dto.AssetDeletedEvent;
import com.jpablodrexler.photomanager.domain.port.in.folder.GetFolderIdByPathUseCase;
import com.jpablodrexler.photomanager.domain.port.out.AssetSearchCachePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Evicts every {@code assets} cache entry belonging to a folder ({@code assets:{folderId}:*})
 * whenever that folder's asset list changes, keeping the paginated-search cache
 * (see {@code redis-search-tag-cache}) from serving stale results after a catalog or delete. The
 * actual cursor-based {@code SCAN}/{@code DEL} eviction lives in
 * {@link AssetSearchCachePort} (implemented by {@code AssetSearchCacheServiceAdapter}), shared with
 * the tag mutation use cases, which call the same port synchronously since a tag change can equally
 * invalidate a folder's tag-filtered search results.
 * Uses its own explicit, persistent consumer group ({@code asset-search-cache-invalidator}) —
 * the same pattern as {@link AuditLogKafkaListener} (`audit-log-writer`) — so exactly one backend
 * instance processes each event regardless of how many replicas are running.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AssetSearchCacheInvalidationListener {

    private static final String CONSUMER_GROUP = "asset-search-cache-invalidator";

    private final AssetSearchCachePort assetSearchCachePort;
    private final GetFolderIdByPathUseCase getFolderIdByPathUseCase;

    @KafkaListener(topics = "asset.cataloged", groupId = CONSUMER_GROUP,
            containerFactory = "kafkaListenerContainerFactory")
    public void onAssetCataloged(AssetCatalogedEvent event) {
        Long folderId = getFolderIdByPathUseCase.execute(event.folderPath());
        if (folderId == null) {
            log.debug("Skipping assets cache eviction: folder path '{}' could not be resolved to a folderId",
                    event.folderPath());
            return;
        }
        assetSearchCachePort.evictFolder(folderId);
    }

    @KafkaListener(topics = "asset.deleted", groupId = CONSUMER_GROUP,
            containerFactory = "kafkaListenerContainerFactory")
    public void onAssetDeleted(AssetDeletedEvent event) {
        assetSearchCachePort.evictFolder(event.folderId());
    }
}
