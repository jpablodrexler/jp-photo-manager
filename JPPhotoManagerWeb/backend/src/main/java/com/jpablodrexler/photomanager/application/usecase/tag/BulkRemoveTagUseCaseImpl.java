package com.jpablodrexler.photomanager.application.usecase.tag;

import com.jpablodrexler.photomanager.domain.model.Tag;
import com.jpablodrexler.photomanager.domain.port.in.tag.BulkRemoveTagUseCase;
import com.jpablodrexler.photomanager.domain.port.out.AssetRepository;
import com.jpablodrexler.photomanager.domain.port.out.TagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BulkRemoveTagUseCaseImpl implements BulkRemoveTagUseCase {

    private final AssetRepository assetRepository;
    private final TagRepository tagRepository;

    @Override
    @Transactional
    public void execute(List<Long> assetIds, String name) {
        String normalized = name.toLowerCase(Locale.ROOT).trim();
        Optional<Tag> tagOpt = tagRepository.findByName(normalized);
        if (tagOpt.isEmpty()) {
            return;
        }
        Tag tag = tagOpt.get();

        for (Long assetId : assetIds) {
            assetRepository.removeTagFromAsset(assetId, tag.getTagId());
        }

        // Delete the tag if no assets reference it after the bulk removal
        if (!tagRepository.isUsedByOtherAssets(tag.getTagId(), -1L)) {
            tagRepository.deleteById(tag.getTagId());
        }
    }
}
