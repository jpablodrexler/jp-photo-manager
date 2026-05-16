package com.jpablodrexler.photomanager.infrastructure.persistence.jpa;

import com.jpablodrexler.photomanager.infrastructure.persistence.entity.SyncAssetsDirectoriesDefinitionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JpaSyncConfigRepository extends JpaRepository<SyncAssetsDirectoriesDefinitionEntity, Long> {

    List<SyncAssetsDirectoriesDefinitionEntity> findAllByOrderByOrderAsc();
}
