package com.jpablodrexler.photomanager.application.dto;

import java.util.List;

public record PaginatedResult<T>(List<T> items, long total, int page, int pageSize) {}
