package com.jpablodrexler.photomanager.application.usecase.tag;

import com.jpablodrexler.photomanager.domain.model.Tag;
import com.jpablodrexler.photomanager.domain.port.in.tag.RemoveTagFromAssetUseCase;
import com.jpablodrexler.photomanager.domain.port.out.AssetRepository;
import com.jpablodrexler.photomanager.domain.port.out.TagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class RemoveTagFromAssetUseCaseImpl implements RemoveTagFromAssetUseCase {

    private final AssetRepository assetRepository;
    private final TagRepository tagRepository;

    @Override
    @Transactional
    public void execute(Long assetId, String name) {
        String normalized = name.toLowerCase(Locale.ROOT).trim();
        Tag tag = tagRepository.findByName(normalized)
                .orElseThrow(() -> new NoSuchElementException("Tag not found: " + normalized));

        int removed = assetRepository.removeTagFromAsset(assetId, tag.getTagId());
        if (removed == 0) {
            throw new NoSuchElementException("Tag '" + normalized + "' is not assigned to asset " + assetId);
        }

        if (!tagRepository.isUsedByOtherAssets(tag.getTagId(), assetId)) {
            tagRepository.deleteById(tag.getTagId());
        }
    }
}
