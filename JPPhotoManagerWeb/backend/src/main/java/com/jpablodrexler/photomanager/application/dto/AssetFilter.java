package com.jpablodrexler.photomanager.application.dto;

import com.jpablodrexler.photomanager.domain.enums.SortCriteria;

import java.time.LocalDate;
import java.util.Set;

public record AssetFilter(
        Long folderId,
        String search,
        LocalDate dateFrom,
        LocalDate dateTo,
        Integer minRating,
        SortCriteria sortCriteria,
        int page,
        int pageSize,
        boolean includeDeleted,
        Set<String> tags
) {}
