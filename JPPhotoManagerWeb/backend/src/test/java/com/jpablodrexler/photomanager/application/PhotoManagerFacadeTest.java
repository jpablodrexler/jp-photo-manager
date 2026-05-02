package com.jpablodrexler.photomanager.application;

import com.jpablodrexler.photomanager.application.dto.AssetImage;
import com.jpablodrexler.photomanager.application.dto.ConvertAssetsResult;
import com.jpablodrexler.photomanager.application.dto.PaginatedData;
import com.jpablodrexler.photomanager.application.dto.SyncAssetsResult;
import com.jpablodrexler.photomanager.domain.entity.*;
import com.jpablodrexler.photomanager.domain.enums.SortCriteria;
import com.jpablodrexler.photomanager.domain.repository.*;
import com.jpablodrexler.photomanager.domain.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PhotoManagerFacadeTest {

    @Mock
    AssetRepository assetRepository;
    @Mock
    FolderRepository folderRepository;
    @Mock
    RecentTargetPathRepository recentTargetPathRepository;
    @Mock
    SyncAssetsConfigRepository syncAssetsConfigRepository;
    @Mock
    ConvertAssetsConfigRepository convertAssetsConfigRepository;
    @Mock
    CatalogAssetsService catalogAssetsService;
    @Mock
    FindDuplicatedAssetsService findDuplicatedAssetsService;
    @Mock
    MoveAssetsService moveAssetsService;
    @Mock
    SyncAssetsService syncAssetsService;
    @Mock
    ConvertAssetsService convertAssetsService;
    @Mock
    StorageService storageService;

    @InjectMocks
    PhotoManagerFacadeImpl sut;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(sut, "initialDirectory", "/home/user/Pictures");
        // Use "/" as the catalog root so all test paths (e.g. "/dest", "/src") pass validation.
        ReflectionTestUtils.setField(sut, "rootCatalogFolders", "/");
    }

    // --- getAssets ---

    @Test
    void getAssets_folderNotFound_returnsEmptyPaginatedData() {
        when(folderRepository.findByPath("/photos")).thenReturn(Optional.empty());

        PaginatedData<Asset> result = sut.getAssets("/photos", 0, SortCriteria.FILE_NAME);

        assertThat(result.getItems()).isEmpty();
        assertThat(result.getTotalItems()).isZero();
    }

    @Test
    void getAssets_folderFound_returnsPaginatedAssets() {
        Folder folder = buildFolder(1L, "/photos");
        Asset asset = buildAsset(folder, "photo.jpg");
        Page<Asset> page = new PageImpl<>(List.of(asset));

        when(folderRepository.findByPath("/photos")).thenReturn(Optional.of(folder));
        when(assetRepository.findByFolder(eq(folder), any(Pageable.class))).thenReturn(page);

        PaginatedData<Asset> result = sut.getAssets("/photos", 0, SortCriteria.FILE_NAME);

        assertThat(result.getItems()).hasSize(1);
        assertThat(result.getTotalItems()).isEqualTo(1);
        assertThat(result.getPageIndex()).isZero();
    }

    @Test
    void getAssets_nullSortCriteria_usesDefaultSort() {
        Folder folder = buildFolder(1L, "/photos");
        Page<Asset> page = new PageImpl<>(List.of());

        when(folderRepository.findByPath("/photos")).thenReturn(Optional.of(folder));
        when(assetRepository.findByFolder(eq(folder), any(Pageable.class))).thenReturn(page);

        PaginatedData<Asset> result = sut.getAssets("/photos", 0, null);

        assertThat(result.getItems()).isEmpty();
    }

    // --- catalogAssetsAsync ---

    @Test
    void catalogAssetsAsync_delegatesToCatalogAssetsService() {
        CompletableFuture<Void> future = CompletableFuture.completedFuture(null);
        when(catalogAssetsService.catalogAssetsAsync(any())).thenReturn(future);

        CompletableFuture<Void> result = sut.catalogAssetsAsync(null);

        assertThat(result).isSameAs(future);
    }

    // --- getDuplicatedAssets ---

    @Test
    void getDuplicatedAssets_delegatesToFindDuplicatedAssetsService() {
        List<List<Asset>> duplicates = List.of(List.of(new Asset(), new Asset()));
        when(findDuplicatedAssetsService.getDuplicatedAssets()).thenReturn(duplicates);

        List<List<Asset>> result = sut.getDuplicatedAssets();

        assertThat(result).isSameAs(duplicates);
    }

    // --- moveAssets ---

    @Test
    void moveAssets_destinationFolderExists_usesExistingFolderWithoutCreatingNew() {
        Folder dest = buildFolder(2L, "/dest");
        Asset asset = buildAsset(buildFolder(1L, "/source"), "photo.jpg");
        asset.setAssetId(10L);

        when(assetRepository.findAllById(List.of(10L))).thenReturn(List.of(asset));
        when(folderRepository.findByPath("/dest")).thenReturn(Optional.of(dest));
        when(moveAssetsService.moveAssets(any(), eq(dest), eq(false))).thenReturn(true);
        when(recentTargetPathRepository.existsByPath("/dest")).thenReturn(true);

        boolean result = sut.moveAssets(new Long[] { 10L }, "/dest", false);

        assertThat(result).isTrue();
        verify(folderRepository, never()).save(any());
    }

    @Test
    void moveAssets_destinationFolderMissing_createsAndPersistsNewFolder() {
        Asset asset = buildAsset(buildFolder(1L, "/source"), "photo.jpg");
        asset.setAssetId(10L);
        Folder savedFolder = buildFolder(99L, "/newdest");

        when(assetRepository.findAllById(List.of(10L))).thenReturn(List.of(asset));
        when(folderRepository.findByPath("/newdest")).thenReturn(Optional.empty());
        when(folderRepository.save(any())).thenReturn(savedFolder);
        when(moveAssetsService.moveAssets(any(), eq(savedFolder), eq(false))).thenReturn(true);
        when(recentTargetPathRepository.existsByPath("/newdest")).thenReturn(true);

        sut.moveAssets(new Long[] { 10L }, "/newdest", false);

        verify(folderRepository).save(argThat(f -> "/newdest".equals(f.getPath())));
    }

    @Test
    void moveAssets_moveSucceeds_savesRecentTargetPath() {
        Folder dest = buildFolder(2L, "/dest");
        Asset asset = buildAsset(buildFolder(1L, "/src"), "photo.jpg");
        asset.setAssetId(5L);

        when(assetRepository.findAllById(List.of(5L))).thenReturn(List.of(asset));
        when(folderRepository.findByPath("/dest")).thenReturn(Optional.of(dest));
        when(moveAssetsService.moveAssets(any(), eq(dest), eq(false))).thenReturn(true);
        when(recentTargetPathRepository.existsByPath("/dest")).thenReturn(false);
        when(recentTargetPathRepository.findAllByOrderByIdDesc()).thenReturn(List.of());

        sut.moveAssets(new Long[] { 5L }, "/dest", false);

        verify(recentTargetPathRepository).save(argThat(r -> "/dest".equals(r.getPath())));
    }

    @Test
    void moveAssets_moveFails_doesNotSaveRecentTargetPath() {
        Folder dest = buildFolder(2L, "/dest");
        Asset asset = buildAsset(buildFolder(1L, "/src"), "photo.jpg");
        asset.setAssetId(5L);

        when(assetRepository.findAllById(List.of(5L))).thenReturn(List.of(asset));
        when(folderRepository.findByPath("/dest")).thenReturn(Optional.of(dest));
        when(moveAssetsService.moveAssets(any(), eq(dest), eq(false))).thenReturn(false);

        boolean result = sut.moveAssets(new Long[] { 5L }, "/dest", false);

        assertThat(result).isFalse();
        verify(recentTargetPathRepository, never()).save(any());
    }

    @Test
    void moveAssets_pathAlreadyExists_doesNotSaveDuplicate() {
        Folder dest = buildFolder(2L, "/dest");
        Asset asset = buildAsset(buildFolder(1L, "/src"), "photo.jpg");
        asset.setAssetId(5L);

        when(assetRepository.findAllById(List.of(5L))).thenReturn(List.of(asset));
        when(folderRepository.findByPath("/dest")).thenReturn(Optional.of(dest));
        when(moveAssetsService.moveAssets(any(), eq(dest), eq(false))).thenReturn(true);
        when(recentTargetPathRepository.existsByPath("/dest")).thenReturn(true);

        sut.moveAssets(new Long[] { 5L }, "/dest", false);

        verify(recentTargetPathRepository, never()).save(any());
    }

    @Test
    void moveAssets_moreThan20RecentPaths_deletesOldestEntries() {
        Folder dest = buildFolder(2L, "/dest");
        Asset asset = buildAsset(buildFolder(1L, "/src"), "photo.jpg");
        asset.setAssetId(5L);

        List<RecentTargetPath> existingPaths = new ArrayList<>();
        for (int i = 0; i < 21; i++) {
            existingPaths.add(new RecentTargetPath("/path" + i));
        }

        when(assetRepository.findAllById(List.of(5L))).thenReturn(List.of(asset));
        when(folderRepository.findByPath("/dest")).thenReturn(Optional.of(dest));
        when(moveAssetsService.moveAssets(any(), eq(dest), eq(false))).thenReturn(true);
        when(recentTargetPathRepository.existsByPath("/dest")).thenReturn(false);
        when(recentTargetPathRepository.findAllByOrderByIdDesc()).thenReturn(existingPaths);

        sut.moveAssets(new Long[] { 5L }, "/dest", false);

        verify(recentTargetPathRepository).deleteAll(existingPaths.subList(20, 21));
    }

    @Test
    void moveAssets_destinationOutsideAllowedRoots_throwsIllegalArgumentException() {
        ReflectionTestUtils.setField(sut, "rootCatalogFolders", "/tmp/photos");

        assertThatThrownBy(() -> sut.moveAssets(new Long[] { 1L }, "/etc/cron.d", false))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void moveAssets_pathTraversalInDestination_throwsIllegalArgumentException() {
        ReflectionTestUtils.setField(sut, "rootCatalogFolders", "/tmp/photos");

        assertThatThrownBy(() -> sut.moveAssets(new Long[] { 1L }, "/tmp/photos/../../../etc", false))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // --- deleteAssets ---

    @Test
    void deleteAssets_delegatesToMoveAssetsService() {
        Asset asset = buildAsset(buildFolder(1L, "/src"), "photo.jpg");
        asset.setAssetId(7L);
        when(assetRepository.findAllById(List.of(7L))).thenReturn(List.of(asset));

        sut.deleteAssets(new Long[] { 7L }, true);

        verify(moveAssetsService).deleteAssets(any(), eq(true));
    }

    // --- syncAssetsAsync / convertAssetsAsync ---

    @Test
    void syncAssetsAsync_delegatesToSyncAssetsService() {
        CompletableFuture<List<SyncAssetsResult>> future = CompletableFuture.completedFuture(List.of());
        when(syncAssetsService.executeAsync(any())).thenReturn(future);

        CompletableFuture<List<SyncAssetsResult>> result = sut.syncAssetsAsync(null);

        assertThat(result).isSameAs(future);
    }

    @Test
    void convertAssetsAsync_delegatesToConvertAssetsService() {
        CompletableFuture<List<ConvertAssetsResult>> future = CompletableFuture.completedFuture(List.of());
        when(convertAssetsService.executeAsync(any())).thenReturn(future);

        CompletableFuture<List<ConvertAssetsResult>> result = sut.convertAssetsAsync(null);

        assertThat(result).isSameAs(future);
    }

    // --- getSyncAssetsConfiguration / setSyncAssetsConfiguration ---

    @Test
    void getSyncAssetsConfiguration_delegatesToRepository() {
        List<SyncAssetsDirectoriesDefinition> defs = List.of(new SyncAssetsDirectoriesDefinition());
        when(syncAssetsConfigRepository.findAllByOrderByOrderAsc()).thenReturn(defs);

        List<SyncAssetsDirectoriesDefinition> result = sut.getSyncAssetsConfiguration();

        assertThat(result).isSameAs(defs);
    }

    @Test
    void setSyncAssetsConfiguration_assignsOrderAndPersists() {
        SyncAssetsDirectoriesDefinition def0 = new SyncAssetsDirectoriesDefinition();
        SyncAssetsDirectoriesDefinition def1 = new SyncAssetsDirectoriesDefinition();
        List<SyncAssetsDirectoriesDefinition> defs = List.of(def0, def1);

        sut.setSyncAssetsConfiguration(defs);

        assertThat(def0.getOrder()).isZero();
        assertThat(def1.getOrder()).isEqualTo(1);
        assertThat(def0.getId()).isNull();
        assertThat(def1.getId()).isNull();
        verify(syncAssetsConfigRepository).deleteAllInBatch();
        verify(syncAssetsConfigRepository).saveAll(defs);
    }

    // --- getConvertAssetsConfiguration / setConvertAssetsConfiguration ---

    @Test
    void getConvertAssetsConfiguration_delegatesToRepository() {
        List<ConvertAssetsDirectoriesDefinition> defs = List.of(new ConvertAssetsDirectoriesDefinition());
        when(convertAssetsConfigRepository.findAllByOrderByOrderAsc()).thenReturn(defs);

        List<ConvertAssetsDirectoriesDefinition> result = sut.getConvertAssetsConfiguration();

        assertThat(result).isSameAs(defs);
    }

    @Test
    void setConvertAssetsConfiguration_assignsOrderAndPersists() {
        ConvertAssetsDirectoriesDefinition def0 = new ConvertAssetsDirectoriesDefinition();
        ConvertAssetsDirectoriesDefinition def1 = new ConvertAssetsDirectoriesDefinition();
        List<ConvertAssetsDirectoriesDefinition> defs = List.of(def0, def1);

        sut.setConvertAssetsConfiguration(defs);

        assertThat(def0.getOrder()).isZero();
        assertThat(def1.getOrder()).isEqualTo(1);
        assertThat(def0.getId()).isNull();
        assertThat(def1.getId()).isNull();
        verify(convertAssetsConfigRepository).deleteAllInBatch();
        verify(convertAssetsConfigRepository).saveAll(defs);
    }

    // --- getRecentTargetPaths ---

    @Test
    void getRecentTargetPaths_mapsRepositoryResultsToPaths() {
        RecentTargetPath rtp = new RecentTargetPath("/recent");
        when(recentTargetPathRepository.findAllByOrderByIdDesc()).thenReturn(List.of(rtp));

        List<String> result = sut.getRecentTargetPaths();

        assertThat(result).containsExactly("/recent");
    }

    // --- getDrives ---

    @Test
    void getDrives_returnsNonEmptyList() {
        List<String> drives = sut.getDrives();

        assertThat(drives).isNotEmpty();
    }

    // --- getSubFolders ---

    @Test
    void getSubFolders_nullParentPath_returnsAllFolders() {
        List<Folder> all = List.of(buildFolder(1L, "/photos"));
        when(folderRepository.findAll()).thenReturn(all);

        List<Folder> result = sut.getSubFolders(null);

        assertThat(result).isSameAs(all);
        verify(folderRepository, never()).findSubFolders(any());
    }

    @Test
    void getSubFolders_blankParentPath_returnsAllFolders() {
        List<Folder> all = List.of(buildFolder(1L, "/photos"));
        when(folderRepository.findAll()).thenReturn(all);

        List<Folder> result = sut.getSubFolders("   ");

        assertThat(result).isSameAs(all);
        verify(folderRepository, never()).findSubFolders(any());
    }

    @Test
    void getSubFolders_withParentPath_queriesSubFoldersWithTrailingSlash() {
        List<Folder> subs = List.of(buildFolder(2L, "/photos/2024"));
        when(folderRepository.findSubFolders("/photos/")).thenReturn(subs);

        List<Folder> result = sut.getSubFolders("/photos");

        assertThat(result).isSameAs(subs);
    }

    // --- getInitialFolder ---

    @Test
    void getInitialFolder_returnsConfiguredDirectory() {
        String result = sut.getInitialFolder();

        assertThat(result).isEqualTo("/home/user/Pictures");
    }

    // --- getAssetImage ---

    @Test
    void getAssetImage_assetFound_returnsBytesAndFileName() throws IOException {
        Folder folder = buildFolder(1L, "/photos");
        Asset asset = buildAsset(folder, "photo.jpg");
        asset.setAssetId(42L);
        when(assetRepository.findById(42L)).thenReturn(Optional.of(asset));
        when(storageService.readFileBytes("/photos/photo.jpg")).thenReturn(new byte[] { 1, 2, 3 });

        AssetImage result = sut.getAssetImage(42L);

        assertThat(result.fileName()).isEqualTo("photo.jpg");
        assertThat(result.bytes()).isEqualTo(new byte[] { 1, 2, 3 });
    }

    @Test
    void getAssetImage_assetNotFound_throwsNoSuchElementException() {
        when(assetRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sut.getAssetImage(99L))
                .isInstanceOf(NoSuchElementException.class);
    }

    // --- helpers ---

    private Folder buildFolder(Long id, String path) {
        Folder folder = new Folder();
        folder.setFolderId(id);
        folder.setPath(path);
        return folder;
    }

    private Asset buildAsset(Folder folder, String fileName) {
        Asset asset = new Asset();
        asset.setFolder(folder);
        asset.setFileName(fileName);
        return asset;
    }
}
