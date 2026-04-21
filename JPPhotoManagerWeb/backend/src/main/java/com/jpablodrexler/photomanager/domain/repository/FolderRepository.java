package com.jpablodrexler.photomanager.domain.repository;

import com.jpablodrexler.photomanager.domain.entity.Folder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FolderRepository extends JpaRepository<Folder, Long> {

    Optional<Folder> findByPath(String path);

    boolean existsByPath(String path);

    @Query("SELECT f FROM Folder f WHERE f.path LIKE :parentPath%")
    List<Folder> findSubFolders(String parentPath);
}
