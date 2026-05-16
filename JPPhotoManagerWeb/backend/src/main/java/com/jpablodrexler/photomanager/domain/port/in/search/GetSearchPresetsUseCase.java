package com.jpablodrexler.photomanager.domain.port.in.search;

import com.jpablodrexler.photomanager.domain.model.SearchPreset;
import java.util.List;
import java.util.UUID;

public interface GetSearchPresetsUseCase {
    List<SearchPreset> execute(UUID userId);
}
