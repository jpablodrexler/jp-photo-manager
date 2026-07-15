package com.jpablodrexler.photomanager.infrastructure.config;

import com.jpablodrexler.photomanager.domain.enums.AuditAction;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LegacyAuditActionReadingConverterTest {

    private final LegacyAuditActionReadingConverter sut = new LegacyAuditActionReadingConverter();

    @Test
    void convert_allLegacyPascalCaseNames_mapToRenamedConstants() {
        assertThat(sut.convert("AssetViewed")).isEqualTo(AuditAction.ASSET_VIEWED);
        assertThat(sut.convert("AssetDownloaded")).isEqualTo(AuditAction.ASSET_DOWNLOADED);
        assertThat(sut.convert("AssetTagged")).isEqualTo(AuditAction.ASSET_TAGGED);
        assertThat(sut.convert("AssetUntagged")).isEqualTo(AuditAction.ASSET_UNTAGGED);
        assertThat(sut.convert("AssetRated")).isEqualTo(AuditAction.ASSET_RATED);
        assertThat(sut.convert("AssetDeleted")).isEqualTo(AuditAction.ASSET_DELETED);
        assertThat(sut.convert("AssetCataloged")).isEqualTo(AuditAction.ASSET_CATALOGED);
        assertThat(sut.convert("CatalogRun")).isEqualTo(AuditAction.CATALOG_RUN);
        assertThat(sut.convert("SyncRun")).isEqualTo(AuditAction.SYNC_RUN);
        assertThat(sut.convert("ConvertRun")).isEqualTo(AuditAction.CONVERT_RUN);
    }

    @Test
    void convert_currentUpperSnakeCaseName_returnsSameConstant() {
        assertThat(sut.convert("ASSET_VIEWED")).isEqualTo(AuditAction.ASSET_VIEWED);
    }

    @Test
    void convert_unknownName_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> sut.convert("NotARealAction"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
