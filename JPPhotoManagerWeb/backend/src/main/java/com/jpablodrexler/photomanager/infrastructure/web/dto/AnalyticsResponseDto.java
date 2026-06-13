package com.jpablodrexler.photomanager.infrastructure.web.dto;

import java.util.List;

public record AnalyticsResponseDto(
        List<FolderStorageEntryDto> folderStorage,
        List<FormatEntryDto> formatDistribution,
        List<MonthlyCountEntryDto> photosPerMonth,
        List<RatingEntryDto> ratingDistribution
) {
    public record FolderStorageEntryDto(String folderPath, long bytes) {}
    public record FormatEntryDto(String extension, long count) {}
    public record MonthlyCountEntryDto(String month, long count) {}
    public record RatingEntryDto(int rating, long count) {}
}
