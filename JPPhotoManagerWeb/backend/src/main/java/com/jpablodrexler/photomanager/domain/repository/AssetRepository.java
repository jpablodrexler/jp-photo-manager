package com.jpablodrexler.photomanager.domain.repository;

import com.jpablodrexler.photomanager.domain.entity.Asset;
import com.jpablodrexler.photomanager.domain.entity.Folder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AssetRepository extends JpaRepository<Asset, Long> {

    Page<Asset> findByFolder(Folder folder, Pageable pageable);

    List<Asset> findByFolder(Folder folder);

    List<Asset> findAll();

    Optional<Asset> findByFolderAndFileName(Folder folder, String fileName);

    boolean existsByFolderAndFileName(Folder folder, String fileName);

    void deleteByFolderAndFileName(Folder folder, String fileName);

    @Query("SELECT a FROM Asset a WHERE a.hash = :hash")
    List<Asset> findByHash(String hash);

    @Query("SELECT a FROM Asset a GROUP BY a.hash HAVING COUNT(a) > 1")
    List<String> findDuplicateHashes();
}
