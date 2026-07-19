package com.jpablodrexler.photomanager.domain.port.out;

import com.jpablodrexler.photomanager.domain.model.Folder;

import java.util.List;
import java.util.Optional;

public interface FolderRepository {

    Optional<Folder> findById(Long id);

    Optional<Folder> findByPath(String path);

    boolean existsByPath(String path);

    List<Folder> findAll();

    List<Folder> findSubFolders(String parentPath);

    Folder save(Folder folder);

    void deleteById(Long id);

    long count();
}
