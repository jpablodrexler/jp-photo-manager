package com.jpablodrexler.photomanager.application.usecase.album;

import com.jpablodrexler.photomanager.application.exception.AlbumNotFoundException;
import com.jpablodrexler.photomanager.domain.model.AlbumData;
import com.jpablodrexler.photomanager.domain.model.AssetFilter;
import com.jpablodrexler.photomanager.domain.port.in.album.GetAlbumSummaryUseCase;
import com.jpablodrexler.photomanager.domain.port.out.AlbumRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetAlbumSummaryUseCaseImpl implements GetAlbumSummaryUseCase {

    private final AlbumRepository albumRepository;
    private final AlbumAssetFilterFactory albumAssetFilterFactory;

    @Override
    @Transactional(readOnly = true)
    public AlbumData execute(Long albumId, UUID userId) {
        var album = albumRepository.findByIdAndUserId(albumId, userId)
                .orElseThrow(() -> new AlbumNotFoundException(albumId));
        long count;
        if (album.getFilterJson() != null) {
            AssetFilter filter = albumAssetFilterFactory.build(album.getFilterJson(), 0, 1);
            count = albumRepository.findSmartAlbumAssets(filter, 0, 1).total();
        } else {
            count = albumRepository.countAssets(albumId);
        }
        return new AlbumData(album.getAlbumId(), album.getName(), album.getDescription(), album.getCreatedAt(), count, album.getFilterJson());
    }
}
