package com.jpablodrexler.photomanager.application.usecase.album;

import com.jpablodrexler.photomanager.application.exception.AlbumNotFoundException;
import com.jpablodrexler.photomanager.domain.port.in.album.RemoveAssetsFromAlbumUseCase;
import com.jpablodrexler.photomanager.domain.port.out.AlbumRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RemoveAssetsFromAlbumUseCaseImpl implements RemoveAssetsFromAlbumUseCase {

    private final AlbumRepository albumRepository;

    @Override
    @Transactional
    public void execute(Long albumId, UUID userId, List<Long> assetIds) {
        albumRepository.findByIdAndUserId(albumId, userId)
                .orElseThrow(() -> new AlbumNotFoundException(albumId));
        albumRepository.removeAssets(albumId, assetIds);
    }
}
