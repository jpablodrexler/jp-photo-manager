package com.jpablodrexler.photomanager.application.usecase.asset;

import com.jpablodrexler.photomanager.domain.model.Asset;
import com.jpablodrexler.photomanager.domain.port.in.asset.GetPlaylistUseCase;
import com.jpablodrexler.photomanager.domain.port.out.AssetRepository;
import com.jpablodrexler.photomanager.domain.port.out.playlist.PlaylistParserPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Path;
import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class GetPlaylistUseCaseImpl implements GetPlaylistUseCase {

    private final AssetRepository assetRepository;
    private final List<PlaylistParserPort> parsers;

    @Override
    @Transactional(readOnly = true)
    public List<Asset> execute(Long assetId) {
        Asset playlistAsset = assetRepository.findById(assetId)
                .orElseThrow(() -> new NoSuchElementException("Asset not found: " + assetId));

        PlaylistParserPort parser = parsers.stream()
                .filter(p -> p.supports(playlistAsset.getFileName()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unsupported playlist format: " + playlistAsset.getFileName()));

        return parser.parse(Path.of(playlistAsset.getFullPath()));
    }
}
