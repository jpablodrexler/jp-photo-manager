package com.jpablodrexler.photomanager.infrastructure.service;

import com.jpablodrexler.photomanager.domain.entity.Asset;
import com.jpablodrexler.photomanager.domain.repository.AssetRepository;
import com.jpablodrexler.photomanager.domain.service.FindDuplicatedAssetsService;
import com.jpablodrexler.photomanager.domain.service.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FindDuplicatedAssetsServiceImpl implements FindDuplicatedAssetsService {

    private final AssetRepository assetRepository;
    private final StorageService storageService;

    @Override
    @Transactional(readOnly = true)
    public List<List<Asset>> getDuplicatedAssets() {
        List<Asset> allAssets = assetRepository.findByDeletedAtIsNull();

        // Remove stale assets (files deleted externally)
        List<Asset> validAssets = allAssets.stream()
                .filter(a -> {
                    String path = a.getFolder().getPath() + "/" + a.getFileName();
                    boolean exists = storageService.directoryExists(a.getFolder().getPath())
                            && storageService.getFileSize(path) > 0;
                    return exists;
                })
                .collect(Collectors.toList());

        // Group by hash
        Map<String, List<Asset>> groupedByHash = validAssets.stream()
                .collect(Collectors.groupingBy(Asset::getHash));

        // Return only groups with duplicates
        List<List<Asset>> duplicates = new ArrayList<>();
        for (Map.Entry<String, List<Asset>> entry : groupedByHash.entrySet()) {
            if (entry.getValue().size() > 1) {
                duplicates.add(entry.getValue());
            }
        }

        return duplicates;
    }
}
