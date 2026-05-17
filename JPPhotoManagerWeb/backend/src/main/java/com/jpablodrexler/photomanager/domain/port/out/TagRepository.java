package com.jpablodrexler.photomanager.domain.port.out;

import com.jpablodrexler.photomanager.domain.model.Tag;

import java.util.List;
import java.util.Optional;

public interface TagRepository {

    Optional<Tag> findByName(String name);

    List<Tag> findByNameContaining(String q, int limit);

    Tag save(Tag tag);

    void deleteById(Long id);

    boolean isUsedByOtherAssets(Long tagId, Long excludeAssetId);
}
