package com.jpablodrexler.photomanager.application.usecase.tag;

import com.jpablodrexler.photomanager.domain.model.Tag;
import com.jpablodrexler.photomanager.domain.port.in.tag.AddTagToAssetUseCase;
import com.jpablodrexler.photomanager.domain.port.out.AssetRepository;
import com.jpablodrexler.photomanager.domain.port.out.TagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class AddTagToAssetUseCaseImpl implements AddTagToAssetUseCase {

    private final AssetRepository assetRepository;
    private final TagRepository tagRepository;

    @Override
    @Transactional
    public void execute(Long assetId, String name) {
        if (!assetRepository.existsById(assetId)) {
            throw new NoSuchElementException("Asset not found: " + assetId);
        }

        String normalized = name.toLowerCase(Locale.ROOT).trim();
        Tag tag = tagRepository.findByName(normalized)
                .orElseGet(() -> tagRepository.save(Tag.builder().name(normalized).build()));

        assetRepository.addTagToAsset(assetId, tag.getTagId());
    }
}
