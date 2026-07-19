package com.jpablodrexler.photomanager.application.usecase.sync;

import com.jpablodrexler.photomanager.domain.model.SyncDirectoriesDefinition;
import com.jpablodrexler.photomanager.domain.port.in.sync.GetSyncConfigUseCase;
import com.jpablodrexler.photomanager.domain.port.out.SyncConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GetSyncConfigUseCaseImpl implements GetSyncConfigUseCase {

    private final SyncConfigRepository syncConfigRepository;

    @Override
    @Transactional(readOnly = true)
    public List<SyncDirectoriesDefinition> execute() {
        return syncConfigRepository.findAllOrderByOrder();
    }
}
