package com.jpablodrexler.photomanager.infrastructure.persistence.jpa;

import com.jpablodrexler.photomanager.infrastructure.persistence.entity.FolderEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface JpaFolderRepository extends JpaRepository<FolderEntity, Long> {

    Optional<FolderEntity> findByPath(String path);

    boolean existsByPath(String path);

    @Query("SELECT f FROM FolderEntity f WHERE f.path LIKE :parentPath%")
    List<FolderEntity> findSubFolders(String parentPath);
}
