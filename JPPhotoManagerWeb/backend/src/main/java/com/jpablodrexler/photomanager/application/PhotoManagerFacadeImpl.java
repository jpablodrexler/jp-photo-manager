package com.jpablodrexler.photomanager.application;

import com.jpablodrexler.photomanager.application.dto.AlbumData;
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
import com.jpablodrexler.photomanager.domain.service.RecycleBinService;
import lombok.RequiredArgsConstructor;
import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jpablodrexler.photomanager.api.exception.FolderNotFoundException;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.springframework.web.multipart.MultipartFile;
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
    private final CatalogFolderService catalogFolderService;
    private final CatalogAssetsService catalogAssetsService;
    private final FindDuplicatedAssetsService findDuplicatedAssetsService;
    private final MoveAssetsService moveAssetsService;
    private final SyncAssetsService syncAssetsService;
    private final ConvertAssetsService convertAssetsService;
    private final StorageService storageService;
    private final AlbumService albumService;
    private final AlbumRepository albumRepository;
    private final RecycleBinService recycleBinService;

    @Value("${photomanager.initial-directory:${user.home}/Pictures}")
    private String initialDirectory;

    @Value("${photomanager.root-catalog-folders:${user.home}/Pictures}")
    private String rootCatalogFolders;

    @Override
    @Transactional(readOnly = true)
    public PaginatedData<Asset> getAssets(String folderPath, int pageIndex, SortCriteria sortCriteria,
                                          String search, LocalDate dateFrom, LocalDate dateTo) {
        Optional<Folder> folder = folderRepository.findByPath(folderPath);
        if (folder.isEmpty()) {
            return new PaginatedData<>(List.of(), 0, 0, 0);
        }

        Sort sort = buildSort(sortCriteria);
        PageRequest pageRequest = PageRequest.of(pageIndex, PAGE_SIZE, sort);
        String searchParam = (search != null && !search.isBlank()) ? search.trim() : null;
        LocalDateTime dateFromDt = (dateFrom != null) ? dateFrom.atStartOfDay() : null;
        LocalDateTime dateToDt   = (dateTo   != null) ? dateTo.atTime(LocalTime.MAX) : null;
        Page<Asset> page = assetRepository.findByFolderWithFilters(
                folder.get(), searchParam, dateFromDt, dateToDt, pageRequest);

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

    @Override
    @Transactional
    public Asset uploadAsset(String folderPath, MultipartFile file) throws IOException {
        if (!folderRepository.existsByPath(folderPath)) {
            throw new FolderNotFoundException(folderPath);
        }
        Path tempFile = Files.createTempFile(UUID.randomUUID().toString() + "_", "_" + file.getOriginalFilename());
        try {
            file.transferTo(tempFile.toFile());
            String destPath = folderPath + "/" + file.getOriginalFilename();
            storageService.copyFile(tempFile.toString(), destPath);
            return catalogFolderService.createAsset(folderPath, file.getOriginalFilename());
        } finally {
            Files.deleteIfExists(tempFile);
        }
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

    @Override
    @Transactional(readOnly = true)
    public List<AlbumData> getAlbums(UUID userId) {
        return albumService.findByUserId(userId).stream()
                .map(this::toAlbumData)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public AlbumData createAlbum(UUID userId, String name, String description) {
        return toAlbumData(albumService.createAlbum(userId, name, description));
    }

    @Override
    @Transactional(readOnly = true)
    public AlbumData getAlbumSummary(Long albumId, UUID userId) {
        Album album = albumService.findByIdAndUserId(albumId, userId)
                .orElseThrow(() -> new com.jpablodrexler.photomanager.api.exception.AlbumNotFoundException(albumId));
        return toAlbumData(album);
    }

    @Override
    @Transactional(readOnly = true)
    public PaginatedData<Asset> getAlbumAssets(Long albumId, UUID userId, int pageIndex) {
        return albumService.getAlbumAssets(albumId, userId, pageIndex);
    }

    @Override
    @Transactional
    public AlbumData updateAlbum(Long albumId, UUID userId, String name, String description) {
        return toAlbumData(albumService.updateAlbum(albumId, userId, name, description));
    }

    @Override
    @Transactional
    public void deleteAlbum(Long albumId, UUID userId) {
        albumService.deleteAlbum(albumId, userId);
    }

    @Override
    @Transactional
    public void addAssetsToAlbum(Long albumId, UUID userId, List<Long> assetIds) {
        albumService.addAssets(albumId, userId, assetIds);
    }

    @Override
    @Transactional
    public void removeAssetsFromAlbum(Long albumId, UUID userId, List<Long> assetIds) {
        albumService.removeAssets(albumId, userId, assetIds);
    }

    @Override
    @Transactional(readOnly = true)
    public void downloadAssets(List<Long> assetIds, OutputStream out) throws IOException {
        List<Asset> assets = assetRepository.findAllById(assetIds);

        Set<String> seenNames = new HashSet<>();
        Map<Long, String> entryNameByAssetId = new java.util.LinkedHashMap<>();
        for (Asset asset : assets) {
            String name = asset.getFileName();
            if (!seenNames.add(name)) {
                name = asset.getAssetId() + "_" + asset.getFileName();
            }
            entryNameByAssetId.put(asset.getAssetId(), name);
        }

        ZipOutputStream zipOut = new ZipOutputStream(out);
        for (Asset asset : assets) {
            String entryName = entryNameByAssetId.get(asset.getAssetId());
            try {
                byte[] bytes = storageService.readFileBytes(asset.getFullPath());
                zipOut.putNextEntry(new ZipEntry(entryName));
                zipOut.write(bytes);
                zipOut.closeEntry();
            } catch (IOException e) {
                log.warn("Skipping unreadable asset {}: {}", asset.getAssetId(), e.getMessage());
            }
        }
        zipOut.finish();
    }

    @Override
    @Transactional(readOnly = true)
    public PaginatedData<Asset> getRecycleBin(int pageIndex) {
        return recycleBinService.getDeletedAssets(pageIndex);
    }

    @Override
    @Transactional
    public void restoreAssets(List<Long> assetIds) {
        recycleBinService.restoreAssets(assetIds);
    }

    @Override
    @Transactional
    public void purgeRecycleBin(List<Long> assetIds) {
        if (assetIds == null) {
            recycleBinService.purgeAll();
        } else {
            recycleBinService.purgeAssets(assetIds);
        }
    }

    private AlbumData toAlbumData(Album album) {
        long count = albumRepository.countAssets(album.getAlbumId());
        return new AlbumData(album.getAlbumId(), album.getName(), album.getDescription(), album.getCreatedAt(), count);
    }
}
