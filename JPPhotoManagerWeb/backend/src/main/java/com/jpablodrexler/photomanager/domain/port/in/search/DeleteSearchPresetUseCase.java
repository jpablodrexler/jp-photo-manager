package com.jpablodrexler.photomanager.domain.port.in.search;

import java.util.UUID;

public interface DeleteSearchPresetUseCase {
    void execute(Long presetId, UUID userId);
}
