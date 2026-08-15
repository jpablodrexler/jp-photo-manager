package com.jpablodrexler.photomanager.infrastructure.web.dto.request;

import com.jpablodrexler.photomanager.application.dto.AlbumFilterJson;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UpdateAlbumRequestDtoTest {

    @Test
    void isFilterJsonValid_nullFilterJson_returnsTrue() {
        UpdateAlbumRequestDto dto = new UpdateAlbumRequestDto("Trip", "desc", null);

        assertThat(dto.isFilterJsonValid()).isTrue();
    }

    @Test
    void isFilterJsonValid_filterJsonWithSearchOnly_returnsTrue() {
        UpdateAlbumRequestDto dto = new UpdateAlbumRequestDto("Trip", "desc",
                new AlbumFilterJson("sunset", null, null, null));

        assertThat(dto.isFilterJsonValid()).isTrue();
    }

    @Test
    void isFilterJsonValid_filterJsonWithDateFromOnly_returnsTrue() {
        UpdateAlbumRequestDto dto = new UpdateAlbumRequestDto("Trip", "desc",
                new AlbumFilterJson(null, "2024-01-01", null, null));

        assertThat(dto.isFilterJsonValid()).isTrue();
    }

    @Test
    void isFilterJsonValid_filterJsonWithDateToOnly_returnsTrue() {
        UpdateAlbumRequestDto dto = new UpdateAlbumRequestDto("Trip", "desc",
                new AlbumFilterJson(null, null, "2024-12-31", null));

        assertThat(dto.isFilterJsonValid()).isTrue();
    }

    @Test
    void isFilterJsonValid_filterJsonWithMinRatingOnly_returnsTrue() {
        UpdateAlbumRequestDto dto = new UpdateAlbumRequestDto("Trip", "desc",
                new AlbumFilterJson(null, null, null, 3));

        assertThat(dto.isFilterJsonValid()).isTrue();
    }

    @Test
    void isFilterJsonValid_filterJsonWithAllFieldsNull_returnsFalse() {
        UpdateAlbumRequestDto dto = new UpdateAlbumRequestDto("Trip", "desc",
                new AlbumFilterJson(null, null, null, null));

        assertThat(dto.isFilterJsonValid()).isFalse();
    }
}
