package com.jpablodrexler.photomanager.application.usecase.home;

import com.jpablodrexler.photomanager.application.dto.AssetSummaryDto;
import com.jpablodrexler.photomanager.application.dto.FolderStat;
import com.jpablodrexler.photomanager.application.dto.HomeStats;
import com.jpablodrexler.photomanager.domain.model.Asset;
import com.jpablodrexler.photomanager.domain.model.CatalogRunState;
import com.jpablodrexler.photomanager.domain.model.Folder;
import com.jpablodrexler.photomanager.domain.port.out.AssetRepository;
import com.jpablodrexler.photomanager.domain.port.out.CatalogStateRepository;
import com.jpablodrexler.photomanager.domain.port.out.FolderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetHomeStatsUseCaseImplTest {

    @Mock FolderRepository folderRepository;
    @Mock AssetRepository assetRepository;
    @Mock CatalogStateRepository catalogStateRepository;
    @InjectMocks GetHomeStatsUseCaseImpl sut;

    @Test
    void execute_withLastCompleted_returnsStatsWithLastCatalogDate() {
        Instant completedAt = Instant.now();
        CatalogRunState state = CatalogRunState.builder().lastCompletedAt(completedAt).build();
        when(folderRepository.count()).thenReturn(5L);
        when(assetRepository.count()).thenReturn(200L);
        when(catalogStateRepository.find()).thenReturn(Optional.of(state));
        when(assetRepository.sumFileSize()).thenReturn(0L);
        when(assetRepository.countDuplicates()).thenReturn(0L);
        when(assetRepository.findTopFoldersByAssetCount(5)).thenReturn(List.of());
        when(assetRepository.findRecentAssets(12)).thenReturn(List.of());

        HomeStats result = sut.execute();

        assertThat(result.folderCount()).isEqualTo(5L);
        assertThat(result.assetCount()).isEqualTo(200L);
        assertThat(result.lastCatalogCompletedAt()).isEqualTo(completedAt);
    }

    @Test
    void execute_withoutLastCompleted_returnsStatsWithNullLastCatalogDate() {
        when(folderRepository.count()).thenReturn(0L);
        when(assetRepository.count()).thenReturn(0L);
        when(catalogStateRepository.find()).thenReturn(Optional.empty());
        when(assetRepository.sumFileSize()).thenReturn(0L);
        when(assetRepository.countDuplicates()).thenReturn(0L);
        when(assetRepository.findTopFoldersByAssetCount(5)).thenReturn(List.of());
        when(assetRepository.findRecentAssets(12)).thenReturn(List.of());

        HomeStats result = sut.execute();

        assertThat(result.lastCatalogCompletedAt()).isNull();
    }

    @Test
    void execute_withAssets_returnsTotalFileSize() {
        when(folderRepository.count()).thenReturn(2L);
        when(assetRepository.count()).thenReturn(10L);
        when(catalogStateRepository.find()).thenReturn(Optional.empty());
        when(assetRepository.sumFileSize()).thenReturn(5_000_000L);
        when(assetRepository.countDuplicates()).thenReturn(0L);
        when(assetRepository.findTopFoldersByAssetCount(5)).thenReturn(List.of());
        when(assetRepository.findRecentAssets(12)).thenReturn(List.of());

        HomeStats result = sut.execute();

        assertThat(result.totalFileSize()).isEqualTo(5_000_000L);
    }

    @Test
    void execute_withDuplicates_returnsDuplicateCount() {
        when(folderRepository.count()).thenReturn(1L);
        when(assetRepository.count()).thenReturn(5L);
        when(catalogStateRepository.find()).thenReturn(Optional.empty());
        when(assetRepository.sumFileSize()).thenReturn(0L);
        when(assetRepository.countDuplicates()).thenReturn(3L);
        when(assetRepository.findTopFoldersByAssetCount(5)).thenReturn(List.of());
        when(assetRepository.findRecentAssets(12)).thenReturn(List.of());

        HomeStats result = sut.execute();

        assertThat(result.duplicateCount()).isEqualTo(3L);
    }

    @Test
    void execute_withTopFolders_returnsTopFoldersList() {
        List<FolderStat> topFolders = List.of(
                new FolderStat("/photos/vacation", 100L),
                new FolderStat("/photos/family", 50L));
        when(folderRepository.count()).thenReturn(2L);
        when(assetRepository.count()).thenReturn(150L);
        when(catalogStateRepository.find()).thenReturn(Optional.empty());
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
        Folder folder = Folder.builder().path("/photos").build();
        Asset asset = Asset.builder().assetId(1L).fileName("sunset.jpg").folder(folder).build();
        when(folderRepository.count()).thenReturn(1L);
        when(assetRepository.count()).thenReturn(1L);
        when(catalogStateRepository.find()).thenReturn(Optional.empty());
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
    }

    @Test
    void execute_emptyLibrary_returnsZeroedStats() {
        when(folderRepository.count()).thenReturn(0L);
        when(assetRepository.count()).thenReturn(0L);
        when(catalogStateRepository.find()).thenReturn(Optional.empty());
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
}
