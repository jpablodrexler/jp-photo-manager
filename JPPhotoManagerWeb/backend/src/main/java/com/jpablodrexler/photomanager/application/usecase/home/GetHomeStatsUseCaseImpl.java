package com.jpablodrexler.photomanager.application.usecase.home;

import com.jpablodrexler.photomanager.application.dto.HomeStats;
import com.jpablodrexler.photomanager.domain.port.in.home.GetHomeStatsUseCase;
import com.jpablodrexler.photomanager.domain.port.out.AssetRepository;
import com.jpablodrexler.photomanager.domain.port.out.CatalogStateRepository;
import com.jpablodrexler.photomanager.domain.port.out.FolderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetHomeStatsUseCaseImpl implements GetHomeStatsUseCase {

    private final FolderRepository folderRepository;
    private final AssetRepository assetRepository;
    private final CatalogStateRepository catalogStateRepository;

    @Override
    @Transactional(readOnly = true)
    public HomeStats execute() {
        long folderCount = folderRepository.count();
        long assetCount = assetRepository.count();
        var lastCompleted = catalogStateRepository.find()
                .map(state -> state.getLastCompletedAt())
                .orElse(null);
        return new HomeStats(folderCount, assetCount, lastCompleted);
    }
}
