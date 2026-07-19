package com.jpablodrexler.photomanager.infrastructure.persistence.adapter;

import com.jpablodrexler.photomanager.domain.model.ConvertDirectoriesDefinition;
import com.jpablodrexler.photomanager.domain.port.out.ConvertConfigRepository;
import com.jpablodrexler.photomanager.infrastructure.persistence.entity.ConvertAssetsDirectoriesDefinitionEntity;
import com.jpablodrexler.photomanager.infrastructure.persistence.jpa.JpaConvertConfigRepository;
import com.jpablodrexler.photomanager.infrastructure.persistence.mapper.ConvertConfigEntityMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ConvertConfigRepositoryImpl implements ConvertConfigRepository {

    private final JpaConvertConfigRepository jpa;
    private final ConvertConfigEntityMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public List<ConvertDirectoriesDefinition> findAllOrderByOrder() {
        return jpa.findAllByOrderByOrderAsc().stream().map(mapper::toDomain).toList();
    }

    @Override
    @Transactional
    public void saveAll(List<ConvertDirectoriesDefinition> definitions) {
        jpa.deleteAllInBatch();
        List<ConvertAssetsDirectoriesDefinitionEntity> entities = definitions.stream()
                .map(mapper::toEntity)
                .toList();
        jpa.saveAll(entities);
    }
}
