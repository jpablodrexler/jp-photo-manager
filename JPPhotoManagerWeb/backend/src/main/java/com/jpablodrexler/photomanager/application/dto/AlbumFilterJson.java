package com.jpablodrexler.photomanager.application.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AlbumFilterJson(
        String search,
        String dateFrom,
        String dateTo,
        Integer minRating
) {}
