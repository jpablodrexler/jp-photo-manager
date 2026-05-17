package com.jpablodrexler.photomanager.domain.port.in.tag;

import java.util.List;

public interface BulkAddTagUseCase {
    void execute(List<Long> assetIds, String name);
}
