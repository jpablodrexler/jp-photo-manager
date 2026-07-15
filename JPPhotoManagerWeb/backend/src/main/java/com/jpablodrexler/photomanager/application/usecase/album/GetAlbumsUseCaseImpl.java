package com.jpablodrexler.photomanager.application.usecase.album;

import com.jpablodrexler.photomanager.domain.model.AlbumData;
import com.jpablodrexler.photomanager.domain.model.AssetFilter;
import com.jpablodrexler.photomanager.domain.model.Album;
import com.jpablodrexler.photomanager.domain.port.in.album.GetAlbumsUseCase;
import com.jpablodrexler.photomanager.domain.port.out.AlbumRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GetAlbumsUseCaseImpl implements GetAlbumsUseCase {

    private final AlbumRepository albumRepository;
    private final AlbumAssetFilterFactory albumAssetFilterFactory;

    @Override
    @Transactional(readOnly = true)
    public List<AlbumData> execute(UUID userId) {
        return albumRepository.findByUserId(userId).stream()
                .map(album -> new AlbumData(
                        album.getAlbumId(),
                        album.getName(),
                        album.getDescription(),
                        album.getCreatedAt(),
                        countAssets(album),
                        album.getFilterJson()))
                .collect(Collectors.toList());
    }

    private long countAssets(Album album) {
        if (album.getFilterJson() != null) {
            AssetFilter filter = albumAssetFilterFactory.build(album.getFilterJson(), 0, 1);
            return albumRepository.findSmartAlbumAssets(filter, 0, 1).total();
        }
        return albumRepository.countAssets(album.getAlbumId());
    }
}
