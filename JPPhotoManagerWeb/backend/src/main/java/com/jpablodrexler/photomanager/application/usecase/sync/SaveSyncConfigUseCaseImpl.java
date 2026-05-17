package com.jpablodrexler.photomanager.application.usecase.sync;

import com.jpablodrexler.photomanager.domain.model.SyncDirectoriesDefinition;
import com.jpablodrexler.photomanager.domain.port.in.sync.SaveSyncConfigUseCase;
import com.jpablodrexler.photomanager.domain.port.out.SyncConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SaveSyncConfigUseCaseImpl implements SaveSyncConfigUseCase {

    private final SyncConfigRepository syncConfigRepository;

    @Override
    @Transactional
    public void execute(List<SyncDirectoriesDefinition> definitions) {
        syncConfigRepository.deleteAll();
        for (int i = 0; i < definitions.size(); i++) {
            definitions.get(i).setId(null);
            definitions.get(i).setOrder(i);
        }
        syncConfigRepository.saveAll(definitions);
    }
}
