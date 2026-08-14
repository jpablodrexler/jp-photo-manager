package com.jpablodrexler.photomanager.infrastructure.persistence.jpa;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AssetSummaryTest {

    @Test
    void getThumbnailUrl_buildsUrlFromAssetId() {
        AssetSummary sut = mock(AssetSummary.class);
        when(sut.getAssetId()).thenReturn(42L);
        when(sut.getThumbnailUrl()).thenCallRealMethod();

        assertThat(sut.getThumbnailUrl()).isEqualTo("/api/assets/42/thumbnail");
    }
}
