package com.jpablodrexler.photomanager.infrastructure.adapter.out.playlist;

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
import java.util.TreeMap;

@Component
@RequiredArgsConstructor
@Slf4j
public class PlsPlaylistParserAdapter implements PlaylistParserPort {

    private final AssetRepository assetRepository;

    @Override
    public boolean supports(String fileName) {
        return fileName.toLowerCase().endsWith(".pls");
    }

    @Override
    public List<Asset> parse(Path playlistPath) {
        List<Asset> result = new ArrayList<>();
        try {
            List<String> lines = Files.readAllLines(playlistPath);
            TreeMap<Integer, String> fileEntries = new TreeMap<>();
            for (String line : lines) {
                String trimmed = line.trim();
                if (!trimmed.toLowerCase().startsWith("file")) {
                    continue;
                }
                int eqIdx = trimmed.indexOf('=');
                if (eqIdx < 0) {
                    continue;
                }
                String key = trimmed.substring(0, eqIdx).trim();
                String value = trimmed.substring(eqIdx + 1).trim();
                try {
                    int index = Integer.parseInt(key.substring(4));
                    fileEntries.put(index, value);
                } catch (NumberFormatException ignored) {
                    // skip malformed key
                }
            }
            for (String filePath : fileEntries.values()) {
                String fileName = Path.of(filePath).getFileName().toString();
                List<Asset> found = assetRepository.findByFileName(fileName);
                if (!found.isEmpty()) {
                    result.add(found.get(0));
                }
            }
        } catch (IOException e) {
            log.error("Failed to parse PLS playlist: {}", playlistPath, e);
        }
        return result;
    }
}
