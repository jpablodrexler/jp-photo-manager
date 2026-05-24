package com.jpablodrexler.photomanager.domain.port.out;

import com.jpablodrexler.photomanager.application.dto.AssetFilter;
import com.jpablodrexler.photomanager.application.dto.FolderStat;
import com.jpablodrexler.photomanager.application.dto.PaginatedResult;
import com.jpablodrexler.photomanager.domain.model.Asset;
import com.jpablodrexler.photomanager.domain.model.Folder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AssetRepository {

    Optional<Asset> findById(Long id);

    boolean existsById(Long id);

    Optional<Asset> findByFolderAndFileName(Folder folder, String fileName);

    List<Asset> findByFileName(String fileName);

    PaginatedResult<Asset> findFiltered(AssetFilter filter);

    List<Asset> findAllFilteredSortedByDateDesc(AssetFilter filter);

    List<Asset> findByFolder(Folder folder);

    List<Asset> findAll();

    List<Asset> findAllById(List<Long> ids);

    List<Asset> findNotDeleted();

    List<Asset> findAllDeleted();

    PaginatedResult<Asset> findDeleted(int page, int pageSize);

    List<Asset> findDeletedBefore(LocalDateTime cutoff);

    Asset save(Asset asset);

    void deleteById(Long id);

    void addTagToAsset(Long assetId, Long tagId);

    int removeTagFromAsset(Long assetId, Long tagId);

    boolean hasTag(Long assetId, Long tagId);

    long countTotal();

    long countDeleted();

    long count();

    long sumFileSize();

    long countDuplicates();

    List<FolderStat> findTopFoldersByAssetCount(int limit);

    List<Asset> findRecentAssets(int limit);
}
