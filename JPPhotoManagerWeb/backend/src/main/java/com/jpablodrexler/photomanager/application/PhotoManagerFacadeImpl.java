package com.jpablodrexler.photomanager.application;

import com.jpablodrexler.photomanager.application.dto.AssetImage;
import com.jpablodrexler.photomanager.application.dto.CatalogChangeNotification;
import com.jpablodrexler.photomanager.application.dto.ConvertAssetsResult;
import com.jpablodrexler.photomanager.application.dto.HomeStats;
import com.jpablodrexler.photomanager.application.dto.PaginatedData;
import com.jpablodrexler.photomanager.application.dto.SyncAssetsResult;
import com.jpablodrexler.photomanager.domain.entity.*;
import com.jpablodrexler.photomanager.domain.enums.SortCriteria;
import com.jpablodrexler.photomanager.domain.repository.*;
import com.jpablodrexler.photomanager.domain.repository.AssetExifRepository;
import com.jpablodrexler.photomanager.domain.service.*;
import lombok.RequiredArgsConstructor;
import java.io.IOException;
import java.util.NoSuchElementException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PhotoManagerFacadeImpl implements PhotoManagerFacade {

    private static final int PAGE_SIZE = 100;
    private static final int MAX_RECENT_PATHS = 20;
    private static final Sort DEFAULT_SORT = Sort.by("fileName").ascending();
    private static final Map<SortCriteria, Sort> SORT_MAP = Map.of(
            SortCriteria.FILE_NAME, DEFAULT_SORT,
            SortCriteria.FILE_SIZE, Sort.by("fileSize").descending(),
            SortCriteria.FILE_CREATION_DATE_TIME, Sort.by("fileCreationDateTime").descending(),
            SortCriteria.FILE_MODIFICATION_DATE_TIME, Sort.by("fileModificationDateTime").descending(),
            SortCriteria.THUMBNAIL_CREATION_DATE_TIME, Sort.by("thumbnailCreationDateTime").descending());

    private final AssetRepository assetRepository;
    private final AssetExifRepository assetExifRepository;
    private final FolderRepository folderRepository;
    private final CatalogRunStateRepository catalogRunStateRepository;
    private final RecentTargetPathRepository recentTargetPathRepository;
    private final SyncAssetsConfigRepository syncAssetsConfigRepository;
    private final ConvertAssetsConfigRepository convertAssetsConfigRepository;
    private final CatalogAssetsService catalogAssetsService;
    private final FindDuplicatedAssetsService findDuplicatedAssetsService;
    private final MoveAssetsService moveAssetsService;
    private final SyncAssetsService syncAssetsService;
    private final ConvertAssetsService convertAssetsService;
    private final StorageService storageService;

    @Value("${photomanager.initial-directory:${user.home}/Pictures}")
    private String initialDirectory;

    @Value("${photomanager.root-catalog-folders:${user.home}/Pictures}")
    private String rootCatalogFolders;

    @Override
    @Transactional(readOnly = true)
    public PaginatedData<Asset> getAssets(String folderPath, int pageIndex, SortCriteria sortCriteria) {
        Optional<Folder> folder = folderRepository.findByPath(folderPath);
        if (folder.isEmpty()) {
            return new PaginatedData<>(List.of(), 0, 0, 0);
        }

        Sort sort = buildSort(sortCriteria);
        PageRequest pageRequest = PageRequest.of(pageIndex, PAGE_SIZE, sort);
        Page<Asset> page = assetRepository.findByFolder(folder.get(), pageRequest);

        return new PaginatedData<>(page.getContent(), pageIndex, page.getTotalPages(), page.getTotalElements());
    }

    @Override
    public CompletableFuture<Void> catalogAssetsAsync(Consumer<CatalogChangeNotification> callback) {
        return catalogAssetsService.catalogAssetsAsync(callback);
    }

    @Override
    @Transactional(readOnly = true)
    public List<List<Asset>> getDuplicatedAssets() {
        return findDuplicatedAssetsService.getDuplicatedAssets();
    }

    @Override
    @Transactional
    public boolean moveAssets(Long[] assetIds, String destinationFolderPath, boolean preserveOriginal) {
        validateDestinationPath(destinationFolderPath);
        List<Asset> assets = assetRepository.findAllById(Arrays.asList(assetIds));
        Folder destinationFolder = folderRepository.findByPath(destinationFolderPath)
                .orElseGet(() -> {
                    Folder f = new Folder();
                    f.setPath(destinationFolderPath);
                    return folderRepository.save(f);
                });

        boolean result = moveAssetsService.moveAssets(assets.toArray(new Asset[0]), destinationFolder,
                preserveOriginal);

        if (result) {
            saveRecentTargetPath(destinationFolderPath);
        }
        return result;
    }

    @Override
    @Transactional
    public void deleteAssets(Long[] assetIds, boolean deleteFiles) {
        List<Asset> assets = assetRepository.findAllById(Arrays.asList(assetIds));
        moveAssetsService.deleteAssets(assets.toArray(new Asset[0]), deleteFiles);
    }

    @Override
    public CompletableFuture<List<SyncAssetsResult>> syncAssetsAsync(Consumer<String> statusCallback) {
        return syncAssetsService.executeAsync(statusCallback);
    }

    @Override
    public CompletableFuture<List<ConvertAssetsResult>> convertAssetsAsync(Consumer<String> statusCallback) {
        return convertAssetsService.executeAsync(statusCallback);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SyncAssetsDirectoriesDefinition> getSyncAssetsConfiguration() {
        return syncAssetsConfigRepository.findAllByOrderByOrderAsc();
    }

    @Override
    @Transactional
    public void setSyncAssetsConfiguration(List<SyncAssetsDirectoriesDefinition> definitions) {
        syncAssetsConfigRepository.deleteAllInBatch();
        for (int i = 0; i < definitions.size(); i++) {
            definitions.get(i).setId(null);
            definitions.get(i).setOrder(i);
        }
        syncAssetsConfigRepository.saveAll(definitions);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConvertAssetsDirectoriesDefinition> getConvertAssetsConfiguration() {
        return convertAssetsConfigRepository.findAllByOrderByOrderAsc();
    }

    @Override
    @Transactional
    public void setConvertAssetsConfiguration(List<ConvertAssetsDirectoriesDefinition> definitions) {
        convertAssetsConfigRepository.deleteAllInBatch();
        for (int i = 0; i < definitions.size(); i++) {
            definitions.get(i).setId(null);
            definitions.get(i).setOrder(i);
        }
        convertAssetsConfigRepository.saveAll(definitions);
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getRecentTargetPaths() {
        return recentTargetPathRepository.findAllByOrderByIdDesc().stream()
                .map(RecentTargetPath::getPath)
                .collect(Collectors.toList());
    }

    @Override
    public List<String> getDrives() {
        return Arrays.stream(File.listRoots())
                .map(File::getAbsolutePath)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Folder> getSubFolders(String parentPath) {
        if (parentPath == null || parentPath.isBlank()) {
            return folderRepository.findAll();
        }
        return folderRepository.findSubFolders(parentPath + "/");
    }

    @Override
    public String getInitialFolder() {
        return initialDirectory;
    }

    @Override
    @Transactional(readOnly = true)
    public AssetImage getAssetImage(Long assetId) throws IOException {
        Asset asset = assetRepository.findById(assetId)
                .orElseThrow(() -> new NoSuchElementException("Asset not found: " + assetId));
        String filePath = asset.getFolder().getPath() + "/" + asset.getFileName();
        byte[] bytes = storageService.readFileBytes(filePath);
        return new AssetImage(bytes, asset.getFileName());
    }

    @Override
    @Transactional(readOnly = true)
    public HomeStats getHomeStats() {
        long folderCount = folderRepository.count();
        long assetCount = assetRepository.count();
        var lastCompleted = catalogRunStateRepository.findById(1)
                .map(state -> state.getLastCompletedAt())
                .orElse(null);
        return new HomeStats(folderCount, assetCount, lastCompleted);
    }

    private void validateDestinationPath(String destinationFolderPath) {
        Path destination = Paths.get(destinationFolderPath).normalize().toAbsolutePath();
        boolean withinRoot = Arrays.stream(rootCatalogFolders.split(";"))
                .map(root -> Paths.get(root.trim()).normalize().toAbsolutePath())
                .anyMatch(destination::startsWith);
        if (!withinRoot) {
            throw new IllegalArgumentException("Destination path is outside the allowed catalog roots.");
        }
    }

    private void saveRecentTargetPath(String path) {
        if (!recentTargetPathRepository.existsByPath(path)) {
            recentTargetPathRepository.save(new RecentTargetPath(path));
            List<RecentTargetPath> all = recentTargetPathRepository.findAllByOrderByIdDesc();
            if (all.size() > MAX_RECENT_PATHS) {
                List<RecentTargetPath> toDelete = all.subList(MAX_RECENT_PATHS, all.size());
                recentTargetPathRepository.deleteAll(toDelete);
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public AssetExif getAssetExif(Long assetId) {
        assetRepository.findById(assetId)
                .orElseThrow(() -> new NoSuchElementException("Asset not found: " + assetId));
        return assetExifRepository.findByAssetAssetId(assetId).orElse(null);
    }

    private Sort buildSort(SortCriteria criteria) {
        if (criteria == null) {
            return DEFAULT_SORT;
        }
        return SORT_MAP.getOrDefault(criteria, DEFAULT_SORT);
    }
}
