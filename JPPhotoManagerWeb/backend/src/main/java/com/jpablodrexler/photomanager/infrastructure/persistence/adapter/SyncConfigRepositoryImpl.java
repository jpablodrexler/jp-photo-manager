package com.jpablodrexler.photomanager.infrastructure.persistence.adapter;

import com.jpablodrexler.photomanager.domain.model.SyncDirectoriesDefinition;
import com.jpablodrexler.photomanager.domain.port.out.SyncConfigRepository;
import com.jpablodrexler.photomanager.infrastructure.persistence.entity.SyncAssetsDirectoriesDefinitionEntity;
import com.jpablodrexler.photomanager.infrastructure.persistence.jpa.JpaSyncConfigRepository;
import com.jpablodrexler.photomanager.infrastructure.persistence.mapper.SyncConfigEntityMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SyncConfigRepositoryImpl implements SyncConfigRepository {

    private final JpaSyncConfigRepository jpa;
    private final SyncConfigEntityMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public List<SyncDirectoriesDefinition> findAllOrderByOrder() {
        return jpa.findAllByOrderByOrderAsc().stream().map(mapper::toDomain).toList();
    }

    @Override
    @Transactional
    public void saveAll(List<SyncDirectoriesDefinition> definitions) {
        jpa.deleteAllInBatch();
        List<SyncAssetsDirectoriesDefinitionEntity> entities = definitions.stream()
                .map(mapper::toEntity)
                .toList();
        jpa.saveAll(entities);
    }
}
