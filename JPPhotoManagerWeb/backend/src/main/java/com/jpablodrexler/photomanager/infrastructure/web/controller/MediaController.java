package com.jpablodrexler.photomanager.infrastructure.web.controller;

import com.jpablodrexler.photomanager.domain.model.Asset;
import com.jpablodrexler.photomanager.domain.port.in.asset.GetPlaylistUseCase;
import com.jpablodrexler.photomanager.domain.port.in.asset.StreamAssetUseCase;
import com.jpablodrexler.photomanager.infrastructure.web.dto.AssetDto;
import com.jpablodrexler.photomanager.infrastructure.web.mapper.AssetWebMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourceRegion;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRange;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class MediaController {

    private static final long MAX_RANGE_LENGTH = 1024L * 1024L;

    private final StreamAssetUseCase streamAssetUseCase;
    private final GetPlaylistUseCase getPlaylistUseCase;
    private final AssetWebMapper assetWebMapper;

    @GetMapping("/api/assets/{id}/stream")
    public ResponseEntity<ResourceRegion> streamAsset(
            @PathVariable Long id,
            @RequestHeader(value = HttpHeaders.RANGE, required = false) String rangeHeader) {

        Asset asset = streamAssetUseCase.execute(id);

        Resource resource = new FileSystemResource(asset.getFullPath());
        if (!resource.exists()) {
            return ResponseEntity.notFound().build();
        }

        MediaType mediaType = MediaTypeFactory.getMediaType(resource)
                .orElse(MediaType.APPLICATION_OCTET_STREAM);

        long contentLength;
        try {
            contentLength = resource.contentLength();
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(mediaType);
        headers.set(HttpHeaders.ACCEPT_RANGES, "bytes");

        if (rangeHeader != null) {
            List<HttpRange> ranges = HttpRange.parseRanges(rangeHeader);
            if (!ranges.isEmpty()) {
                HttpRange range = ranges.get(0);
                long start = range.getRangeStart(contentLength);
                long end = range.getRangeEnd(contentLength);
                long rangeLength = Math.min(MAX_RANGE_LENGTH, end - start + 1);
                ResourceRegion region = new ResourceRegion(resource, start, rangeLength);
                return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                        .headers(headers)
                        .body(region);
            }
        }

        ResourceRegion region = new ResourceRegion(resource, 0, contentLength);
        return ResponseEntity.ok().headers(headers).body(region);
    }

    @GetMapping("/api/audio/playlist/{id}")
    public ResponseEntity<List<AssetDto>> getPlaylist(@PathVariable Long id) {
        List<Asset> assets = getPlaylistUseCase.execute(id);
        List<AssetDto> dtos = assets.stream().map(assetWebMapper::toDto).toList();
        return ResponseEntity.ok(dtos);
    }
}
