package com.jpablodrexler.photomanager.domain.model;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record FilterPreset(
        String search,
        String dateFrom,
        String dateTo,
        Integer minRating
) {}
