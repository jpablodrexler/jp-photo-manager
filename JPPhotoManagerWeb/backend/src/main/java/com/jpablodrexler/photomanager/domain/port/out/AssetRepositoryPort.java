package com.jpablodrexler.photomanager.domain.port.out;

import com.jpablodrexler.photomanager.application.dto.AssetFilter;
import com.jpablodrexler.photomanager.application.dto.PaginatedResult;
import com.jpablodrexler.photomanager.domain.model.Asset;
import com.jpablodrexler.photomanager.domain.model.Folder;

import java.util.List;
import java.util.Optional;

public interface AssetRepositoryPort {
    Optional<Asset> findById(Long id);
    Optional<Asset> findByFolderAndFileName(Folder folder, String fileName);
    PaginatedResult<Asset> findFiltered(AssetFilter filter);
    List<Asset> findByFolder(Folder folder);
    List<Asset> findAll();
    List<Asset> findAllById(List<Long> ids);
    List<Asset> findByDeletedAtIsNull();
    PaginatedResult<Asset> findDeleted(int page, int pageSize);
    List<Asset> findByHash(String hash);
    Asset save(Asset asset);
    void delete(Asset asset);
    long count();
    long countDeleted();
    boolean existsByFolderAndFileName(Folder folder, String fileName);
}
