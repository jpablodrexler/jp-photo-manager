package com.jpablodrexler.photomanager.application.usecase.album;

import com.jpablodrexler.photomanager.domain.model.AlbumData;
import com.jpablodrexler.photomanager.domain.model.AssetFilter;
import com.jpablodrexler.photomanager.application.dto.AlbumFilterJson;
import com.jpablodrexler.photomanager.application.exception.AlbumNotFoundException;
import com.jpablodrexler.photomanager.domain.enums.SortCriteria;
import com.jpablodrexler.photomanager.domain.port.in.album.UpdateAlbumUseCase;
import com.jpablodrexler.photomanager.domain.port.out.AlbumRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UpdateAlbumUseCaseImpl implements UpdateAlbumUseCase {

    private static final int PAGE_SIZE = 50;

    private final AlbumRepository albumRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public AlbumData execute(Long albumId, UUID userId, String name, String description, String filterJson) {
        var album = albumRepository.findByIdAndUserId(albumId, userId)
                .orElseThrow(() -> new AlbumNotFoundException(albumId));
        album.setName(name);
        album.setDescription(description);
        album.setFilterJson(filterJson);
        var saved = albumRepository.save(album);
        long count;
        if (saved.getFilterJson() != null) {
            AssetFilter filter = buildAssetFilter(saved.getFilterJson(), 0, 1);
            count = albumRepository.findSmartAlbumAssets(filter, 0, 1).total();
        } else {
            count = albumRepository.countAssets(albumId);
        }
        return new AlbumData(saved.getAlbumId(), saved.getName(), saved.getDescription(), saved.getCreatedAt(), count, saved.getFilterJson());
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
