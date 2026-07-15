package com.jpablodrexler.photomanager.infrastructure.persistence.adapter;

import com.jpablodrexler.photomanager.domain.model.RecentTargetPath;
import com.jpablodrexler.photomanager.domain.port.out.RecentTargetPathRepository;
import com.jpablodrexler.photomanager.infrastructure.persistence.entity.RecentTargetPathEntity;
import com.jpablodrexler.photomanager.infrastructure.persistence.jpa.JpaRecentTargetPathRepository;
import com.jpablodrexler.photomanager.infrastructure.persistence.mapper.RecentTargetPathEntityMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RecentTargetPathRepositoryImpl implements RecentTargetPathRepository {

    private final JpaRecentTargetPathRepository jpa;
    private final RecentTargetPathEntityMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public List<RecentTargetPath> findAllOrderByIdDesc() {
        return jpa.findAllByOrderByIdDesc().stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByPath(String path) {
        return jpa.existsByPath(path);
    }

    @Override
    @Transactional
    public RecentTargetPath save(RecentTargetPath recentTargetPath) {
        RecentTargetPathEntity entity = mapper.toEntity(recentTargetPath);
        RecentTargetPathEntity saved = jpa.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        jpa.deleteById(id);
    }

    @Override
    @Transactional
    public void deleteAll(List<RecentTargetPath> paths) {
        List<Long> ids = paths.stream().map(RecentTargetPath::getId).toList();
        jpa.deleteAllById(ids);
    }
}
