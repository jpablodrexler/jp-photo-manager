package com.jpablodrexler.photomanager.infrastructure.persistence.adapter;

import com.jpablodrexler.photomanager.domain.model.AssetFilter;
import com.jpablodrexler.photomanager.domain.model.PaginatedResult;
import com.jpablodrexler.photomanager.domain.model.Album;
import com.jpablodrexler.photomanager.domain.model.Asset;
import com.jpablodrexler.photomanager.domain.port.out.AlbumRepository;
import com.jpablodrexler.photomanager.infrastructure.persistence.jpa.JpaAlbumRepository;
import com.jpablodrexler.photomanager.infrastructure.persistence.jpa.JpaAssetRepository;
import com.jpablodrexler.photomanager.infrastructure.persistence.mapper.AlbumEntityMapper;
import com.jpablodrexler.photomanager.infrastructure.persistence.mapper.AssetEntityMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AlbumRepositoryImpl implements AlbumRepository {

    private static final int PAGE_SIZE = 100;

    private final JpaAlbumRepository jpa;
    private final JpaAssetRepository jpaAssetRepository;
    private final AlbumEntityMapper albumMapper;
    private final AssetEntityMapper assetMapper;

    @Override
    @Transactional(readOnly = true)
    public List<Album> findByUserId(UUID userId) {
        return jpa.findByUser_Id(userId).stream().map(albumMapper::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Album> findByIdAndUserId(Long albumId, UUID userId) {
        return jpa.findByAlbumIdAndUser_Id(albumId, userId).map(albumMapper::toDomain);
    }

    @Override
    @Transactional
    public Album save(Album album) {
        return albumMapper.toDomain(jpa.save(albumMapper.toEntity(album)));
    }

    @Override
    @Transactional
    public void deleteById(Long albumId) {
        jpa.deleteById(albumId);
    }

    @Override
    @Transactional(readOnly = true)
    public PaginatedResult<Asset> findAssetsByAlbumId(Long albumId, int page, int pageSize) {
        int size = pageSize > 0 ? pageSize : PAGE_SIZE;
        Page<com.jpablodrexler.photomanager.infrastructure.persistence.entity.AssetEntity> entityPage =
                jpa.findAssetsByAlbumId(albumId, PageRequest.of(page, size));
        List<Asset> items = entityPage.getContent().stream().map(assetMapper::toDomain).toList();
        return new PaginatedResult<>(items, entityPage.getTotalElements(), page, size);
    }

    @Override
    @Transactional(readOnly = true)
    public long countAssets(Long albumId) {
        return jpa.countAssets(albumId);
    }

    @Override
    @Transactional
    public void addAssets(Long albumId, List<Long> assetIds) {
        for (Long assetId : assetIds) {
            if (!jpa.existsAsset(albumId, assetId)) {
                jpa.addAsset(albumId, assetId);
            }
        }
    }

    @Override
    @Transactional
    public void removeAssets(Long albumId, List<Long> assetIds) {
        for (Long assetId : assetIds) {
            jpa.removeAsset(albumId, assetId);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PaginatedResult<Asset> findSmartAlbumAssets(AssetFilter filter, int page, int pageSize) {
        int size = pageSize > 0 ? pageSize : PAGE_SIZE;
        String search = (filter.search() != null && !filter.search().isBlank())
                ? "%" + filter.search().trim().toLowerCase() + "%" : null;
        LocalDateTime dateFrom = filter.dateFrom() != null ? filter.dateFrom().atStartOfDay() : null;
        LocalDateTime dateTo = filter.dateTo() != null ? filter.dateTo().atTime(LocalTime.MAX) : null;
        Integer minRating = (filter.minRating() != null && filter.minRating() > 0) ? filter.minRating() : null;
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by("fileName").ascending());
        Page<com.jpablodrexler.photomanager.infrastructure.persistence.entity.AssetEntity> entityPage =
                jpaAssetRepository.findWithFilters(null, search, dateFrom, dateTo, minRating, filter.tags(), pageRequest);
        List<Asset> items = entityPage.getContent().stream().map(assetMapper::toDomain).toList();
        return new PaginatedResult<>(items, entityPage.getTotalElements(), page, size);
    }
}
