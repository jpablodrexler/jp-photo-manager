package com.jpablodrexler.photomanager.application.usecase.analytics;

import com.jpablodrexler.photomanager.domain.model.AnalyticsData;
import com.jpablodrexler.photomanager.domain.model.FolderStorageEntry;
import com.jpablodrexler.photomanager.domain.model.FormatEntry;
import com.jpablodrexler.photomanager.domain.model.MonthlyCountEntry;
import com.jpablodrexler.photomanager.domain.model.RatingEntry;
import com.jpablodrexler.photomanager.domain.port.out.AssetRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetAnalyticsUseCaseImplTest {

    @Mock AssetRepository assetRepository;
    @InjectMocks GetAnalyticsUseCaseImpl sut;

    @Test
    void execute_withData_returnsAssembledAnalyticsData() {
        when(assetRepository.sumFileSizeByFolder()).thenReturn(List.of(
                new FolderStorageEntry("/photos/vacation", 6_000_000L)));
        when(assetRepository.countByExtension()).thenReturn(List.of(
                new FormatEntry("jpg", 250L)));
        when(assetRepository.countByCreationMonth()).thenReturn(List.of(
                new MonthlyCountEntry("2024-01", 10L)));
        when(assetRepository.countByRating()).thenReturn(List.of(
                new RatingEntry(0, 100L)));

        AnalyticsData result = sut.execute();

        assertThat(result.getFolderStorage()).hasSize(1);
        assertThat(result.getFormatDistribution()).hasSize(1);
        assertThat(result.getPhotosPerMonth()).hasSize(1);
        assertThat(result.getRatingDistribution()).hasSize(1);
    }

    @Test
    void execute_withMoreThan20Folders_capsTo20PlusOther() {
        List<FolderStorageEntry> folders = new ArrayList<>();
        for (int i = 0; i < 25; i++) {
            folders.add(new FolderStorageEntry("/folder/" + i, (25L - i) * 1_000_000L));
        }
        when(assetRepository.sumFileSizeByFolder()).thenReturn(folders);
        when(assetRepository.countByExtension()).thenReturn(List.of());
        when(assetRepository.countByCreationMonth()).thenReturn(List.of());
        when(assetRepository.countByRating()).thenReturn(List.of());

        AnalyticsData result = sut.execute();

        assertThat(result.getFolderStorage()).hasSize(21);
        FolderStorageEntry other = result.getFolderStorage().get(20);
        assertThat(other.folderPath()).isEqualTo("other");
        long expectedOther = (5L + 4L + 3L + 2L + 1L) * 1_000_000L;
        assertThat(other.bytes()).isEqualTo(expectedOther);
    }

    @Test
    void execute_withExactly20Folders_noOtherEntry() {
        List<FolderStorageEntry> folders = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            folders.add(new FolderStorageEntry("/folder/" + i, 1_000_000L));
        }
        when(assetRepository.sumFileSizeByFolder()).thenReturn(folders);
        when(assetRepository.countByExtension()).thenReturn(List.of());
        when(assetRepository.countByCreationMonth()).thenReturn(List.of());
        when(assetRepository.countByRating()).thenReturn(List.of());

        AnalyticsData result = sut.execute();

        assertThat(result.getFolderStorage()).hasSize(20);
        assertThat(result.getFolderStorage()).noneMatch(e -> "other".equals(e.folderPath()));
    }

    @Test
    void execute_withExtensionlessAsset_mapsToUnknown() {
        when(assetRepository.sumFileSizeByFolder()).thenReturn(List.of());
        when(assetRepository.countByExtension()).thenReturn(List.of(
                new FormatEntry("", 5L),
                new FormatEntry("jpg", 100L)));
        when(assetRepository.countByCreationMonth()).thenReturn(List.of());
        when(assetRepository.countByRating()).thenReturn(List.of());

        AnalyticsData result = sut.execute();

        assertThat(result.getFormatDistribution()).anySatisfy(e -> {
            assertThat(e.extension()).isEqualTo("unknown");
            assertThat(e.count()).isEqualTo(5L);
        });
        assertThat(result.getFormatDistribution()).anySatisfy(e ->
                assertThat(e.extension()).isEqualTo("jpg"));
    }

    @Test
    void execute_withEmptyRepository_returnsEmptyLists() {
        when(assetRepository.sumFileSizeByFolder()).thenReturn(List.of());
        when(assetRepository.countByExtension()).thenReturn(List.of());
        when(assetRepository.countByCreationMonth()).thenReturn(List.of());
        when(assetRepository.countByRating()).thenReturn(List.of());

        AnalyticsData result = sut.execute();

        assertThat(result.getFolderStorage()).isEmpty();
        assertThat(result.getFormatDistribution()).isEmpty();
        assertThat(result.getPhotosPerMonth()).isEmpty();
        assertThat(result.getRatingDistribution()).isEmpty();
    }
}
