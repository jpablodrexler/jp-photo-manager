package com.jpablodrexler.photomanager.application.usecase.asset;

import com.jpablodrexler.photomanager.application.dto.AssetFilter;
import com.jpablodrexler.photomanager.application.dto.PaginatedResult;
import com.jpablodrexler.photomanager.domain.model.Asset;
import com.jpablodrexler.photomanager.domain.model.TimelineGroup;
import com.jpablodrexler.photomanager.domain.port.in.asset.GetAssetsTimelineUseCase;
import com.jpablodrexler.photomanager.domain.port.out.AssetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class GetAssetsTimelineUseCaseImpl implements GetAssetsTimelineUseCase {

    static final int TIMELINE_PAGE_SIZE = 30;
    private static final DateTimeFormatter LABEL_FORMATTER = DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.ENGLISH);

    private final AssetRepository assetRepository;

    @Override
    @Transactional(readOnly = true)
    public PaginatedResult<TimelineGroup> execute(AssetFilter filter) {
        List<Asset> assets = assetRepository.findAllFilteredSortedByDateDesc(filter);

        Map<LocalDate, List<Asset>> grouped = new LinkedHashMap<>();
        for (Asset asset : assets) {
            LocalDate date = asset.getFileCreationDateTime() != null
                    ? asset.getFileCreationDateTime().toLocalDate()
                    : LocalDate.EPOCH;
            grouped.computeIfAbsent(date, k -> new ArrayList<>()).add(asset);
        }

        List<TimelineGroup> allGroups = new ArrayList<>();
        for (Map.Entry<LocalDate, List<Asset>> entry : grouped.entrySet()) {
            LocalDate date = entry.getKey();
            String label = date.format(LABEL_FORMATTER);
            allGroups.add(new TimelineGroup(date, label, entry.getValue()));
        }

        int page = filter.page();
        int fromIdx = page * TIMELINE_PAGE_SIZE;
        int toIdx = Math.min(fromIdx + TIMELINE_PAGE_SIZE, allGroups.size());
        List<TimelineGroup> pageGroups = fromIdx < allGroups.size()
                ? allGroups.subList(fromIdx, toIdx)
                : List.of();

        return new PaginatedResult<>(pageGroups, allGroups.size(), page, TIMELINE_PAGE_SIZE);
    }
}
