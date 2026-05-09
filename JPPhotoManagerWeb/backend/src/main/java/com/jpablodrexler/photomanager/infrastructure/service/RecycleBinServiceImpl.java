package com.jpablodrexler.photomanager.infrastructure.service;

import com.jpablodrexler.photomanager.application.dto.PaginatedData;
import com.jpablodrexler.photomanager.domain.entity.Asset;
import com.jpablodrexler.photomanager.domain.repository.AssetRepository;
import com.jpablodrexler.photomanager.domain.service.RecycleBinService;
import com.jpablodrexler.photomanager.domain.service.StorageService;
import com.jpablodrexler.photomanager.domain.service.ThumbnailStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecycleBinServiceImpl implements RecycleBinService {

    private static final int PAGE_SIZE = 100;

    private final AssetRepository assetRepository;
    private final StorageService storageService;
    private final ThumbnailStorageService thumbnailStorageService;

    @Value("${photomanager.recycle-bin-retention-days:30}")
    private int retentionDays;

    @Override
    @Transactional(readOnly = true)
    public PaginatedData<Asset> getDeletedAssets(int pageIndex) {
        Page<Asset> page = assetRepository.findByDeletedAtIsNotNullOrderByDeletedAtDesc(
                PageRequest.of(pageIndex, PAGE_SIZE));
        return new PaginatedData<>(page.getContent(), pageIndex, page.getTotalPages(), page.getTotalElements());
    }

    @Override
    @Transactional
    public void restoreAssets(List<Long> assetIds) {
        List<Asset> assets = assetRepository.findAllById(assetIds);
        for (Asset asset : assets) {
            asset.setDeletedAt(null);
            assetRepository.save(asset);
        }
    }

    @Override
    @Transactional
    public void purgeAssets(List<Long> assetIds) {
        List<Asset> assets = assetRepository.findAllById(assetIds);
        for (Asset asset : assets) {
            try {
                storageService.deleteFile(asset.getFullPath());
            } catch (IOException e) {
                log.warn("Could not delete file for asset {}: {}", asset.getAssetId(), e.getMessage());
            }
            thumbnailStorageService.deleteThumbnail(asset.getThumbnailBlobName());
            assetRepository.delete(asset);
        }
    }

    @Override
    @Transactional
    public void purgeAll() {
        List<Asset> assets = assetRepository.findByDeletedAtIsNotNullOrderByDeletedAtDesc(Pageable.unpaged())
                .getContent();
        List<Long> ids = assets.stream().map(Asset::getAssetId).toList();
        purgeAssets(ids);
    }

    @Override
    @Transactional
    public void purgeExpired(int retentionDays) {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(retentionDays);
        List<Asset> assets = assetRepository.findByDeletedAtBeforeAndDeletedAtIsNotNull(cutoff);
        List<Long> ids = assets.stream().map(Asset::getAssetId).toList();
        if (!ids.isEmpty()) {
            purgeAssets(ids);
            log.info("Auto-purged {} expired recycle-bin assets (older than {} days)", ids.size(), retentionDays);
        }
    }

    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void autoPurgeExpired() {
        purgeExpired(retentionDays);
    }
}
