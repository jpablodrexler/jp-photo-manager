package com.jpablodrexler.photomanager.application.usecase.analytics;

import com.jpablodrexler.photomanager.domain.model.AnalyticsData;
import com.jpablodrexler.photomanager.domain.model.FolderStorageEntry;
import com.jpablodrexler.photomanager.domain.model.FormatEntry;
import com.jpablodrexler.photomanager.domain.port.in.analytics.GetAnalyticsUseCase;
import com.jpablodrexler.photomanager.domain.port.out.AssetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GetAnalyticsUseCaseImpl implements GetAnalyticsUseCase {

    private static final int MAX_FOLDERS = 20;

    private final AssetRepository assetRepository;

    @Override
    @Transactional(readOnly = true)
    public AnalyticsData execute() {
        List<FolderStorageEntry> folderStorage = capFolderStorage(assetRepository.sumFileSizeByFolder());
        List<FormatEntry> formatDistribution = normaliseExtensions(assetRepository.countByExtension());

        return AnalyticsData.builder()
                .folderStorage(folderStorage)
                .formatDistribution(formatDistribution)
                .photosPerMonth(assetRepository.countByCreationMonth())
                .ratingDistribution(assetRepository.countByRating())
                .build();
    }

    private List<FolderStorageEntry> capFolderStorage(List<FolderStorageEntry> raw) {
        if (raw.size() <= MAX_FOLDERS) {
            return raw;
        }
        List<FolderStorageEntry> top = new ArrayList<>(raw.subList(0, MAX_FOLDERS));
        long otherBytes = raw.subList(MAX_FOLDERS, raw.size()).stream()
                .mapToLong(FolderStorageEntry::bytes)
                .sum();
        top.add(new FolderStorageEntry("other", otherBytes));
        return top;
    }

    private List<FormatEntry> normaliseExtensions(List<FormatEntry> raw) {
        return raw.stream()
                .map(e -> (e.extension() == null || e.extension().isBlank())
                        ? new FormatEntry("unknown", e.count())
                        : e)
                .toList();
    }
}
