package com.jpablodrexler.photomanager.domain.model;

import java.util.List;

public record PaginatedResult<T>(List<T> items, long total, int page, int pageSize) {

    public int totalPages() {
        return pageSize > 0 ? (int) Math.ceil((double) total / pageSize) : 0;
    }
}
