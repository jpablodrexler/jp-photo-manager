package com.jpablodrexler.photomanager.application.usecase.album;

import com.jpablodrexler.photomanager.application.exception.AlbumNotFoundException;
import com.jpablodrexler.photomanager.domain.model.Asset;
import com.jpablodrexler.photomanager.domain.model.AssetFilter;
import com.jpablodrexler.photomanager.domain.model.PaginatedResult;
import com.jpablodrexler.photomanager.domain.port.in.album.GetAlbumAssetsUseCase;
import com.jpablodrexler.photomanager.domain.port.out.AlbumRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetAlbumAssetsUseCaseImpl implements GetAlbumAssetsUseCase {

    private static final int PAGE_SIZE = 50;

    private final AlbumRepository albumRepository;
    private final AlbumAssetFilterFactory albumAssetFilterFactory;

    @Override
    @Transactional(readOnly = true)
    public PaginatedResult<Asset> execute(Long albumId, UUID userId, int page) {
        var album = albumRepository.findByIdAndUserId(albumId, userId)
                .orElseThrow(() -> new AlbumNotFoundException(albumId));
        if (album.getFilterJson() != null) {
            AssetFilter filter = albumAssetFilterFactory.build(album.getFilterJson(), page, PAGE_SIZE);
            return albumRepository.findSmartAlbumAssets(filter, page, PAGE_SIZE);
        }
        return albumRepository.findAssetsByAlbumId(albumId, page, PAGE_SIZE);
    }
}
