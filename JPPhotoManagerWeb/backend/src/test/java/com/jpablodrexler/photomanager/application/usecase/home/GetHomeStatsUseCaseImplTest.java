package com.jpablodrexler.photomanager.application.usecase.home;

import com.jpablodrexler.photomanager.domain.model.AssetSummaryDto;
import com.jpablodrexler.photomanager.domain.model.FolderStat;
import com.jpablodrexler.photomanager.domain.model.HomeStats;
import com.jpablodrexler.photomanager.domain.model.Asset;
import com.jpablodrexler.photomanager.domain.model.Folder;
import com.jpablodrexler.photomanager.domain.port.out.AssetRepository;
import com.jpablodrexler.photomanager.domain.port.out.FolderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.explore.JobExplorer;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetHomeStatsUseCaseImplTest {

    @Mock FolderRepository folderRepository;
    @Mock AssetRepository assetRepository;
    @Mock JobExplorer jobExplorer;
    @InjectMocks GetHomeStatsUseCaseImpl sut;

    @Test
    void execute_withCompletedJob_returnsLastCatalogCompletedAt() {
        LocalDateTime completedAt = LocalDateTime.of(2024, 6, 1, 10, 0, 0);
        JobInstance instance = new JobInstance(1L, "catalogJob");
        JobExecution execution = new JobExecution(instance, null);
        execution.setStatus(BatchStatus.COMPLETED);
        execution.setEndTime(completedAt);
        when(jobExplorer.getJobInstances("catalogJob", 0, 20)).thenReturn(List.of(instance));
        when(jobExplorer.getJobExecutions(instance)).thenReturn(List.of(execution));
        stubCommonRepositoryCalls(5L, 200L);

        HomeStats result = sut.execute();

        assertThat(result.folderCount()).isEqualTo(5L);
        assertThat(result.assetCount()).isEqualTo(200L);
        assertThat(result.lastCatalogCompletedAt()).isNotNull();
    }

    @Test
    void execute_withNoCompletedJob_returnsNullLastCatalogDate() {
        when(jobExplorer.getJobInstances("catalogJob", 0, 20)).thenReturn(List.of());
        stubCommonRepositoryCalls(0L, 0L);

        HomeStats result = sut.execute();

        assertThat(result.lastCatalogCompletedAt()).isNull();
    }

    @Test
    void execute_withAssets_returnsTotalFileSize() {
        when(jobExplorer.getJobInstances("catalogJob", 0, 20)).thenReturn(List.of());
        when(folderRepository.count()).thenReturn(2L);
        when(assetRepository.count()).thenReturn(10L);
        when(assetRepository.sumFileSize()).thenReturn(5_000_000L);
        when(assetRepository.countDuplicates()).thenReturn(0L);
        when(assetRepository.findTopFoldersByAssetCount(5)).thenReturn(List.of());
        when(assetRepository.findRecentAssets(12)).thenReturn(List.of());

        HomeStats result = sut.execute();

        assertThat(result.totalFileSize()).isEqualTo(5_000_000L);
    }

    @Test
    void execute_withDuplicates_returnsDuplicateCount() {
        when(jobExplorer.getJobInstances("catalogJob", 0, 20)).thenReturn(List.of());
        when(folderRepository.count()).thenReturn(1L);
        when(assetRepository.count()).thenReturn(5L);
        when(assetRepository.sumFileSize()).thenReturn(0L);
        when(assetRepository.countDuplicates()).thenReturn(3L);
        when(assetRepository.findTopFoldersByAssetCount(5)).thenReturn(List.of());
        when(assetRepository.findRecentAssets(12)).thenReturn(List.of());

        HomeStats result = sut.execute();

        assertThat(result.duplicateCount()).isEqualTo(3L);
    }

    @Test
    void execute_withTopFolders_returnsTopFoldersList() {
        when(jobExplorer.getJobInstances("catalogJob", 0, 20)).thenReturn(List.of());
        List<FolderStat> topFolders = List.of(
                new FolderStat("/photos/vacation", 100L),
                new FolderStat("/photos/family", 50L));
        when(folderRepository.count()).thenReturn(2L);
        when(assetRepository.count()).thenReturn(150L);
        when(assetRepository.sumFileSize()).thenReturn(0L);
        when(assetRepository.countDuplicates()).thenReturn(0L);
        when(assetRepository.findTopFoldersByAssetCount(5)).thenReturn(topFolders);
        when(assetRepository.findRecentAssets(12)).thenReturn(List.of());

        HomeStats result = sut.execute();

        assertThat(result.topFolders()).hasSize(2);
        assertThat(result.topFolders().get(0).path()).isEqualTo("/photos/vacation");
        assertThat(result.topFolders().get(0).assetCount()).isEqualTo(100L);
    }

    @Test
    void execute_withRecentAssets_returnsAssetSummaryDtos() {
        when(jobExplorer.getJobInstances("catalogJob", 0, 20)).thenReturn(List.of());
        Folder folder = Folder.builder().path("/photos").build();
        Asset asset = Asset.builder().assetId(1L).fileName("sunset.jpg").folder(folder).fileSize(512_000L).build();
        when(folderRepository.count()).thenReturn(1L);
        when(assetRepository.count()).thenReturn(1L);
        when(assetRepository.sumFileSize()).thenReturn(0L);
        when(assetRepository.countDuplicates()).thenReturn(0L);
        when(assetRepository.findTopFoldersByAssetCount(5)).thenReturn(List.of());
        when(assetRepository.findRecentAssets(12)).thenReturn(List.of(asset));

        HomeStats result = sut.execute();

        assertThat(result.recentAssets()).hasSize(1);
        AssetSummaryDto dto = result.recentAssets().get(0);
        assertThat(dto.assetId()).isEqualTo(1L);
        assertThat(dto.fileName()).isEqualTo("sunset.jpg");
        assertThat(dto.folderPath()).isEqualTo("/photos");
        assertThat(dto.thumbnailUrl()).isEqualTo("/api/assets/1/thumbnail");
        assertThat(dto.fileSize()).isEqualTo(512_000L);
    }

    @Test
    void execute_emptyLibrary_returnsZeroedStats() {
        when(jobExplorer.getJobInstances("catalogJob", 0, 20)).thenReturn(List.of());
        when(folderRepository.count()).thenReturn(0L);
        when(assetRepository.count()).thenReturn(0L);
        when(assetRepository.sumFileSize()).thenReturn(0L);
        when(assetRepository.countDuplicates()).thenReturn(0L);
        when(assetRepository.findTopFoldersByAssetCount(5)).thenReturn(List.of());
        when(assetRepository.findRecentAssets(12)).thenReturn(List.of());

        HomeStats result = sut.execute();

        assertThat(result.totalFileSize()).isZero();
        assertThat(result.duplicateCount()).isZero();
        assertThat(result.topFolders()).isEmpty();
        assertThat(result.recentAssets()).isEmpty();
    }

    private void stubCommonRepositoryCalls(long folderCount, long assetCount) {
        when(folderRepository.count()).thenReturn(folderCount);
        when(assetRepository.count()).thenReturn(assetCount);
        when(assetRepository.sumFileSize()).thenReturn(0L);
        when(assetRepository.countDuplicates()).thenReturn(0L);
        when(assetRepository.findTopFoldersByAssetCount(5)).thenReturn(List.of());
        when(assetRepository.findRecentAssets(12)).thenReturn(List.of());
    }
}
