package com.jpablodrexler.photomanager.application.usecase.home;

import com.jpablodrexler.photomanager.application.dto.AssetSummaryDto;
import com.jpablodrexler.photomanager.application.dto.FolderStat;
import com.jpablodrexler.photomanager.application.dto.HomeStats;
import com.jpablodrexler.photomanager.domain.port.in.home.GetHomeStatsUseCase;
import com.jpablodrexler.photomanager.domain.port.out.AssetRepository;
import com.jpablodrexler.photomanager.domain.port.out.FolderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GetHomeStatsUseCaseImpl implements GetHomeStatsUseCase {

    private final FolderRepository folderRepository;
    private final AssetRepository assetRepository;
    private final JobExplorer jobExplorer;

    @Override
    @Transactional(readOnly = true)
    public HomeStats execute() {
        long folderCount = folderRepository.count();
        long assetCount = assetRepository.count();
        long totalFileSize = assetRepository.sumFileSize();
        long duplicateCount = assetRepository.countDuplicates();
        List<FolderStat> topFolders = assetRepository.findTopFoldersByAssetCount(5);
        List<AssetSummaryDto> recentAssets = assetRepository.findRecentAssets(12)
                .stream()
                .map(a -> new AssetSummaryDto(
                        a.getAssetId(),
                        a.getFileName(),
                        a.getFolder().getPath(),
                        "/api/assets/" + a.getAssetId() + "/thumbnail"))
                .toList();
        Instant lastCatalogCompletedAt = findLastCatalogCompletedAt();
        return new HomeStats(folderCount, assetCount, lastCatalogCompletedAt, totalFileSize, duplicateCount, topFolders, recentAssets);
    }

    private Instant findLastCatalogCompletedAt() {
        return jobExplorer.getJobInstances("catalogJob", 0, 20)
                .stream()
                .flatMap(instance -> jobExplorer.getJobExecutions(instance).stream())
                .filter(e -> e.getStatus() == BatchStatus.COMPLETED && e.getEndTime() != null)
                .max(Comparator.comparing(e -> e.getEndTime()))
                .map(e -> e.getEndTime().toInstant(ZoneOffset.UTC))
                .orElse(null);
    }
}
