package com.jpablodrexler.photomanager.infrastructure.service;

import com.jpablodrexler.photomanager.api.exception.AlbumNotFoundException;
import com.jpablodrexler.photomanager.application.dto.PaginatedData;
import com.jpablodrexler.photomanager.domain.entity.Album;
import com.jpablodrexler.photomanager.domain.entity.Asset;
import com.jpablodrexler.photomanager.domain.repository.AlbumRepository;
import com.jpablodrexler.photomanager.domain.repository.AssetRepository;
import com.jpablodrexler.photomanager.domain.repository.UserRepository;
import com.jpablodrexler.photomanager.domain.service.AlbumService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AlbumServiceImpl implements AlbumService {

    private static final int PAGE_SIZE = 50;

    private final AlbumRepository albumRepository;
    private final UserRepository userRepository;
    private final AssetRepository assetRepository;

    @Override
    @Transactional(readOnly = true)
    public List<Album> findByUserId(UUID userId) {
        return albumRepository.findByUser_Id(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Album> findByIdAndUserId(Long albumId, UUID userId) {
        return albumRepository.findByAlbumIdAndUser_Id(albumId, userId);
    }

    @Override
    @Transactional
    public Album createAlbum(UUID userId, String name, String description) {
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        Album album = new Album();
        album.setUser(user);
        album.setName(name);
        album.setDescription(description);
        album.setCreatedAt(Instant.now());
        return albumRepository.save(album);
    }

    @Override
    @Transactional
    public Album updateAlbum(Long albumId, UUID userId, String name, String description) {
        Album album = albumRepository.findByAlbumIdAndUser_Id(albumId, userId)
                .orElseThrow(() -> new AlbumNotFoundException(albumId));
        album.setName(name);
        album.setDescription(description);
        return albumRepository.save(album);
    }

    @Override
    @Transactional
    public void deleteAlbum(Long albumId, UUID userId) {
        Album album = albumRepository.findByAlbumIdAndUser_Id(albumId, userId)
                .orElseThrow(() -> new AlbumNotFoundException(albumId));
        albumRepository.delete(album);
    }

    @Override
    @Transactional(readOnly = true)
    public PaginatedData<Asset> getAlbumAssets(Long albumId, UUID userId, int pageIndex) {
        albumRepository.findByAlbumIdAndUser_Id(albumId, userId)
                .orElseThrow(() -> new AlbumNotFoundException(albumId));
        PageRequest pageRequest = PageRequest.of(pageIndex, PAGE_SIZE);
        Page<Asset> page = assetRepository.findByAlbumId(albumId, pageRequest);
        return new PaginatedData<>(page.getContent(), pageIndex, page.getTotalPages(), page.getTotalElements());
    }

    @Override
    @Transactional
    public void addAssets(Long albumId, UUID userId, List<Long> assetIds) {
        Album album = albumRepository.findByAlbumIdAndUser_Id(albumId, userId)
                .orElseThrow(() -> new AlbumNotFoundException(albumId));
        List<Asset> toAdd = assetRepository.findAllById(assetIds);
        Set<Long> existing = album.getAssets().stream()
                .map(Asset::getAssetId)
                .collect(Collectors.toSet());
        toAdd.stream()
                .filter(a -> !existing.contains(a.getAssetId()))
                .forEach(album.getAssets()::add);
        albumRepository.save(album);
    }

    @Override
    @Transactional
    public void removeAssets(Long albumId, UUID userId, List<Long> assetIds) {
        Album album = albumRepository.findByAlbumIdAndUser_Id(albumId, userId)
                .orElseThrow(() -> new AlbumNotFoundException(albumId));
        Set<Long> toRemove = Set.copyOf(assetIds);
        album.getAssets().removeIf(a -> toRemove.contains(a.getAssetId()));
        albumRepository.save(album);
    }
}
