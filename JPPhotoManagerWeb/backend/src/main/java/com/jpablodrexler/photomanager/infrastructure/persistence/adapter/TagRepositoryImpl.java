package com.jpablodrexler.photomanager.infrastructure.persistence.adapter;

import com.jpablodrexler.photomanager.domain.model.Tag;
import com.jpablodrexler.photomanager.domain.port.out.TagRepository;
import com.jpablodrexler.photomanager.infrastructure.persistence.mapper.TagEntityMapper;
import com.jpablodrexler.photomanager.infrastructure.persistence.jpa.JpaTagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TagRepositoryImpl implements TagRepository {

    private final JpaTagRepository jpa;
    private final TagEntityMapper tagMapper;

    @Override
    @Transactional(readOnly = true)
    public Optional<Tag> findByName(String name) {
        return jpa.findByName(name).map(tagMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Tag> findByNameContaining(String q, int limit) {
        return jpa.findByNameContainingIgnoreCaseOrderByName(q, PageRequest.of(0, limit))
                .stream().map(tagMapper::toDomain).toList();
    }

    @Override
    @Transactional
    public Tag save(Tag tag) {
        return tagMapper.toDomain(jpa.save(tagMapper.toEntity(tag)));
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        jpa.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isUsedByOtherAssets(Long tagId, Long excludeAssetId) {
        return jpa.isUsedByOtherAssets(tagId, excludeAssetId);
    }
}
