package com.jpablodrexler.photomanager.domain.port.in.sync;

import com.jpablodrexler.photomanager.domain.model.SyncDirectoriesDefinition;
import java.util.List;

public interface SaveSyncConfigUseCase {
    void execute(List<SyncDirectoriesDefinition> definitions);
}
