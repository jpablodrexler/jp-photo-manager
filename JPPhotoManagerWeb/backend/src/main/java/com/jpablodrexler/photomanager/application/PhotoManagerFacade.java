package com.jpablodrexler.photomanager.application;

import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import com.jpablodrexler.photomanager.infrastructure.web.dto.CreatePresetRequest;
import com.jpablodrexler.photomanager.infrastructure.web.dto.SearchPresetDto;
import com.jpablodrexler.photomanager.application.dto.AlbumData;
import com.jpablodrexler.photomanager.application.dto.AssetImage;
import com.jpablodrexler.photomanager.application.dto.CatalogChangeNotification;
import com.jpablodrexler.photomanager.application.dto.ConvertAssetsResult;
import com.jpablodrexler.photomanager.application.dto.HomeStats;
import com.jpablodrexler.photomanager.application.dto.PaginatedData;
import com.jpablodrexler.photomanager.application.dto.SyncAssetsResult;
import com.jpablodrexler.photomanager.domain.model.Asset;
import com.jpablodrexler.photomanager.domain.model.AssetExif;
import com.jpablodrexler.photomanager.domain.model.ConvertDirectoriesDefinition;
import com.jpablodrexler.photomanager.domain.model.Folder;
import com.jpablodrexler.photomanager.domain.model.SyncDirectoriesDefinition;
import com.jpablodrexler.photomanager.domain.enums.SortCriteria;
import org.springframework.web.multipart.MultipartFile;

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

    List<SyncDirectoriesDefinition> getSyncAssetsConfiguration();

    void setSyncAssetsConfiguration(List<SyncDirectoriesDefinition> definitions);

    List<ConvertDirectoriesDefinition> getConvertAssetsConfiguration();

    void setConvertAssetsConfiguration(List<ConvertDirectoriesDefinition> definitions);

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

    List<SearchPresetDto> listSearchPresets(UUID userId);

    SearchPresetDto saveSearchPreset(UUID userId, CreatePresetRequest request);

    void deleteSearchPreset(UUID userId, Long presetId);
}
