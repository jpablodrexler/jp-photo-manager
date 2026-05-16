package com.jpablodrexler.photomanager.domain.port.in.recycle;

import java.util.List;

public interface RestoreAssetsUseCase {
    void execute(List<Long> assetIds);
}
