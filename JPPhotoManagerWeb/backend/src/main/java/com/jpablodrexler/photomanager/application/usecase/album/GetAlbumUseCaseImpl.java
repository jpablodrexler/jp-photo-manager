package com.jpablodrexler.photomanager.application.usecase.album;

import com.jpablodrexler.photomanager.application.exception.AlbumNotFoundException;
import com.jpablodrexler.photomanager.application.dto.AlbumData;
import com.jpablodrexler.photomanager.application.dto.PaginatedResult;
import com.jpablodrexler.photomanager.domain.model.Asset;
import com.jpablodrexler.photomanager.domain.port.in.album.GetAlbumUseCase;
import com.jpablodrexler.photomanager.domain.port.out.AlbumRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetAlbumUseCaseImpl implements GetAlbumUseCase {

    private static final int PAGE_SIZE = 50;

    private final AlbumRepository albumRepository;

    @Override
    @Transactional(readOnly = true)
    public AlbumData executeSummary(Long albumId, UUID userId) {
        var album = albumRepository.findByIdAndUserId(albumId, userId)
                .orElseThrow(() -> new AlbumNotFoundException(albumId));
        long count = albumRepository.countAssets(albumId);
        return new AlbumData(album.getAlbumId(), album.getName(), album.getDescription(), album.getCreatedAt(), count);
    }

    @Override
    @Transactional(readOnly = true)
    public PaginatedResult<Asset> executeAssets(Long albumId, UUID userId, int page) {
        albumRepository.findByIdAndUserId(albumId, userId)
                .orElseThrow(() -> new AlbumNotFoundException(albumId));
        return albumRepository.findAssetsByAlbumId(albumId, page, PAGE_SIZE);
    }
}
