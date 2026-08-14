package com.jpablodrexler.photomanager.infrastructure.persistence.adapter;

import com.jpablodrexler.photomanager.domain.model.AssetFilter;
import com.jpablodrexler.photomanager.domain.model.PaginatedResult;
import com.jpablodrexler.photomanager.domain.enums.SortCriteria;
import com.jpablodrexler.photomanager.domain.model.Asset;
import com.jpablodrexler.photomanager.domain.model.Folder;
import com.jpablodrexler.photomanager.infrastructure.persistence.entity.AssetEntity;
import com.jpablodrexler.photomanager.infrastructure.persistence.entity.FolderEntity;
import com.jpablodrexler.photomanager.infrastructure.persistence.jpa.AssetSummary;
import com.jpablodrexler.photomanager.infrastructure.persistence.jpa.FolderAssetCount;
import com.jpablodrexler.photomanager.infrastructure.persistence.jpa.FolderStorageProjection;
import com.jpablodrexler.photomanager.infrastructure.persistence.jpa.FormatProjection;
import com.jpablodrexler.photomanager.infrastructure.persistence.jpa.JpaAssetRepository;
import com.jpablodrexler.photomanager.infrastructure.persistence.jpa.MonthlyCountProjection;
import com.jpablodrexler.photomanager.infrastructure.persistence.jpa.RatingProjection;
import com.jpablodrexler.photomanager.infrastructure.persistence.mapper.AssetEntityMapper;
import com.jpablodrexler.photomanager.infrastructure.persistence.mapper.FolderEntityMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssetRepositoryImplTest {

    @Mock JpaAssetRepository jpa;
    @Mock AssetEntityMapper assetMapper;
    @Mock FolderEntityMapper folderMapper;
    @InjectMocks AssetRepositoryImpl sut;

    @Test
    void findById_present_returnsMappedDomain() {
        AssetEntity entity = new AssetEntity();
        Asset domain = Asset.builder().assetId(1L).build();
        when(jpa.findByIdWithFolder(1L)).thenReturn(Optional.of(entity));
        when(assetMapper.toDomain(entity)).thenReturn(domain);

        Optional<Asset> result = sut.findById(1L);

        assertThat(result).contains(domain);
    }

    @Test
    void findById_absent_returnsEmpty() {
        when(jpa.findByIdWithFolder(99L)).thenReturn(Optional.empty());

        assertThat(sut.findById(99L)).isEmpty();
    }

    @Test
    void findByFolderAndFileName_present_returnsMappedDomain() {
        Folder folder = Folder.builder().folderId(1L).path("/photos").build();
        FolderEntity folderEntity = new FolderEntity();
        AssetEntity entity = new AssetEntity();
        Asset domain = Asset.builder().assetId(5L).build();
        when(folderMapper.toEntity(folder)).thenReturn(folderEntity);
        when(jpa.findByFolderAndFileName(folderEntity, "a.jpg")).thenReturn(Optional.of(entity));
        when(assetMapper.toDomain(entity)).thenReturn(domain);

        Optional<Asset> result = sut.findByFolderAndFileName(folder, "a.jpg");

        assertThat(result).contains(domain);
    }

    @Test
    void findFiltered_noFilters_callsJpaWithNullSearchAndNulls() {
        AssetFilter filter = new AssetFilter(1L, null, null, null, null, null, 0, 10, false, null);
        Page<AssetEntity> emptyPage = new PageImpl<>(List.of());
        when(jpa.findWithFilters(any(), isNull(), isNull(), isNull(), isNull(), isNull(), any())).thenReturn(emptyPage);

        PaginatedResult<Asset> result = sut.findFiltered(filter);

        assertThat(result.total()).isZero();
        verify(jpa).findWithFilters(any(), isNull(), isNull(), isNull(), isNull(), isNull(), any());
    }

    @Test
    void findFiltered_nullFolderId_passesNullFolderToJpa() {
        AssetFilter filter = new AssetFilter(null, null, null, null, null, null, 0, 10, false, null);
        Page<AssetEntity> emptyPage = new PageImpl<>(List.of());
        ArgumentCaptor<FolderEntity> folderCaptor = ArgumentCaptor.forClass(FolderEntity.class);
        when(jpa.findWithFilters(folderCaptor.capture(), isNull(), isNull(), isNull(), isNull(), isNull(), any()))
                .thenReturn(emptyPage);

        sut.findFiltered(filter);

        assertThat(folderCaptor.getValue()).isNull();
    }

    @Test
    void findAllFilteredSortedByDateDesc_nullFolderId_passesNullFolderToJpa() {
        AssetFilter filter = new AssetFilter(null, null, null, null, null, null, 0, 10, false, null);
        Page<AssetEntity> emptyPage = new PageImpl<>(List.of());
        ArgumentCaptor<FolderEntity> folderCaptor = ArgumentCaptor.forClass(FolderEntity.class);
        when(jpa.findWithFilters(folderCaptor.capture(), isNull(), isNull(), isNull(), isNull(), isNull(), any()))
                .thenReturn(emptyPage);

        sut.findAllFilteredSortedByDateDesc(filter);

        assertThat(folderCaptor.getValue()).isNull();
    }

    @Test
    void findFiltered_withSearchText_passesSearchWithWildcards() {
        AssetFilter filter = new AssetFilter(1L, " Cat ", null, null, null, null, 0, 10, false, null);
        Page<AssetEntity> emptyPage = new PageImpl<>(List.of());
        when(jpa.findWithFilters(any(), eq("%cat%"), isNull(), isNull(), isNull(), isNull(), any())).thenReturn(emptyPage);

        sut.findFiltered(filter);

        verify(jpa).findWithFilters(any(), eq("%cat%"), isNull(), isNull(), isNull(), isNull(), any());
    }

    @Test
    void findFiltered_withBlankSearch_passesNullSearch() {
        AssetFilter filter = new AssetFilter(1L, "   ", null, null, null, null, 0, 10, false, null);
        Page<AssetEntity> emptyPage = new PageImpl<>(List.of());
        when(jpa.findWithFilters(any(), isNull(), isNull(), isNull(), isNull(), isNull(), any())).thenReturn(emptyPage);

        sut.findFiltered(filter);

        verify(jpa).findWithFilters(any(), isNull(), isNull(), isNull(), isNull(), isNull(), any());
    }

    @Test
    void findFiltered_withDateRange_passesDateTimeRangeToJpa() {
        LocalDate from = LocalDate.of(2024, 1, 1);
        LocalDate to = LocalDate.of(2024, 12, 31);
        AssetFilter filter = new AssetFilter(1L, null, from, to, null, null, 0, 10, false, null);
        Page<AssetEntity> emptyPage = new PageImpl<>(List.of());
        LocalDateTime expectedFrom = from.atStartOfDay();
        LocalDateTime expectedTo = to.atTime(LocalTime.MAX);
        when(jpa.findWithFilters(any(), isNull(), eq(expectedFrom), eq(expectedTo), isNull(), isNull(), any())).thenReturn(emptyPage);

        sut.findFiltered(filter);

        verify(jpa).findWithFilters(any(), isNull(), eq(expectedFrom), eq(expectedTo), isNull(), isNull(), any());
    }

    @Test
    void findFiltered_withPositiveMinRating_passesMinRatingToJpa() {
        AssetFilter filter = new AssetFilter(1L, null, null, null, 3, null, 0, 10, false, null);
        Page<AssetEntity> emptyPage = new PageImpl<>(List.of());
        when(jpa.findWithFilters(any(), isNull(), isNull(), isNull(), eq(3), isNull(), any())).thenReturn(emptyPage);

        sut.findFiltered(filter);

        verify(jpa).findWithFilters(any(), isNull(), isNull(), isNull(), eq(3), isNull(), any());
    }

    @Test
    void findFiltered_withZeroMinRating_passesNullMinRating() {
        AssetFilter filter = new AssetFilter(1L, null, null, null, 0, null, 0, 10, false, null);
        Page<AssetEntity> emptyPage = new PageImpl<>(List.of());
        when(jpa.findWithFilters(any(), isNull(), isNull(), isNull(), isNull(), isNull(), any())).thenReturn(emptyPage);

        sut.findFiltered(filter);

        verify(jpa).findWithFilters(any(), isNull(), isNull(), isNull(), isNull(), isNull(), any());
    }

    @Test
    void findFiltered_withFileSizeSort_usesFileSizeDescSort() {
        AssetFilter filter = new AssetFilter(1L, null, null, null, null, SortCriteria.FILE_SIZE, 0, 10, false, null);
        Page<AssetEntity> emptyPage = new PageImpl<>(List.of());
        when(jpa.findWithFilters(any(), any(), any(), any(), any(), any(), any())).thenReturn(emptyPage);

        sut.findFiltered(filter);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(jpa).findWithFilters(any(), any(), any(), any(), any(), any(), pageableCaptor.capture());
        Sort sort = pageableCaptor.getValue().getSort();
        assertThat(sort.getOrderFor("fileSize")).isNotNull();
        assertThat(sort.getOrderFor("fileSize").getDirection()).isEqualTo(Sort.Direction.DESC);
    }

    @Test
    void findFiltered_withZeroPageSize_usesDefaultPageSize() {
        AssetFilter filter = new AssetFilter(1L, null, null, null, null, null, 0, 0, false, null);
        Page<AssetEntity> emptyPage = new PageImpl<>(List.of());
        when(jpa.findWithFilters(any(), any(), any(), any(), any(), any(), any())).thenReturn(emptyPage);

        PaginatedResult<Asset> result = sut.findFiltered(filter);

        assertThat(result.pageSize()).isEqualTo(100);
    }

    @Test
    void findByFolder_returnsMappedList() {
        Folder folder = Folder.builder().folderId(1L).path("/photos").build();
        FolderEntity folderEntity = new FolderEntity();
        AssetEntity entity = new AssetEntity();
        Asset domain = Asset.builder().assetId(1L).build();
        when(folderMapper.toEntity(folder)).thenReturn(folderEntity);
        when(jpa.findByFolder(folderEntity)).thenReturn(List.of(entity));
        when(assetMapper.toDomain(entity)).thenReturn(domain);

        List<Asset> result = sut.findByFolder(folder);

        assertThat(result).containsExactly(domain);
    }

    @Test
    void findAll_returnsMappedList() {
        AssetEntity entity = new AssetEntity();
        Asset domain = Asset.builder().assetId(2L).build();
        when(jpa.findAllWithFolder()).thenReturn(List.of(entity));
        when(assetMapper.toDomain(entity)).thenReturn(domain);

        assertThat(sut.findAll()).containsExactly(domain);
    }

    @Test
    void findAllById_returnsMappedList() {
        AssetEntity entity = new AssetEntity();
        Asset domain = Asset.builder().assetId(1L).build();
        when(jpa.findAllByIdWithFolder(List.of(1L))).thenReturn(List.of(entity));
        when(assetMapper.toDomain(entity)).thenReturn(domain);

        assertThat(sut.findAllById(List.of(1L))).containsExactly(domain);
    }

    @Test
    void findNotDeleted_returnsMappedList() {
        AssetEntity entity = new AssetEntity();
        Asset domain = Asset.builder().assetId(3L).build();
        when(jpa.findNotDeleted()).thenReturn(List.of(entity));
        when(assetMapper.toDomain(entity)).thenReturn(domain);

        assertThat(sut.findNotDeleted()).containsExactly(domain);
    }

    @Test
    void findAllDeleted_returnsMappedList() {
        AssetEntity entity = new AssetEntity();
        Asset domain = Asset.builder().assetId(4L).build();
        Page<AssetEntity> page = new PageImpl<>(List.of(entity));
        when(jpa.findDeletedOrderByDeletedAtDesc(Pageable.unpaged())).thenReturn(page);
        when(assetMapper.toDomain(entity)).thenReturn(domain);

        assertThat(sut.findAllDeleted()).containsExactly(domain);
    }

    @Test
    void findDeleted_returnsPaginatedResult() {
        AssetEntity entity = new AssetEntity();
        Asset domain = Asset.builder().assetId(5L).build();
        Page<AssetEntity> page = new PageImpl<>(List.of(entity));
        when(jpa.findDeletedOrderByDeletedAtDesc(any(Pageable.class))).thenReturn(page);
        when(assetMapper.toDomain(entity)).thenReturn(domain);

        PaginatedResult<Asset> result = sut.findDeleted(0, 50);

        assertThat(result.items()).containsExactly(domain);
        assertThat(result.pageSize()).isEqualTo(50);
    }

    @Test
    void findDeletedBefore_returnsMappedList() {
        LocalDateTime cutoff = LocalDateTime.now();
        AssetEntity entity = new AssetEntity();
        Asset domain = Asset.builder().assetId(6L).build();
        when(jpa.findByDeletedAtBefore(cutoff)).thenReturn(List.of(entity));
        when(assetMapper.toDomain(entity)).thenReturn(domain);

        assertThat(sut.findDeletedBefore(cutoff)).containsExactly(domain);
    }

    @Test
    void save_mapsAndPersists() {
        Asset asset = Asset.builder().assetId(1L).build();
        AssetEntity entity = new AssetEntity();
        when(assetMapper.toEntity(asset)).thenReturn(entity);
        when(jpa.save(entity)).thenReturn(entity);
        when(assetMapper.toDomain(entity)).thenReturn(asset);

        Asset result = sut.save(asset);

        assertThat(result).isEqualTo(asset);
        verify(jpa).save(entity);
    }

    @Test
    void deleteById_delegatesToJpa() {
        sut.deleteById(7L);
        verify(jpa).deleteById(7L);
    }

    @Test
    void countTotal_delegatesToJpa() {
        when(jpa.countByDeletedAtIsNull()).thenReturn(42L);
        assertThat(sut.countTotal()).isEqualTo(42L);
    }

    @Test
    void countDeleted_delegatesToJpa() {
        when(jpa.countByDeletedAtIsNotNull()).thenReturn(5L);
        assertThat(sut.countDeleted()).isEqualTo(5L);
    }

    @Test
    void count_delegatesToJpa() {
        when(jpa.count()).thenReturn(47L);
        assertThat(sut.count()).isEqualTo(47L);
    }

    @Test
    void findFiltered_withTags_passesTagsToJpa() {
        Set<String> tags = Set.of("vacation", "family");
        AssetFilter filter = new AssetFilter(1L, null, null, null, null, null, 0, 10, false, tags);
        Page<AssetEntity> emptyPage = new PageImpl<>(List.of());
        when(jpa.findWithFilters(any(), isNull(), isNull(), isNull(), isNull(), eq(tags), any())).thenReturn(emptyPage);

        sut.findFiltered(filter);

        verify(jpa).findWithFilters(any(), isNull(), isNull(), isNull(), isNull(), eq(tags), any());
    }

    @Test
    void findFiltered_withEmptyTags_passesEmptySetToJpa() {
        AssetFilter filter = new AssetFilter(1L, null, null, null, null, null, 0, 10, false, Set.of());
        Page<AssetEntity> emptyPage = new PageImpl<>(List.of());
        when(jpa.findWithFilters(any(), isNull(), isNull(), isNull(), isNull(), eq(Set.of()), any())).thenReturn(emptyPage);

        sut.findFiltered(filter);

        verify(jpa).findWithFilters(any(), isNull(), isNull(), isNull(), isNull(), eq(Set.of()), any());
    }

    @Test
    void addTagToAsset_delegatesToJpa() {
        sut.addTagToAsset(1L, 10L);
        verify(jpa).addTagToAsset(1L, 10L);
    }

    @Test
    void removeTagFromAsset_delegatesToJpaAndReturnsCount() {
        when(jpa.removeTagFromAsset(1L, 10L)).thenReturn(1);
        assertThat(sut.removeTagFromAsset(1L, 10L)).isEqualTo(1);
    }

    @Test
    void hasTag_delegatesToJpa() {
        when(jpa.hasTag(1L, 10L)).thenReturn(true);
        assertThat(sut.hasTag(1L, 10L)).isTrue();
    }

    // --- kafka-async-upload: completeIfAllStagesFinished (idempotent COMPLETED transition) ---

    @Test
    void completeIfAllStagesFinished_rowUpdated_returnsTrue() {
        when(jpa.completeIfAllStagesFinished(1L)).thenReturn(1);

        assertThat(sut.completeIfAllStagesFinished(1L)).isTrue();
    }

    @Test
    void completeIfAllStagesFinished_alreadyCompletedOrNotAllStagesDone_returnsFalse() {
        // Simulates the race described in design.md risk #3: a second concurrent caller's guarded
        // UPDATE (WHERE processing_status <> 'COMPLETED') affects zero rows once the first caller's
        // UPDATE has already committed, so at most one caller ever observes true for a given asset.
        when(jpa.completeIfAllStagesFinished(1L)).thenReturn(0);

        assertThat(sut.completeIfAllStagesFinished(1L)).isFalse();
    }

    @Test
    void updateHash_delegatesToJpaWithTargetedColumnUpdate() {
        LocalDateTime now = LocalDateTime.now();
        sut.updateHash(1L, "abc123", now);
        verify(jpa).updateHash(1L, "abc123", now);
    }

    @Test
    void updateExifCompletedAt_delegatesToJpa() {
        LocalDateTime now = LocalDateTime.now();
        sut.updateExifCompletedAt(1L, now);
        verify(jpa).updateExifCompletedAt(1L, now);
    }

    @Test
    void updateThumbnail_delegatesToJpa() {
        LocalDateTime now = LocalDateTime.now();
        sut.updateThumbnail(1L, now, now);
        verify(jpa).updateThumbnail(1L, now, now);
    }

    @Test
    void updateProcessingStatus_delegatesToJpa() {
        sut.updateProcessingStatus(1L, com.jpablodrexler.photomanager.domain.enums.ProcessingStatus.FAILED);
        verify(jpa).updateProcessingStatus(1L, com.jpablodrexler.photomanager.domain.enums.ProcessingStatus.FAILED);
    }

    @Test
    void findByFileName_returnsMappedList() {
        AssetEntity entity = new AssetEntity();
        Asset domain = Asset.builder().assetId(8L).build();
        when(jpa.findByFileNameNotDeleted("a.jpg")).thenReturn(List.of(entity));
        when(assetMapper.toDomain(entity)).thenReturn(domain);

        assertThat(sut.findByFileName("a.jpg")).containsExactly(domain);
    }

    @Test
    void sumFileSize_delegatesToJpa() {
        when(jpa.sumFileSize()).thenReturn(12345L);
        assertThat(sut.sumFileSize()).isEqualTo(12345L);
    }

    @Test
    void countDuplicates_delegatesToJpa() {
        when(jpa.countDuplicates()).thenReturn(3L);
        assertThat(sut.countDuplicates()).isEqualTo(3L);
    }

    @Test
    void findTopFoldersByAssetCount_returnsMappedFolderStats() {
        FolderAssetCount projection = org.mockito.Mockito.mock(FolderAssetCount.class);
        when(projection.getFolderPath()).thenReturn("/photos");
        when(projection.getAssetCount()).thenReturn(7L);
        when(jpa.findTopFoldersByAssetCount(PageRequest.of(0, 5))).thenReturn(List.of(projection));

        assertThat(sut.findTopFoldersByAssetCount(5))
                .extracting(fs -> fs.path(), fs -> fs.assetCount())
                .containsExactly(org.assertj.core.api.Assertions.tuple("/photos", 7L));
    }

    @Test
    void findRecentAssets_buildsAssetsFromSummaryProjection() {
        AssetSummary summary = org.mockito.Mockito.mock(AssetSummary.class);
        when(summary.getAssetId()).thenReturn(1L);
        when(summary.getFileName()).thenReturn("a.jpg");
        when(summary.getFolderPath()).thenReturn("/photos");
        when(summary.getFileSize()).thenReturn(1024L);
        when(jpa.findRecentAssets(PageRequest.of(0, 10))).thenReturn(List.of(summary));

        List<Asset> result = sut.findRecentAssets(10);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getFileName()).isEqualTo("a.jpg");
        assertThat(result.get(0).getFolder().getPath()).isEqualTo("/photos");
        assertThat(result.get(0).getFileSize()).isEqualTo(1024L);
    }

    @Test
    void sumFileSizeByFolder_returnsMappedEntries() {
        FolderStorageProjection projection = org.mockito.Mockito.mock(FolderStorageProjection.class);
        when(projection.getFolderPath()).thenReturn("/photos");
        when(projection.getBytes()).thenReturn(2048L);
        when(jpa.sumFileSizeByFolder()).thenReturn(List.of(projection));

        assertThat(sut.sumFileSizeByFolder())
                .extracting(e -> e.folderPath(), e -> e.bytes())
                .containsExactly(org.assertj.core.api.Assertions.tuple("/photos", 2048L));
    }

    @Test
    void countByExtension_returnsMappedEntries() {
        FormatProjection projection = org.mockito.Mockito.mock(FormatProjection.class);
        when(projection.getExtension()).thenReturn("jpg");
        when(projection.getCnt()).thenReturn(10L);
        when(jpa.countByExtension()).thenReturn(List.of(projection));

        assertThat(sut.countByExtension())
                .extracting(e -> e.extension(), e -> e.count())
                .containsExactly(org.assertj.core.api.Assertions.tuple("jpg", 10L));
    }

    @Test
    void countByCreationMonth_returnsMappedEntries() {
        MonthlyCountProjection projection = org.mockito.Mockito.mock(MonthlyCountProjection.class);
        when(projection.getMonth()).thenReturn("2026-01");
        when(projection.getCnt()).thenReturn(4L);
        when(jpa.countByCreationMonth()).thenReturn(List.of(projection));

        assertThat(sut.countByCreationMonth())
                .extracting(e -> e.month(), e -> e.count())
                .containsExactly(org.assertj.core.api.Assertions.tuple("2026-01", 4L));
    }

    @Test
    void countByRating_returnsMappedEntries() {
        RatingProjection projection = org.mockito.Mockito.mock(RatingProjection.class);
        when(projection.getRating()).thenReturn(5);
        when(projection.getCnt()).thenReturn(2L);
        when(jpa.countByRating()).thenReturn(List.of(projection));

        assertThat(sut.countByRating())
                .extracting(e -> e.rating(), e -> e.count())
                .containsExactly(org.assertj.core.api.Assertions.tuple(5, 2L));
    }

    @Test
    void findAllFilteredSortedByDateDesc_withNonNullFolderIdAndFilters_passesAllToJpa() {
        LocalDate from = LocalDate.of(2024, 1, 1);
        LocalDate to = LocalDate.of(2024, 12, 31);
        AssetFilter filter = new AssetFilter(1L, "cat", from, to, 3, null, 0, 10, false, null);
        Page<AssetEntity> emptyPage = new PageImpl<>(List.of());
        ArgumentCaptor<FolderEntity> folderCaptor = ArgumentCaptor.forClass(FolderEntity.class);
        when(jpa.findWithFilters(folderCaptor.capture(), eq("%cat%"), eq(from.atStartOfDay()),
                eq(to.atTime(LocalTime.MAX)), eq(3), isNull(), any())).thenReturn(emptyPage);

        sut.findAllFilteredSortedByDateDesc(filter);

        assertThat(folderCaptor.getValue().getFolderId()).isEqualTo(1L);
    }

    @Test
    void toFolderEntityOrNull_nonNullFolderId_setsFolderIdOnEntity() {
        AssetFilter filter = new AssetFilter(3L, null, null, null, null, null, 0, 10, false, null);
        Page<AssetEntity> emptyPage = new PageImpl<>(List.of());
        ArgumentCaptor<FolderEntity> folderCaptor = ArgumentCaptor.forClass(FolderEntity.class);
        when(jpa.findWithFilters(folderCaptor.capture(), isNull(), isNull(), isNull(), isNull(), isNull(), any()))
                .thenReturn(emptyPage);

        sut.findFiltered(filter);

        assertThat(folderCaptor.getValue().getFolderId()).isEqualTo(3L);
    }
}
