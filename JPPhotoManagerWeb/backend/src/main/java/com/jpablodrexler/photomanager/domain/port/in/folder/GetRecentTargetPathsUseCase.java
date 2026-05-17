package com.jpablodrexler.photomanager.domain.port.in.folder;

import java.util.List;

public interface GetRecentTargetPathsUseCase {
    List<String> execute();
}
