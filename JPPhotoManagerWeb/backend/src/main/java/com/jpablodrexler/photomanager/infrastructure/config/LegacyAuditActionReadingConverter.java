package com.jpablodrexler.photomanager.infrastructure.config;

import com.jpablodrexler.photomanager.domain.enums.AuditAction;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;

import java.util.Map;

/**
 * {@code AuditAction}'s enum constants were renamed from PascalCase (e.g. {@code AssetViewed}) to
 * UPPER_SNAKE_CASE (e.g. {@code ASSET_VIEWED}). Spring Data MongoDB serializes enums by
 * {@code name()}, so without this converter historical {@code asset_audit_log} documents written
 * under the old names would fail to deserialize.
 */
@ReadingConverter
public class LegacyAuditActionReadingConverter implements Converter<String, AuditAction> {

    private static final Map<String, AuditAction> LEGACY_NAMES = Map.ofEntries(
            Map.entry("AssetViewed", AuditAction.ASSET_VIEWED),
            Map.entry("AssetDownloaded", AuditAction.ASSET_DOWNLOADED),
            Map.entry("AssetTagged", AuditAction.ASSET_TAGGED),
            Map.entry("AssetUntagged", AuditAction.ASSET_UNTAGGED),
            Map.entry("AssetRated", AuditAction.ASSET_RATED),
            Map.entry("AssetDeleted", AuditAction.ASSET_DELETED),
            Map.entry("AssetCataloged", AuditAction.ASSET_CATALOGED),
            Map.entry("CatalogRun", AuditAction.CATALOG_RUN),
            Map.entry("SyncRun", AuditAction.SYNC_RUN),
            Map.entry("ConvertRun", AuditAction.CONVERT_RUN)
    );

    @Override
    public AuditAction convert(String source) {
        AuditAction legacy = LEGACY_NAMES.get(source);
        return legacy != null ? legacy : AuditAction.valueOf(source);
    }
}
