package com.jpablodrexler.photomanager.infrastructure.service;

import com.jpablodrexler.photomanager.domain.model.Asset;
import com.jpablodrexler.photomanager.domain.port.out.AssetRepository;
import com.jpablodrexler.photomanager.domain.port.out.playlist.PlaylistParserPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class M3uPlaylistParserServiceAdapter implements PlaylistParserPort {

    private final AssetRepository assetRepository;

    @Override
    public boolean supports(String fileName) {
        String lower = fileName.toLowerCase();
        return lower.endsWith(".m3u") || lower.endsWith(".m3u8");
    }

    @Override
    public List<Asset> parse(Path playlistPath) {
        List<Asset> result = new ArrayList<>();
        try {
            List<String> lines = Files.readAllLines(playlistPath);
            for (String line : lines) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }
                String fileName = Path.of(trimmed).getFileName().toString();
                List<Asset> found = assetRepository.findByFileName(fileName);
                if (!found.isEmpty()) {
                    result.add(found.get(0));
                }
            }
        } catch (IOException e) {
            log.error("Failed to parse M3U playlist: {}", playlistPath, e);
        }
        return result;
    }
}
