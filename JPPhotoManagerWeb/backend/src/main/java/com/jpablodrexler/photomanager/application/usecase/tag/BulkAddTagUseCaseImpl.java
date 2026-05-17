package com.jpablodrexler.photomanager.application.usecase.tag;

import com.jpablodrexler.photomanager.domain.model.Tag;
import com.jpablodrexler.photomanager.domain.port.in.tag.BulkAddTagUseCase;
import com.jpablodrexler.photomanager.domain.port.out.AssetRepository;
import com.jpablodrexler.photomanager.domain.port.out.TagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class BulkAddTagUseCaseImpl implements BulkAddTagUseCase {

    private final AssetRepository assetRepository;
    private final TagRepository tagRepository;

    @Override
    @Transactional
    public void execute(List<Long> assetIds, String name) {
        String normalized = name.toLowerCase(Locale.ROOT).trim();
        Tag tag = tagRepository.findByName(normalized)
                .orElseGet(() -> tagRepository.save(Tag.builder().name(normalized).build()));

        for (Long assetId : assetIds) {
            assetRepository.addTagToAsset(assetId, tag.getTagId());
        }
    }
}
