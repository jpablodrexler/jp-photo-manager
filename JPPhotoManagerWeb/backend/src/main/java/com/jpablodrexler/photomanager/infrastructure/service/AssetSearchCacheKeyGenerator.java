package com.jpablodrexler.photomanager.infrastructure.service;

import com.jpablodrexler.photomanager.domain.model.AssetFilter;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Set;
import java.util.TreeSet;

/**
 * Builds the {@code assets} cache key for {@code GetAssetsUseCaseImpl.execute(AssetFilter)} as
 * {@code {folderId}:{sha256Hex(remaining filter fields)}}. The {@code folderId} segment is kept as
 * a visible literal prefix (rather than folded into the hash) so
 * {@code AssetSearchCacheInvalidationListener} can evict every entry belonging to one folder with a
 * {@code SCAN MATCH "assets:{folderId}:*"} without touching any other folder's entries. Every other
 * filter field (search, date range, rating, sort, paging, tags) is hashed together, since each
 * distinct combination is a distinct result set that must not collide with another.
 */
@Component("assetSearchCacheKeyGenerator")
public class AssetSearchCacheKeyGenerator implements KeyGenerator {

    private static final String FIELD_DELIMITER = "|";
    private static final String NO_FOLDER = "none";

    @Override
    public Object generate(Object target, Method method, Object... params) {
        AssetFilter filter = (AssetFilter) params[0];
        String folderSegment = filter.folderId() != null ? String.valueOf(filter.folderId()) : NO_FOLDER;
        return folderSegment + ":" + sha256Hex(buildHashInput(filter));
    }

    private String buildHashInput(AssetFilter filter) {
        return String.join(FIELD_DELIMITER,
                nullToEmpty(filter.search()),
                nullToEmpty(filter.dateFrom()),
                nullToEmpty(filter.dateTo()),
                nullToEmpty(filter.minRating()),
                nullToEmpty(filter.sortCriteria()),
                String.valueOf(filter.page()),
                String.valueOf(filter.pageSize()),
                String.valueOf(filter.includeDeleted()),
                sortedTags(filter.tags()));
    }

    private String sortedTags(Set<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return "";
        }
        return String.join(",", new TreeSet<>(tags));
    }

    private String nullToEmpty(Object value) {
        return value == null ? "" : value.toString();
    }

    private String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                hex.append(Character.forDigit((b >> 4) & 0xF, 16));
                hex.append(Character.forDigit(b & 0xF, 16));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is a mandatory JDK algorithm (JLS/JCA baseline); this can never happen at runtime.
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }
}
