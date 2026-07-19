package com.jpablodrexler.photomanager.domain.port.in.tag;

import java.util.List;
import java.util.UUID;

public interface BulkAddTagUseCase {
    void execute(List<Long> assetIds, String name, UUID userId);
}
