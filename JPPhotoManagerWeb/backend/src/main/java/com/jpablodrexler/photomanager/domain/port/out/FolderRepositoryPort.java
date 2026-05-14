package com.jpablodrexler.photomanager.domain.port.out;

import com.jpablodrexler.photomanager.domain.model.Folder;

import java.util.List;
import java.util.Optional;

public interface FolderRepositoryPort {
    Optional<Folder> findById(Long id);
    Optional<Folder> findByPath(String path);
    boolean existsByPath(String path);
    List<Folder> findSubFolders(String parentPath);
    List<Folder> findAll();
    Folder save(Folder folder);
    void deleteById(Long id);
    long count();
}
