package com.jpablodrexler.photomanager.domain.repository;

import com.jpablodrexler.photomanager.domain.entity.ConvertAssetsDirectoriesDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ConvertAssetsConfigRepository extends JpaRepository<ConvertAssetsDirectoriesDefinition, Long> {

    List<ConvertAssetsDirectoriesDefinition> findAllByOrderByOrderAsc();
}
