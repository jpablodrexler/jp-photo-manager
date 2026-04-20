package com.jpablodrexler.photomanager.application;

import com.jpablodrexler.photomanager.application.dto.*;
import com.jpablodrexler.photomanager.domain.entity.*;
import com.jpablodrexler.photomanager.domain.enums.SortCriteria;
import com.jpablodrexler.photomanager.domain.repository.*;
import com.jpablodrexler.photomanager.domain.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.nio.file.FileSystems;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PhotoManagerFacade {

    private static final int PAGE_SIZE = 100;
    private static final int MAX_RECENT_PATHS = 20;

    private final AssetRepository assetRepository;
    private final FolderRepository folderRepository;
    private final RecentTargetPathRepository recentTargetPathRepository;
    private final SyncAssetsConfigRepository syncAssetsConfigRepository;
    private final ConvertAssetsConfigRepository convertAssetsConfigRepository;
    private final CatalogAssetsService catalogAssetsService;
    private final FindDuplicatedAssetsService findDuplicatedAssetsService;
    private final MoveAssetsService moveAssetsService;
    private final SyncAssetsService syncAssetsService;
    private final ConvertAssetsService convertAssetsService;

    @Value("${photomanager.initial-directory:${user.home}/Pictures}")
    private String initialDirectory;

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

    public CompletableFuture<Void> catalogAssetsAsync(Consumer<CatalogChangeNotification> callback) {
        return catalogAssetsService.catalogAssetsAsync(callback);
    }

    @Transactional(readOnly = true)
    public List<List<Asset>> getDuplicatedAssets() {
        return findDuplicatedAssetsService.getDuplicatedAssets();
    }

    @Transactional
    public boolean moveAssets(Long[] assetIds, String destinationFolderPath, boolean preserveOriginal) {
        List<Asset> assets = assetRepository.findAllById(Arrays.asList(assetIds));
        Folder destinationFolder = folderRepository.findByPath(destinationFolderPath)
                .orElseGet(() -> {
                    Folder f = new Folder();
                    f.setPath(destinationFolderPath);
                    return folderRepository.save(f);
                });

        boolean result = moveAssetsService.moveAssets(assets.toArray(new Asset[0]), destinationFolder, preserveOriginal);

        if (result) {
            saveRecentTargetPath(destinationFolderPath);
        }
        return result;
    }

    @Transactional
    public void deleteAssets(Long[] assetIds, boolean deleteFiles) {
        List<Asset> assets = assetRepository.findAllById(Arrays.asList(assetIds));
        moveAssetsService.deleteAssets(assets.toArray(new Asset[0]), deleteFiles);
    }

    public CompletableFuture<List<SyncAssetsResult>> syncAssetsAsync(Consumer<String> statusCallback) {
        return syncAssetsService.executeAsync(statusCallback);
    }

    public CompletableFuture<List<ConvertAssetsResult>> convertAssetsAsync(Consumer<String> statusCallback) {
        return convertAssetsService.executeAsync(statusCallback);
    }

    @Transactional(readOnly = true)
    public List<SyncAssetsDirectoriesDefinition> getSyncAssetsConfiguration() {
        return syncAssetsConfigRepository.findAllByOrderByOrderAsc();
    }

    @Transactional
    public void setSyncAssetsConfiguration(List<SyncAssetsDirectoriesDefinition> definitions) {
        syncAssetsConfigRepository.deleteAll();
        for (int i = 0; i < definitions.size(); i++) {
            definitions.get(i).setOrder(i);
        }
        syncAssetsConfigRepository.saveAll(definitions);
    }

    @Transactional(readOnly = true)
    public List<ConvertAssetsDirectoriesDefinition> getConvertAssetsConfiguration() {
        return convertAssetsConfigRepository.findAllByOrderByOrderAsc();
    }

    @Transactional
    public void setConvertAssetsConfiguration(List<ConvertAssetsDirectoriesDefinition> definitions) {
        convertAssetsConfigRepository.deleteAll();
        for (int i = 0; i < definitions.size(); i++) {
            definitions.get(i).setOrder(i);
        }
        convertAssetsConfigRepository.saveAll(definitions);
    }

    @Transactional(readOnly = true)
    public List<String> getRecentTargetPaths() {
        return recentTargetPathRepository.findAllByOrderByIdDesc().stream()
                .map(RecentTargetPath::getPath)
                .collect(Collectors.toList());
    }

    public List<String> getDrives() {
        return Arrays.stream(File.listRoots())
                .map(File::getAbsolutePath)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<Folder> getSubFolders(String parentPath) {
        if (parentPath == null || parentPath.isBlank()) {
            return folderRepository.findAll();
        }
        return folderRepository.findSubFolders(parentPath + "/");
    }

    public String getInitialFolder() {
        return initialDirectory;
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

    private Sort buildSort(SortCriteria criteria) {
        if (criteria == null) return Sort.by("fileName").ascending();
        return switch (criteria) {
            case FILE_NAME -> Sort.by("fileName").ascending();
            case FILE_SIZE -> Sort.by("fileSize").descending();
            case FILE_CREATION_DATE_TIME -> Sort.by("fileCreationDateTime").descending();
            case FILE_MODIFICATION_DATE_TIME -> Sort.by("fileModificationDateTime").descending();
            case THUMBNAIL_CREATION_DATE_TIME -> Sort.by("thumbnailCreationDateTime").descending();
            default -> Sort.by("fileName").ascending();
        };
    }
}
