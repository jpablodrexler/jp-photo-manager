package com.jpablodrexler.photomanager.infrastructure.persistence.adapter;

import com.jpablodrexler.photomanager.domain.model.Folder;
import com.jpablodrexler.photomanager.domain.port.out.FolderRepository;
import com.jpablodrexler.photomanager.infrastructure.persistence.jpa.JpaFolderRepository;
import com.jpablodrexler.photomanager.infrastructure.persistence.mapper.FolderEntityMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class FolderRepositoryImpl implements FolderRepository {

    private final JpaFolderRepository jpa;
    private final FolderEntityMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public Optional<Folder> findById(Long id) {
        return jpa.findById(id).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Folder> findByPath(String path) {
        return jpa.findByPath(path).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByPath(String path) {
        return jpa.existsByPath(path);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Folder> findAll() {
        return jpa.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Folder> findSubFolders(String parentPath) {
        return jpa.findSubFolders(parentPath).stream().map(mapper::toDomain).toList();
    }

    @Override
    @Transactional
    public Folder save(Folder folder) {
        return mapper.toDomain(jpa.save(mapper.toEntity(folder)));
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        jpa.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public long count() {
        return jpa.count();
    }
}
