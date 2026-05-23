package com.jpablodrexler.photomanager.application.usecase.asset;

import com.jpablodrexler.photomanager.application.dto.AssetFilter;
import com.jpablodrexler.photomanager.application.dto.PaginatedResult;
import com.jpablodrexler.photomanager.domain.enums.SortCriteria;
import com.jpablodrexler.photomanager.domain.model.Asset;
import com.jpablodrexler.photomanager.domain.model.Folder;
import com.jpablodrexler.photomanager.domain.model.TimelineGroup;
import com.jpablodrexler.photomanager.domain.port.out.AssetRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetAssetsTimelineUseCaseImplTest {

    @Mock AssetRepository assetRepository;
    @InjectMocks GetAssetsTimelineUseCaseImpl sut;

    private AssetFilter filterForPage(int page) {
        return new AssetFilter(1L, null, null, null, null, SortCriteria.FILE_NAME, page, 0, false, null);
    }

    private Asset assetWithDate(long id, LocalDateTime dateTime) {
        return Asset.builder()
                .assetId(id)
                .fileName("file" + id + ".jpg")
                .folder(Folder.builder().folderId(1L).path("/photos").build())
                .fileCreationDateTime(dateTime)
                .build();
    }

    @Test
    void execute_assetsFromTwoDays_returnsTwoGroups() {
        LocalDateTime day1 = LocalDateTime.of(2024, 5, 10, 12, 0);
        LocalDateTime day2 = LocalDateTime.of(2024, 5, 9, 8, 0);
        List<Asset> assets = List.of(assetWithDate(1L, day1), assetWithDate(2L, day2));
        when(assetRepository.findAllFilteredSortedByDateDesc(any())).thenReturn(assets);

        PaginatedResult<TimelineGroup> result = sut.execute(filterForPage(0));

        assertThat(result.items()).hasSize(2);
        assertThat(result.items().get(0).getLocalDate()).isEqualTo(LocalDate.of(2024, 5, 10));
        assertThat(result.items().get(1).getLocalDate()).isEqualTo(LocalDate.of(2024, 5, 9));
        assertThat(result.total()).isEqualTo(2);
        assertThat(result.page()).isEqualTo(0);
    }

    @Test
    void execute_assetsOnSameDay_returnsOneGroup() {
        LocalDateTime day = LocalDateTime.of(2024, 6, 1, 10, 0);
        List<Asset> assets = List.of(assetWithDate(1L, day), assetWithDate(2L, day.plusHours(2)));
        when(assetRepository.findAllFilteredSortedByDateDesc(any())).thenReturn(assets);

        PaginatedResult<TimelineGroup> result = sut.execute(filterForPage(0));

        assertThat(result.items()).hasSize(1);
        assertThat(result.items().get(0).getAssets()).hasSize(2);
    }

    @Test
    void execute_emptyResult_returnsEmptyGroups() {
        when(assetRepository.findAllFilteredSortedByDateDesc(any())).thenReturn(List.of());

        PaginatedResult<TimelineGroup> result = sut.execute(filterForPage(0));

        assertThat(result.items()).isEmpty();
        assertThat(result.total()).isEqualTo(0);
    }

    @Test
    void execute_moreThanPageSizeDays_paginatesCorrectly() {
        List<Asset> assets = new ArrayList<>();
        for (int i = 0; i < 35; i++) {
            assets.add(assetWithDate(i + 1L, LocalDateTime.of(2024, 1, 1, 0, 0).minusDays(i)));
        }
        when(assetRepository.findAllFilteredSortedByDateDesc(any())).thenReturn(assets);

        PaginatedResult<TimelineGroup> page0 = sut.execute(filterForPage(0));
        PaginatedResult<TimelineGroup> page1 = sut.execute(filterForPage(1));

        assertThat(page0.items()).hasSize(GetAssetsTimelineUseCaseImpl.TIMELINE_PAGE_SIZE);
        assertThat(page1.items()).hasSize(5);
        assertThat(page0.total()).isEqualTo(35);
        assertThat(page1.total()).isEqualTo(35);
    }

    @Test
    void execute_groupLabelIsFormattedDate() {
        LocalDateTime day = LocalDateTime.of(2024, 5, 10, 9, 0);
        when(assetRepository.findAllFilteredSortedByDateDesc(any())).thenReturn(List.of(assetWithDate(1L, day)));

        PaginatedResult<TimelineGroup> result = sut.execute(filterForPage(0));

        assertThat(result.items().get(0).getLabel()).isEqualTo("May 10, 2024");
    }

    @Test
    void execute_assetWithNullCreationDate_groupedUnderEpoch() {
        Asset asset = Asset.builder().assetId(1L).fileName("file.jpg")
                .folder(Folder.builder().folderId(1L).path("/photos").build())
                .fileCreationDateTime(null)
                .build();
        when(assetRepository.findAllFilteredSortedByDateDesc(any())).thenReturn(List.of(asset));

        PaginatedResult<TimelineGroup> result = sut.execute(filterForPage(0));

        assertThat(result.items()).hasSize(1);
        assertThat(result.items().get(0).getLocalDate()).isEqualTo(LocalDate.EPOCH);
    }
}
