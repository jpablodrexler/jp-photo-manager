package com.jpablodrexler.photomanager.infrastructure.web.dto.request;

import com.jpablodrexler.photomanager.application.dto.AlbumFilterJson;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;

public record CreateAlbumRequestDto(
        @NotBlank String name,
        String description,
        AlbumFilterJson filterJson
) {
    @AssertTrue(message = "filterJson must have at least one non-null field when provided")
    public boolean isFilterJsonValid() {
        return filterJson == null
                || filterJson.search() != null
                || filterJson.dateFrom() != null
                || filterJson.dateTo() != null
                || filterJson.minRating() != null;
    }
}
