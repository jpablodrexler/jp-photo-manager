package com.jpablodrexler.photomanager.application.usecase.home;

import com.jpablodrexler.photomanager.domain.model.AssetSummary;
import com.jpablodrexler.photomanager.domain.model.FolderStat;
import com.jpablodrexler.photomanager.domain.model.HomeStats;
import com.jpablodrexler.photomanager.domain.port.in.home.GetHomeStatsUseCase;
import com.jpablodrexler.photomanager.domain.port.out.AssetRepository;
import com.jpablodrexler.photomanager.domain.port.out.CatalogRunHistoryPort;
import com.jpablodrexler.photomanager.domain.port.out.FolderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GetHomeStatsUseCaseImpl implements GetHomeStatsUseCase {

    private final FolderRepository folderRepository;
    private final AssetRepository assetRepository;
    private final CatalogRunHistoryPort catalogRunHistoryPort;

    @Override
    @Transactional(readOnly = true)
    @Cacheable("home-stats")
    public HomeStats execute() {
        long folderCount = folderRepository.count();
        long assetCount = assetRepository.count();
        long totalFileSize = assetRepository.sumFileSize();
        long duplicateCount = assetRepository.countDuplicates();
        List<FolderStat> topFolders = assetRepository.findTopFoldersByAssetCount(5);
        List<AssetSummary> recentAssets = assetRepository.findRecentAssets(12)
                .stream()
                .map(a -> new AssetSummary(
                        a.getAssetId(),
                        a.getFileName(),
                        a.getFolder().getPath(),
                        a.getThumbnailUrl(),
                        a.getFileSize()))
                .toList();
        Instant lastCatalogCompletedAt = catalogRunHistoryPort.findLastCompletedCatalogRunTime().orElse(null);
        return new HomeStats(folderCount, assetCount, lastCatalogCompletedAt, totalFileSize, duplicateCount, topFolders, recentAssets);
    }
}
