package com.jpablodrexler.photomanager.application.usecase.album;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jpablodrexler.photomanager.domain.enums.SortCriteria;
import com.jpablodrexler.photomanager.domain.model.AssetFilter;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AlbumAssetFilterFactoryTest {

    private final AlbumAssetFilterFactory sut = new AlbumAssetFilterFactory(new ObjectMapper());

    @Test
    void build_fullFilterJson_returnsPopulatedAssetFilter() {
        String json = "{\"search\":\"sunset\",\"dateFrom\":\"2024-01-01\",\"dateTo\":\"2024-12-31\",\"minRating\":4}";

        AssetFilter result = sut.build(json, 0, 20);

        assertThat(result.folderId()).isNull();
        assertThat(result.search()).isEqualTo("sunset");
        assertThat(result.dateFrom()).isEqualTo(LocalDate.of(2024, 1, 1));
        assertThat(result.dateTo()).isEqualTo(LocalDate.of(2024, 12, 31));
        assertThat(result.minRating()).isEqualTo(4);
        assertThat(result.sortCriteria()).isEqualTo(SortCriteria.FILE_NAME);
        assertThat(result.page()).isZero();
        assertThat(result.pageSize()).isEqualTo(20);
        assertThat(result.includeDeleted()).isFalse();
        assertThat(result.tags()).isEmpty();
    }

    @Test
    void build_filterJsonWithNullDates_returnsNullDates() {
        String json = "{\"search\":\"beach\"}";

        AssetFilter result = sut.build(json, 1, 10);

        assertThat(result.dateFrom()).isNull();
        assertThat(result.dateTo()).isNull();
        assertThat(result.search()).isEqualTo("beach");
        assertThat(result.page()).isEqualTo(1);
        assertThat(result.pageSize()).isEqualTo(10);
    }

    @Test
    void build_invalidJson_throwsRuntimeException() {
        assertThatThrownBy(() -> sut.build("{not valid json", 0, 10))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to parse filterJson");
    }
}
