package com.jpablodrexler.photomanager.application.usecase.album;

import com.jpablodrexler.photomanager.application.exception.UserNotFoundException;
import com.jpablodrexler.photomanager.domain.model.AlbumData;
import com.jpablodrexler.photomanager.domain.model.Album;
import com.jpablodrexler.photomanager.domain.port.in.album.CreateAlbumUseCase;
import com.jpablodrexler.photomanager.domain.port.out.AlbumRepository;
import com.jpablodrexler.photomanager.domain.port.out.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CreateAlbumUseCaseImpl implements CreateAlbumUseCase {

    private final AlbumRepository albumRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public AlbumData execute(UUID userId, String name, String description, String filterJson) {
        userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        Album album = new Album();
        album.setUserId(userId);
        album.setName(name);
        album.setDescription(description);
        album.setCreatedAt(Instant.now());
        album.setFilterJson(filterJson);
        Album saved = albumRepository.save(album);
        return new AlbumData(saved.getAlbumId(), saved.getName(), saved.getDescription(), saved.getCreatedAt(), 0L, saved.getFilterJson());
    }
}
