package com.jpablodrexler.photomanager.infrastructure.persistence.jpa;

import com.jpablodrexler.photomanager.infrastructure.persistence.entity.ConvertAssetsDirectoriesDefinitionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JpaConvertConfigRepository extends JpaRepository<ConvertAssetsDirectoriesDefinitionEntity, Long> {

    List<ConvertAssetsDirectoriesDefinitionEntity> findAllByOrderByOrderAsc();
}
