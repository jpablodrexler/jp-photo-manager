package com.jpablodrexler.photomanager.domain.port.out;

import com.jpablodrexler.photomanager.domain.model.AssetFilter;
import com.jpablodrexler.photomanager.domain.model.FolderStat;
import com.jpablodrexler.photomanager.domain.model.PaginatedResult;
import com.jpablodrexler.photomanager.domain.enums.ProcessingStatus;
import com.jpablodrexler.photomanager.domain.model.Asset;
import com.jpablodrexler.photomanager.domain.model.Folder;
import com.jpablodrexler.photomanager.domain.model.FolderStorageEntry;
import com.jpablodrexler.photomanager.domain.model.FormatEntry;
import com.jpablodrexler.photomanager.domain.model.MonthlyCountEntry;
import com.jpablodrexler.photomanager.domain.model.RatingEntry;

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

    List<FolderStorageEntry> sumFileSizeByFolder();

    List<FormatEntry> countByExtension();

    List<MonthlyCountEntry> countByCreationMonth();

    List<RatingEntry> countByRating();

    /**
     * Idempotently flips {@code processing_status} to {@code COMPLETED} for the given asset if and
     * only if all three completion timestamps (hash/exif/thumbnail) are already non-null and the
     * asset is not already {@code COMPLETED}. Returns {@code true} for at most one caller across any
     * number of concurrently racing invocations.
     */
    boolean completeIfAllStagesFinished(Long assetId);

    /**
     * Targeted single-column(s) update used instead of a find-then-save round trip: independent
     * concurrent processing stages run against the same row, and a full-entity {@link #save(Asset)}
     * based on a stale in-memory snapshot would silently overwrite whichever other stage had already
     * committed. These methods update only their own column(s).
     */
    void updateHash(Long assetId, String hash, LocalDateTime hashCompletedAt);

    void updateExifCompletedAt(Long assetId, LocalDateTime exifCompletedAt);

    void updateThumbnail(Long assetId, LocalDateTime thumbnailCreationDateTime, LocalDateTime thumbnailCompletedAt);

    void updateProcessingStatus(Long assetId, ProcessingStatus status);
}
