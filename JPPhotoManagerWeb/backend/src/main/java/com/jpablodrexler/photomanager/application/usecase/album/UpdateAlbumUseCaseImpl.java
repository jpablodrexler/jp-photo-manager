package com.jpablodrexler.photomanager.application.usecase.album;

import com.jpablodrexler.photomanager.application.exception.AlbumNotFoundException;
import com.jpablodrexler.photomanager.application.dto.AlbumData;
import com.jpablodrexler.photomanager.domain.port.in.album.UpdateAlbumUseCase;
import com.jpablodrexler.photomanager.domain.port.out.AlbumRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UpdateAlbumUseCaseImpl implements UpdateAlbumUseCase {

    private final AlbumRepository albumRepository;

    @Override
    @Transactional
    public AlbumData execute(Long albumId, UUID userId, String name, String description) {
        var album = albumRepository.findByIdAndUserId(albumId, userId)
                .orElseThrow(() -> new AlbumNotFoundException(albumId));
        album.setName(name);
        album.setDescription(description);
        var saved = albumRepository.save(album);
        long count = albumRepository.countAssets(albumId);
        return new AlbumData(saved.getAlbumId(), saved.getName(), saved.getDescription(), saved.getCreatedAt(), count);
    }
}
