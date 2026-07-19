package com.jpablodrexler.photomanager.domain.model;

public record FilterPreset(
        String search,
        String dateFrom,
        String dateTo,
        Integer minRating
) {}
