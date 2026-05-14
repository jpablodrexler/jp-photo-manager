package com.jpablodrexler.photomanager.domain.port.out;

import com.jpablodrexler.photomanager.domain.model.SyncDirectoriesDefinition;

import java.util.List;

public interface SyncConfigRepositoryPort {
    List<SyncDirectoriesDefinition> findAllOrderByOrder();
    void replaceAll(List<SyncDirectoriesDefinition> definitions);
}
