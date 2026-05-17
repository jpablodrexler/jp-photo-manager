package com.jpablodrexler.photomanager.application.usecase.album;

import com.jpablodrexler.photomanager.application.dto.AlbumData;
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

    @Override
    @Transactional(readOnly = true)
    public List<AlbumData> execute(UUID userId) {
        return albumRepository.findByUserId(userId).stream()
                .map(album -> new AlbumData(
                        album.getAlbumId(),
                        album.getName(),
                        album.getDescription(),
                        album.getCreatedAt(),
                        albumRepository.countAssets(album.getAlbumId())))
                .collect(Collectors.toList());
    }
}
