package com.jpablodrexler.photomanager.application;

import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import com.jpablodrexler.photomanager.application.dto.AlbumData;
import com.jpablodrexler.photomanager.application.dto.AssetImage;
import com.jpablodrexler.photomanager.application.dto.CatalogChangeNotification;
import com.jpablodrexler.photomanager.application.dto.ConvertAssetsResult;
import com.jpablodrexler.photomanager.application.dto.HomeStats;
import com.jpablodrexler.photomanager.application.dto.PaginatedData;
import com.jpablodrexler.photomanager.application.dto.SyncAssetsResult;
import com.jpablodrexler.photomanager.domain.entity.Asset;
import com.jpablodrexler.photomanager.domain.entity.AssetExif;
import org.springframework.web.multipart.MultipartFile;
import com.jpablodrexler.photomanager.domain.entity.ConvertAssetsDirectoriesDefinition;
import com.jpablodrexler.photomanager.domain.entity.Folder;
import com.jpablodrexler.photomanager.domain.entity.SyncAssetsDirectoriesDefinition;
import com.jpablodrexler.photomanager.domain.enums.SortCriteria;

public interface PhotoManagerFacade {
    PaginatedData<Asset> getAssets(String folderPath, int pageIndex, SortCriteria sortCriteria,
                                    String search, LocalDate dateFrom, LocalDate dateTo, Integer minRating);

    void rateAsset(Long assetId, int rating);

    CompletableFuture<Void> catalogAssetsAsync(Consumer<CatalogChangeNotification> callback);

    List<List<Asset>> getDuplicatedAssets();

    boolean moveAssets(Long[] assetIds, String destinationFolderPath, boolean preserveOriginal);

    void deleteAssets(Long[] assetIds, boolean deleteFiles);

    CompletableFuture<List<SyncAssetsResult>> syncAssetsAsync(Consumer<String> statusCallback);

    CompletableFuture<List<ConvertAssetsResult>> convertAssetsAsync(Consumer<String> statusCallback);

    List<SyncAssetsDirectoriesDefinition> getSyncAssetsConfiguration();

    void setSyncAssetsConfiguration(List<SyncAssetsDirectoriesDefinition> definitions);

    List<ConvertAssetsDirectoriesDefinition> getConvertAssetsConfiguration();

    void setConvertAssetsConfiguration(List<ConvertAssetsDirectoriesDefinition> definitions);

    List<String> getRecentTargetPaths();

    List<String> getDrives();

    List<Folder> getSubFolders(String parentPath);

    String getInitialFolder();

    AssetImage getAssetImage(Long assetId) throws IOException;

    HomeStats getHomeStats();

    AssetExif getAssetExif(Long assetId);

    Asset uploadAsset(String folderPath, MultipartFile file) throws IOException;

    List<AlbumData> getAlbums(UUID userId);

    AlbumData createAlbum(UUID userId, String name, String description);

    AlbumData getAlbumSummary(Long albumId, UUID userId);

    PaginatedData<Asset> getAlbumAssets(Long albumId, UUID userId, int pageIndex);

    AlbumData updateAlbum(Long albumId, UUID userId, String name, String description);

    void deleteAlbum(Long albumId, UUID userId);

    void addAssetsToAlbum(Long albumId, UUID userId, List<Long> assetIds);

    void removeAssetsFromAlbum(Long albumId, UUID userId, List<Long> assetIds);

    void downloadAssets(List<Long> assetIds, OutputStream out) throws IOException;

    PaginatedData<Asset> getRecycleBin(int pageIndex);

    void restoreAssets(List<Long> assetIds);

    void purgeRecycleBin(List<Long> assetIds);
}
