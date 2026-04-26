package com.jpablodrexler.photomanager.application;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import com.jpablodrexler.photomanager.application.dto.AssetImage;
import com.jpablodrexler.photomanager.application.dto.CatalogChangeNotification;
import com.jpablodrexler.photomanager.application.dto.ConvertAssetsResult;
import com.jpablodrexler.photomanager.application.dto.PaginatedData;
import com.jpablodrexler.photomanager.application.dto.SyncAssetsResult;
import com.jpablodrexler.photomanager.domain.entity.Asset;
import com.jpablodrexler.photomanager.domain.entity.ConvertAssetsDirectoriesDefinition;
import com.jpablodrexler.photomanager.domain.entity.Folder;
import com.jpablodrexler.photomanager.domain.entity.SyncAssetsDirectoriesDefinition;
import com.jpablodrexler.photomanager.domain.enums.SortCriteria;

public interface PhotoManagerFacade {
    PaginatedData<Asset> getAssets(String folderPath, int pageIndex, SortCriteria sortCriteria);

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
}
