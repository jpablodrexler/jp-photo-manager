package com.jpablodrexler.photomanager.infrastructure.persistence.adapter;

import com.jpablodrexler.photomanager.application.dto.AssetFilter;
import com.jpablodrexler.photomanager.application.dto.FolderStat;
import com.jpablodrexler.photomanager.application.dto.PaginatedResult;
import com.jpablodrexler.photomanager.domain.model.Asset;
import com.jpablodrexler.photomanager.domain.model.Folder;
import com.jpablodrexler.photomanager.domain.model.FolderStorageEntry;
import com.jpablodrexler.photomanager.domain.model.FormatEntry;
import com.jpablodrexler.photomanager.domain.model.MonthlyCountEntry;
import com.jpablodrexler.photomanager.domain.model.RatingEntry;
import com.jpablodrexler.photomanager.domain.port.out.AssetRepository;
import com.jpablodrexler.photomanager.infrastructure.persistence.entity.AssetEntity;
import com.jpablodrexler.photomanager.infrastructure.persistence.entity.FolderEntity;
import com.jpablodrexler.photomanager.infrastructure.persistence.jpa.JpaAssetRepository;
import com.jpablodrexler.photomanager.infrastructure.persistence.mapper.AssetEntityMapper;
import com.jpablodrexler.photomanager.infrastructure.persistence.mapper.FolderEntityMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.jpablodrexler.photomanager.domain.enums.SortCriteria;

@Service
@RequiredArgsConstructor
public class AssetRepositoryImpl implements AssetRepository {

    private static final int DEFAULT_PAGE_SIZE = 100;
    private static final Sort DEFAULT_SORT = Sort.by("fileName").ascending();
    private static final Map<SortCriteria, Sort> SORT_MAP = Map.of(
            SortCriteria.FILE_NAME, DEFAULT_SORT,
            SortCriteria.FILE_SIZE, Sort.by("fileSize").descending(),
            SortCriteria.FILE_CREATION_DATE_TIME, Sort.by("fileCreationDateTime").descending(),
            SortCriteria.FILE_MODIFICATION_DATE_TIME, Sort.by("fileModificationDateTime").descending(),
            SortCriteria.THUMBNAIL_CREATION_DATE_TIME, Sort.by("thumbnailCreationDateTime").descending(),
            SortCriteria.RATING, Sort.by("rating").descending());

    private final JpaAssetRepository jpa;
    private final AssetEntityMapper assetMapper;
    private final FolderEntityMapper folderMapper;

    @Override
    @Transactional(readOnly = true)
    public Optional<Asset> findById(Long id) {
        return jpa.findById(id).map(assetMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsById(Long id) {
        return jpa.existsById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Asset> findByFolderAndFileName(Folder folder, String fileName) {
        FolderEntity folderEntity = folderMapper.toEntity(folder);
        return jpa.findByFolderAndFileName(folderEntity, fileName).map(assetMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Asset> findByFileName(String fileName) {
        return jpa.findByFileNameNotDeleted(fileName).stream().map(assetMapper::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PaginatedResult<Asset> findFiltered(AssetFilter filter) {
        FolderEntity folderEntity = new FolderEntity();
        folderEntity.setFolderId(filter.folderId());

        Sort sort = filter.sortCriteria() != null
                ? SORT_MAP.getOrDefault(filter.sortCriteria(), DEFAULT_SORT)
                : DEFAULT_SORT;
        int pageSize = filter.pageSize() > 0 ? filter.pageSize() : DEFAULT_PAGE_SIZE;
        PageRequest pageRequest = PageRequest.of(filter.page(), pageSize, sort);

        String search = (filter.search() != null && !filter.search().isBlank())
                ? "%" + filter.search().trim().toLowerCase() + "%" : null;
        LocalDateTime dateFrom = filter.dateFrom() != null ? filter.dateFrom().atStartOfDay() : null;
        LocalDateTime dateTo = filter.dateTo() != null ? filter.dateTo().atTime(LocalTime.MAX) : null;
        Integer minRating = (filter.minRating() != null && filter.minRating() > 0) ? filter.minRating() : null;

        Page<AssetEntity> page = jpa.findWithFilters(folderEntity, search, dateFrom, dateTo, minRating, filter.tags(), pageRequest);
        List<Asset> items = page.getContent().stream().map(assetMapper::toDomain).toList();
        return new PaginatedResult<>(items, page.getTotalElements(), filter.page(), pageSize);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Asset> findAllFilteredSortedByDateDesc(AssetFilter filter) {
        FolderEntity folderEntity = new FolderEntity();
        folderEntity.setFolderId(filter.folderId());

        String search = (filter.search() != null && !filter.search().isBlank())
                ? "%" + filter.search().trim().toLowerCase() + "%" : null;
        LocalDateTime dateFrom = filter.dateFrom() != null ? filter.dateFrom().atStartOfDay() : null;
        LocalDateTime dateTo = filter.dateTo() != null ? filter.dateTo().atTime(LocalTime.MAX) : null;
        Integer minRating = (filter.minRating() != null && filter.minRating() > 0) ? filter.minRating() : null;

        Sort sort = Sort.by("fileCreationDateTime").descending();
        Page<AssetEntity> page = jpa.findWithFilters(folderEntity, search, dateFrom, dateTo, minRating, filter.tags(),
                org.springframework.data.domain.Pageable.unpaged(sort));
        return page.getContent().stream().map(assetMapper::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Asset> findByFolder(Folder folder) {
        FolderEntity folderEntity = folderMapper.toEntity(folder);
        return jpa.findByFolder(folderEntity).stream().map(assetMapper::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Asset> findAll() {
        return jpa.findAll().stream().map(assetMapper::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Asset> findAllById(List<Long> ids) {
        return jpa.findAllById(ids).stream().map(assetMapper::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Asset> findNotDeleted() {
        return jpa.findNotDeleted().stream().map(assetMapper::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Asset> findAllDeleted() {
        return jpa.findDeletedOrderByDeletedAtDesc(Pageable.unpaged()).getContent()
                .stream().map(assetMapper::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PaginatedResult<Asset> findDeleted(int page, int pageSize) {
        int size = pageSize > 0 ? pageSize : DEFAULT_PAGE_SIZE;
        Page<AssetEntity> entityPage = jpa.findDeletedOrderByDeletedAtDesc(PageRequest.of(page, size));
        List<Asset> items = entityPage.getContent().stream().map(assetMapper::toDomain).toList();
        return new PaginatedResult<>(items, entityPage.getTotalElements(), page, size);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Asset> findDeletedBefore(LocalDateTime cutoff) {
        return jpa.findByDeletedAtBefore(cutoff).stream().map(assetMapper::toDomain).toList();
    }

    @Override
    @Transactional
    public Asset save(Asset asset) {
        AssetEntity entity = assetMapper.toEntity(asset);
        return assetMapper.toDomain(jpa.save(entity));
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        jpa.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public long countTotal() {
        return jpa.countByDeletedAtIsNull();
    }

    @Override
    @Transactional(readOnly = true)
    public long countDeleted() {
        return jpa.countByDeletedAtIsNotNull();
    }

    @Override
    @Transactional(readOnly = true)
    public long count() {
        return jpa.count();
    }

    @Override
    @Transactional
    public void addTagToAsset(Long assetId, Long tagId) {
        jpa.addTagToAsset(assetId, tagId);
    }

    @Override
    @Transactional
    public int removeTagFromAsset(Long assetId, Long tagId) {
        return jpa.removeTagFromAsset(assetId, tagId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasTag(Long assetId, Long tagId) {
        return jpa.hasTag(assetId, tagId);
    }

    @Override
    @Transactional(readOnly = true)
    public long sumFileSize() {
        return jpa.sumFileSize();
    }

    @Override
    @Transactional(readOnly = true)
    public long countDuplicates() {
        return jpa.countDuplicates();
    }

    @Override
    @Transactional(readOnly = true)
    public List<FolderStat> findTopFoldersByAssetCount(int limit) {
        return jpa.findTopFoldersByAssetCount(PageRequest.of(0, limit))
                .stream()
                .map(fc -> new FolderStat(fc.getFolderPath(), fc.getAssetCount()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Asset> findRecentAssets(int limit) {
        return jpa.findRecentAssets(PageRequest.of(0, limit))
                .stream()
                .map(s -> Asset.builder()
                        .assetId(s.getAssetId())
                        .fileName(s.getFileName())
                        .folder(Folder.builder().path(s.getFolderPath()).build())
                        .fileSize(s.getFileSize())
                        .build())
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<FolderStorageEntry> sumFileSizeByFolder() {
        return jpa.sumFileSizeByFolder().stream()
                .map(p -> new FolderStorageEntry(p.getFolderPath(), p.getBytes()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<FormatEntry> countByExtension() {
        return jpa.countByExtension().stream()
                .map(p -> new FormatEntry(p.getExtension(), p.getCnt()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MonthlyCountEntry> countByCreationMonth() {
        return jpa.countByCreationMonth().stream()
                .map(p -> new MonthlyCountEntry(p.getMonth(), p.getCnt()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<RatingEntry> countByRating() {
        return jpa.countByRating().stream()
                .map(p -> new RatingEntry(p.getRating(), p.getCnt()))
                .toList();
    }
}
