package com.jpablodrexler.photomanager.domain.repository;

import com.jpablodrexler.photomanager.domain.entity.SyncAssetsDirectoriesDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SyncAssetsConfigRepository extends JpaRepository<SyncAssetsDirectoriesDefinition, Long> {

    List<SyncAssetsDirectoriesDefinition> findAllByOrderByOrderAsc();
}
