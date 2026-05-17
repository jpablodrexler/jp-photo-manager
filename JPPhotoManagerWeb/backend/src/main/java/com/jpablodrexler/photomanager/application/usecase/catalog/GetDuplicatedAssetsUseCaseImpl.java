package com.jpablodrexler.photomanager.application.usecase.catalog;

import com.jpablodrexler.photomanager.domain.model.Asset;
import com.jpablodrexler.photomanager.domain.port.in.catalog.GetDuplicatedAssetsUseCase;
import com.jpablodrexler.photomanager.domain.port.out.AssetRepository;
import com.jpablodrexler.photomanager.domain.port.out.StoragePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GetDuplicatedAssetsUseCaseImpl implements GetDuplicatedAssetsUseCase {

    private final AssetRepository assetRepository;
    private final StoragePort storagePort;

    @Override
    @Transactional(readOnly = true)
    public List<List<Asset>> execute() {
        List<Asset> allAssets = assetRepository.findNotDeleted();

        List<Asset> validAssets = allAssets.stream()
                .filter(a -> {
                    String path = a.getFolder().getPath() + "/" + a.getFileName();
                    return storagePort.directoryExists(a.getFolder().getPath())
                            && storagePort.getFileSize(path) > 0;
                })
                .collect(Collectors.toList());

        Map<String, List<Asset>> groupedByHash = validAssets.stream()
                .collect(Collectors.groupingBy(Asset::getHash));

        List<List<Asset>> duplicates = new ArrayList<>();
        for (Map.Entry<String, List<Asset>> entry : groupedByHash.entrySet()) {
            if (entry.getValue().size() > 1) {
                duplicates.add(entry.getValue());
            }
        }
        return duplicates;
    }
}
