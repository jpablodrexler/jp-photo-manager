package com.jpablodrexler.photomanager.domain.port.in.search;

import com.jpablodrexler.photomanager.domain.model.FilterPreset;
import com.jpablodrexler.photomanager.domain.model.SearchPreset;
import java.util.UUID;

public interface CreateSearchPresetUseCase {
    SearchPreset execute(UUID userId, String name, FilterPreset criteria);
}
