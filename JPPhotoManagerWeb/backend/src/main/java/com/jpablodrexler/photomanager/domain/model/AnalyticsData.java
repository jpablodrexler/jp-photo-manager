package com.jpablodrexler.photomanager.domain.model;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class AnalyticsData {
    List<FolderStorageEntry> folderStorage;
    List<FormatEntry> formatDistribution;
    List<MonthlyCountEntry> photosPerMonth;
    List<RatingEntry> ratingDistribution;
}
