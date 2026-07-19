package com.jpablodrexler.photomanager.domain.port.out;

import com.jpablodrexler.photomanager.domain.model.SyncDirectoriesDefinition;

import java.util.List;

public interface SyncConfigRepository {

    List<SyncDirectoriesDefinition> findAllOrderByOrder();

    void saveAll(List<SyncDirectoriesDefinition> definitions);
}
