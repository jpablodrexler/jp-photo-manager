package com.jpablodrexler.photomanager.infrastructure.persistence.adapter;

import com.jpablodrexler.photomanager.domain.model.Folder;
import com.jpablodrexler.photomanager.domain.port.out.FolderRepository;
import com.jpablodrexler.photomanager.infrastructure.persistence.entity.FolderEntity;
import com.jpablodrexler.photomanager.infrastructure.persistence.jpa.JpaFolderRepository;
import com.jpablodrexler.photomanager.infrastructure.persistence.mapper.FolderEntityMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class FolderRepositoryImpl implements FolderRepository {

    private final JpaFolderRepository jpa;
    private final FolderEntityMapper mapper;
    private final PlatformTransactionManager transactionManager;

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

    // A racy find-then-create is not safe under concurrent catalog runs: folders.path is
    // UNIQUE (V35), so two threads both missing the initial lookup would otherwise both try
    // to insert. The insert attempt runs in its own REQUIRES_NEW transaction (programmatic,
    // not @Transactional, since a self-invoked annotation would be ignored per the Spring
    // proxy self-invocation pitfall) so a losing insert only rolls back itself, not whatever
    // transaction the caller is already in; the loser then re-reads the winner's row.
    @Override
    public Folder findOrCreateByPath(String path) {
        Optional<Folder> existing = jpa.findByPath(path).map(mapper::toDomain);
        if (existing.isPresent()) {
            return existing.get();
        }

        TransactionTemplate newFolderTransaction = new TransactionTemplate(transactionManager);
        newFolderTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        try {
            return newFolderTransaction.execute(status -> {
                FolderEntity entity = new FolderEntity();
                entity.setPath(path);
                return mapper.toDomain(jpa.save(entity));
            });
        } catch (DataIntegrityViolationException e) {
            return jpa.findByPath(path).map(mapper::toDomain)
                    .orElseThrow(() -> new IllegalStateException(
                            "Folder path conflicted on insert but is no longer resolvable: " + path, e));
        }
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
