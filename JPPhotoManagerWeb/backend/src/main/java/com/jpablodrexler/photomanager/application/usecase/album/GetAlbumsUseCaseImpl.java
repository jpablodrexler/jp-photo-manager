package com.jpablodrexler.photomanager.application.usecase.album;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jpablodrexler.photomanager.application.dto.AlbumData;
import com.jpablodrexler.photomanager.application.dto.AlbumFilterJson;
import com.jpablodrexler.photomanager.application.dto.AssetFilter;
import com.jpablodrexler.photomanager.domain.enums.SortCriteria;
import com.jpablodrexler.photomanager.domain.model.Album;
import com.jpablodrexler.photomanager.domain.port.in.album.GetAlbumsUseCase;
import com.jpablodrexler.photomanager.domain.port.out.AlbumRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GetAlbumsUseCaseImpl implements GetAlbumsUseCase {

    private final AlbumRepository albumRepository;
    private final ObjectMapper objectMapper;

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
            AssetFilter filter = buildAssetFilter(album.getFilterJson(), 0, 1);
            return albumRepository.findSmartAlbumAssets(filter, 0, 1).total();
        }
        return albumRepository.countAssets(album.getAlbumId());
    }

    private AssetFilter buildAssetFilter(String filterJsonStr, int page, int pageSize) {
        try {
            AlbumFilterJson f = objectMapper.readValue(filterJsonStr, AlbumFilterJson.class);
            LocalDate dateFrom = f.dateFrom() != null ? LocalDate.parse(f.dateFrom()) : null;
            LocalDate dateTo = f.dateTo() != null ? LocalDate.parse(f.dateTo()) : null;
            return new AssetFilter(null, f.search(), dateFrom, dateTo, f.minRating(), SortCriteria.FILE_NAME, page, pageSize, false, Set.of());
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to parse filterJson", e);
        }
    }
}
