package com.jpablodrexler.photomanager.application.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class PaginatedData<T> {

    private List<T> items;
    private int pageIndex;
    private int totalPages;
    private long totalItems;
}
