package com.jpablodrexler.photomanager.application.usecase.album;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jpablodrexler.photomanager.application.dto.AlbumData;
import com.jpablodrexler.photomanager.application.dto.AlbumFilterJson;
import com.jpablodrexler.photomanager.application.dto.AssetFilter;
import com.jpablodrexler.photomanager.application.dto.PaginatedResult;
import com.jpablodrexler.photomanager.application.exception.AlbumNotFoundException;
import com.jpablodrexler.photomanager.domain.enums.SortCriteria;
import com.jpablodrexler.photomanager.domain.model.Asset;
import com.jpablodrexler.photomanager.domain.port.in.album.GetAlbumUseCase;
import com.jpablodrexler.photomanager.domain.port.out.AlbumRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetAlbumUseCaseImpl implements GetAlbumUseCase {

    private static final int PAGE_SIZE = 50;

    private final AlbumRepository albumRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(readOnly = true)
    public AlbumData executeSummary(Long albumId, UUID userId) {
        var album = albumRepository.findByIdAndUserId(albumId, userId)
                .orElseThrow(() -> new AlbumNotFoundException(albumId));
        long count;
        if (album.getFilterJson() != null) {
            AssetFilter filter = buildAssetFilter(album.getFilterJson(), 0, 1);
            count = albumRepository.findSmartAlbumAssets(filter, 0, 1).total();
        } else {
            count = albumRepository.countAssets(albumId);
        }
        return new AlbumData(album.getAlbumId(), album.getName(), album.getDescription(), album.getCreatedAt(), count, album.getFilterJson());
    }

    @Override
    @Transactional(readOnly = true)
    public PaginatedResult<Asset> executeAssets(Long albumId, UUID userId, int page) {
        var album = albumRepository.findByIdAndUserId(albumId, userId)
                .orElseThrow(() -> new AlbumNotFoundException(albumId));
        if (album.getFilterJson() != null) {
            AssetFilter filter = buildAssetFilter(album.getFilterJson(), page, PAGE_SIZE);
            return albumRepository.findSmartAlbumAssets(filter, page, PAGE_SIZE);
        }
        return albumRepository.findAssetsByAlbumId(albumId, page, PAGE_SIZE);
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
