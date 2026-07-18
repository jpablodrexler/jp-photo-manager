package com.jpablodrexler.photomanager.domain.port.out;

/**
 * Evicts the {@code assets} search-results cache (see {@code redis-search-tag-cache}) for a given
 * folder. Any change that can alter a folder's paginated asset search results — cataloging,
 * deletion, or a tag add/remove/bulk-add/bulk-remove on one of its assets — must call this so a
 * stale cached page is never served after the underlying data changes.
 */
public interface AssetSearchCachePort {

    /**
     * Evicts every cached search-result entry for the given folder. A {@code null} folderId is a
     * no-op (nothing to evict).
     */
    void evictFolder(Long folderId);
}
