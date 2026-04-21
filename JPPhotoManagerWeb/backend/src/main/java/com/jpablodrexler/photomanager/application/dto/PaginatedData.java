package com.jpablodrexler.photomanager.application.dto;

import lombok.Data;

import java.util.List;

@Data
public class PaginatedData<T> {

    private List<T> items;
    private int pageIndex;
    private int totalPages;
    private long totalItems;

    public PaginatedData(List<T> items, int pageIndex, int totalPages, long totalItems) {
        this.items = items;
        this.pageIndex = pageIndex;
        this.totalPages = totalPages;
        this.totalItems = totalItems;
    }
}
