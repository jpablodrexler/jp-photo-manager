package com.jpablodrexler.photomanager.domain.port.in.tag;

import java.util.List;

public interface BulkRemoveTagUseCase {
    void execute(List<Long> assetIds, String name);
}
